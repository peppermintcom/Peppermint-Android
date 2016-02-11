package com.peppermint.app.cloud.senders;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.peppermint.app.R;
import com.peppermint.app.cloud.apis.GoogleApi;
import com.peppermint.app.cloud.apis.PeppermintApi;
import com.peppermint.app.cloud.apis.exceptions.GoogleApiInvalidAccessTokenException;
import com.peppermint.app.cloud.apis.exceptions.GoogleApiNoAuthorizationException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.cloud.senders.mail.gmail.GmailSender;
import com.peppermint.app.data.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     Handles all errors thrown by a {@link SenderUploadTask}. <br />
 *     A {@link Sender} might be able to recover from some of these errors using a
 *     {@link SenderErrorHandler}.
 * </p>
 * <p>
 *     SenderErrorHandlers are an extension of {@link SenderObject}s,
 *     thus containing common contextual data.
 * </p>
 */
public class SenderErrorHandler extends SenderObject {

    private static final String TAG = SenderErrorHandler.class.getSimpleName();

    private static final int REQUEST_AUTHORIZATION_GOOGLE = 98764;
    private static final int REQUEST_AUTHORIZATION_PEPPERMINT = 98765;

    protected static final int RECOVERY_RETRY = -1;
    protected static final int RECOVERY_NOK = -2;
    protected static final int RECOVERY_DONOTHING = 1;

    // allow the sender task to be executed up to MAX_RETRIES+1 times
    private static final int MAX_RETRIES = 9;

    // FIXME retry map gets filled and there's no mechanism to clean it up properly!
    private Map<UUID, Integer> mRetryMap;                               // map of upload task ids and their about of retries
    private Map<UUID, SenderUploadTask> mRecoveringTaskMap;             // map of upload tasks that are recovering

    private ThreadPoolExecutor mExecutor;                               // a thread pool for support tasks
    private Map<UUID, SenderSupportTask> mSupportTaskMap;               // map of support tasks under execution
    private Map<UUID, SenderSupportTask> mSupportTaskActivityResultMap; // map of support tasks not under execution, that are waiting for an activity response

    private SenderUploadListener mSenderUploadListener;
    private Sender mSender;

    /**
     * This broadcast receiver gets results from {@link GetResultActivity}<br />
     * It allows any SenderErrorHandler to recover from an error by triggering activities.<br />
     * This is useful for using APIs that request permissions through another Activity, such as the Google API
     **/
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.hasExtra(GetResultActivity.INTENT_ID)) {
                UUID taskId = (UUID) intent.getSerializableExtra(GetResultActivity.INTENT_ID);
                SenderUploadTask recoveringTask = mRecoveringTaskMap.containsKey(taskId) ? mRecoveringTaskMap.get(taskId) : null;
                if(recoveringTask != null) {
                    onUploadTaskActivityResult(recoveringTask,
                            intent.getIntExtra(GetResultActivity.INTENT_REQUESTCODE, -1),
                            intent.getIntExtra(GetResultActivity.INTENT_RESULTCODE, -1),
                            (Intent) intent.getParcelableExtra(GetResultActivity.INTENT_DATA));
                    return;
                }

                SenderSupportTask supportTask = mSupportTaskActivityResultMap.remove(taskId);
                if(supportTask != null) {
                    handleOnSupportTaskActivityResult(supportTask, supportTask.getRelatedUploadTask(),
                            intent.getIntExtra(GetResultActivity.INTENT_REQUESTCODE, -1),
                            intent.getIntExtra(GetResultActivity.INTENT_RESULTCODE, -1),
                            (Intent) intent.getParcelableExtra(GetResultActivity.INTENT_DATA));
                }
            }
        }
    };
    private final String mBroadcastType = this.getClass().getSimpleName() + "-" + getId().toString();
    private final IntentFilter mFilter = new IntentFilter(mBroadcastType);

    public SenderErrorHandler(Sender sender, SenderUploadListener senderUploadListener) {
        super(sender);
        construct(sender, senderUploadListener);
    }

    private void construct(Sender sender, SenderUploadListener senderUploadListener) {
        this.mSender = sender;
        this.mSenderUploadListener = senderUploadListener;

        // upload task maps
        this.mRetryMap = new HashMap<>();
        this.mRecoveringTaskMap = new HashMap<>();

        // support task maps
        this.mSupportTaskMap = new HashMap<>();
        this.mSupportTaskActivityResultMap = new HashMap<>();

        // private thread pool to avoid hanging up other AsyncTasks
        // only one thread so that messages are sent one at a time (allows better control when cancelling)
        this.mExecutor = new ThreadPoolExecutor(/*CPU_COUNT + 1, CPU_COUNT * 2 + 2*/1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Initializes the error handler.<br />
     * <strong>{@link #deinit()} must always be invoked after the error handler is no longer needed.</strong>
     */
    public void init() {
        // register the activity result broadcast listener
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, mFilter);
    }

    /**
     * De-initializes the error handler.
     */
    public void deinit() {
        // unregister the activity result broadcast listener
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        mSupportTaskActivityResultMap.clear();
    }

    public void authorizePeppermint(SenderUploadTask relatedUploadTask, Message message) {
        PeppermintAuthorizationSupportTask task = new PeppermintAuthorizationSupportTask(getSender(), message, null);
        launchSupportTask(task, relatedUploadTask);
    }

    protected UUID launchSupportTask(SenderSupportTask task, SenderUploadTask relatedUploadTask) {
        mSupportTaskMap.put(task.getId(), task);
        task.setSenderSupportListener(mSupportListener);
        task.setRelatedUploadTask(relatedUploadTask);
        task.executeOnExecutor(mExecutor);
        return task.getId();
    }

    protected void onUploadTaskActivityResult(SenderUploadTask recoveringTask, int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_AUTHORIZATION_GOOGLE) {
            if(resultCode == Activity.RESULT_OK) {
                checkRetries(recoveringTask);
                return;
            }
            Toast.makeText(getContext(), R.string.sender_msg_cancelled_gmail_api, Toast.LENGTH_LONG).show();
        }

        doNotRecover(recoveringTask);   // nothing to recover; must be overriden to include additional behaviour
    }

    private void handleOnSupportTaskActivityResult(SenderSupportTask supportTask, SenderUploadTask recoveringTask, int requestCode, int resultCode, Intent data) {
        mSupportTaskMap.remove(supportTask.getId());

        if(requestCode == REQUEST_AUTHORIZATION_GOOGLE) {
            if(resultCode == Activity.RESULT_OK) {
                if(recoveringTask != null) {
                    supportTask.conclude(true);
                    checkRetries(recoveringTask);
                }
                return;
            }

            Toast.makeText(getContext(), R.string.sender_msg_cancelled_gmail_api, Toast.LENGTH_LONG).show();
        }

        onSupportTaskActivityResult(supportTask, recoveringTask, requestCode, resultCode, data);
    }

    protected void onSupportTaskActivityResult(SenderSupportTask supportTask, SenderUploadTask recoveringTask, int requestCode, int resultCode, Intent data) {
        if(recoveringTask != null) {
            // nothing to recover; must be overriden to include additional behaviour
            supportTask.conclude(false);
            doNotRecover(recoveringTask);
        }

        // just ignore the support task in this case
        mTrackerManager.log("Ignored onActivityResult from " + supportTask.getClass().getSimpleName() + " " + supportTask.getId());
        return;
    }

    protected void startActivityForResult(SenderTask senderTask, int requestCode, Intent dataIntent) {
        if(senderTask instanceof SenderSupportTask) {
            mSupportTaskActivityResultMap.put(senderTask.getId(), (SenderSupportTask) senderTask);
        }

        Intent i = new Intent(mContext, GetResultActivity.class);
        i.putExtra(GetResultActivity.INTENT_ID, senderTask.getId());
        i.putExtra(GetResultActivity.INTENT_REQUESTCODE, requestCode);
        i.putExtra(GetResultActivity.INTENT_DATA, dataIntent);
        i.putExtra(GetResultActivity.INTENT_BROADCAST_TYPE, mBroadcastType);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    /**
     * Return {@link #RECOVERY_DONOTHING} if you are doing some kind of asynchronous recovery work and want to do nothing for now
     * Return {@link #RECOVERY_NOK} if there's no way of recovering and to stop the task
     * Return {@link #RECOVERY_RETRY} to just retry until MAX_RETRIES is exceeded
     *
     * @param failedUploadTask
     * @param error
     * @return
     */
    protected int tryToRecover(SenderUploadTask failedUploadTask, Throwable error) {
        return RECOVERY_RETRY;
    }

    /**
     * Tries to recover from the error thrown by the failed sender task.<br />
     * <b>Do NOT override this method. Instead override {@link #tryToRecover(SenderUploadTask, Throwable)}</b>
     * @param failedSenderTask the failed sender task
     */
    public void tryToRecover(SenderUploadTask failedSenderTask) {
        mRecoveringTaskMap.put(failedSenderTask.getId(), failedSenderTask);

        Throwable e = failedSenderTask.getError();

        // in this case just re-new the access token
        if(e instanceof PeppermintApiInvalidAccessTokenException) {
            authorizePeppermint(failedSenderTask, failedSenderTask.getMessage());
            return;
        }

        // in this case, try to ask for another access token
        if(e instanceof GoogleApiInvalidAccessTokenException) {
            GoogleAuthorizationSupportTask task = new GoogleAuthorizationSupportTask(getSender(), failedSenderTask.getMessage(), null);
            launchSupportTask(task, failedSenderTask);
            return;
        }

        // in this case just ask for permissions
        if(e instanceof GoogleApiNoAuthorizationException) {
            Intent intent = ((GoogleApiNoAuthorizationException) e).getHandleIntent();
            startActivityForResult(failedSenderTask, REQUEST_AUTHORIZATION_GOOGLE, intent);
            return;
        }

        int recoveryCode = tryToRecover(failedSenderTask, e);
        switch (recoveryCode) {
            case RECOVERY_DONOTHING:
                return;
            case RECOVERY_NOK:
                doNotRecover(failedSenderTask);
                return;
            default:
                checkRetries(failedSenderTask);
        }
    }

    /**
     * Tells the {@link Sender} that the error thrown by the failed sender task is not recoverable.
     *
     * Either {@link #doNotRecover(SenderUploadTask)} or {@link #doRecover(SenderUploadTask)} must be invoked
     * for each {@link SenderTask} sent to {@link #tryToRecover(SenderUploadTask)}. <br/>
     *
     * @param recoveringTask the failed/recovering task
     */
    protected void doNotRecover(SenderUploadTask recoveringTask) {
        mRetryMap.remove(recoveringTask.getMessage().getId());
        mRecoveringTaskMap.remove(recoveringTask.getId());
        if(mSenderUploadListener != null) {
            mSenderUploadListener.onSendingUploadRequestNotRecovered(recoveringTask, recoveringTask.getError());
        }
    }

    /**
     * Tells the {@link Sender} that the error thrown by the failed sender task has been
     * handled (recovered).
     *
     * Either {@link #doNotRecover(SenderUploadTask)} or {@link #doRecover(SenderUploadTask)} must be invoked
     * for each {@link SenderTask} sent to {@link #tryToRecover(SenderUploadTask)}. <br/>
     *
     * @param recoveringTask the failed/recovering task
     */
    protected void doRecover(SenderUploadTask recoveringTask) {
        doRecover(recoveringTask, true);
    }

    protected void doRecover(SenderUploadTask recoveringTask, boolean cleanRetries) {
        if(cleanRetries) {
            mRetryMap.remove(recoveringTask.getMessage().getId());
        }
        mRecoveringTaskMap.remove(recoveringTask.getId());
        if(mSenderUploadListener != null) {
            mSenderUploadListener.onSendingUploadRequestRecovered(recoveringTask);
        }
    }

    protected void checkRetries(SenderUploadTask failedSendingTask) {
        UUID taskUuid = failedSendingTask.getId();

        if(!mRetryMap.containsKey(taskUuid)) {
            mRetryMap.put(taskUuid, 1);
        } else {
            mRetryMap.put(taskUuid, mRetryMap.get(taskUuid) + 1);
        }

        // if it has failed MAX_RETRIES times, do not try it anymore
        int retryNum = mRetryMap.get(taskUuid);
        if(retryNum > MAX_RETRIES) {
            mRetryMap.remove(taskUuid);
            doNotRecover(failedSendingTask);
            return;
        }

        mTrackerManager.log("Retry #" + retryNum + " for Task " + taskUuid);

        // just try again for MAX_RETRIES times tops
        doRecover(failedSendingTask, false);
    }

    public Sender getSender() {
        return mSender;
    }

    public void setSender(Sender mSender) {
        this.mSender = mSender;
    }

    protected GoogleApi getGoogleApi(String email) {
        GoogleApi api = (GoogleApi) getParameter(GmailSender.PARAM_GOOGLE_API);
        if(api == null) {
            api = new GoogleApi(getContext());
            setParameter(GmailSender.PARAM_GOOGLE_API, api);
        }
        if(api.getCredential() == null || api.getService() == null || api.getAccountName().compareTo(email) != 0) {
            api.setAccountName(email);
        }
        return api;
    }

    protected void setGoogleApi(GoogleApi googleApi) {
        setParameter(Sender.PARAM_GOOGLE_API, googleApi);
    }

    protected PeppermintApi getPeppermintApi() {
        PeppermintApi api = (PeppermintApi) getParameter(Sender.PARAM_PEPPERMINT_API);
        if(api == null) {
            api = new PeppermintApi();
            setPeppermintApi(api);
        }
        return api;
    }

    protected void setPeppermintApi(PeppermintApi peppermintApi) {
        setParameter(Sender.PARAM_PEPPERMINT_API, peppermintApi);
    }

    protected int tryToRecoverSupport(SenderSupportTask supportTask, Throwable error) {
        return RECOVERY_RETRY;
    }

    protected int supportFinishedOk(SenderSupportTask supportTask) {
        return RECOVERY_RETRY;
    }

    private SenderUploadTask finishSupportAndGetUploadTask(SenderSupportTask supportTask) {
        mSupportTaskMap.remove(supportTask.getId());

        if(supportTask.getMessage() == null) {
            return null;
        }

        SenderUploadTask failedUploadTask = mRecoveringTaskMap.get(supportTask.getRelatedUploadTask().getId());
        if(failedUploadTask == null) {
            Log.w(TAG, "Unable to find support task!");
        }

        return failedUploadTask;
    }

    private final SenderSupportListener mSupportListener = new SenderSupportListener() {

        @Override
        public void onSendingSupportStarted(SenderSupportTask supportTask) {
            mTrackerManager.log("Started SenderSupportTask " + supportTask.getClass().getSimpleName());
        }

        @Override
        public void onSendingSupportCancelled(SenderSupportTask supportTask) {
            mTrackerManager.log("Cancelled SenderSupportTask " + supportTask.getClass().getSimpleName());

            SenderUploadTask uploadTask = finishSupportAndGetUploadTask(supportTask);
            supportTask.conclude(false);
            if(uploadTask != null) {
                doNotRecover(uploadTask);
            }
        }

        @Override
        public void onSendingSupportFinished(SenderSupportTask supportTask) {
            SenderUploadTask uploadTask = finishSupportAndGetUploadTask(supportTask);

            int recoveryCode = supportFinishedOk(supportTask);

            mTrackerManager.log("Finished SenderSupportTask " + supportTask.getClass().getSimpleName() + " Code " + recoveryCode);

            switch (recoveryCode) {
                case RECOVERY_DONOTHING:
                    return;
                case RECOVERY_NOK:
                    supportTask.conclude(false);
                    if(uploadTask != null) {
                        doNotRecover(uploadTask);
                    }
                    return;
                default:
                    supportTask.conclude(true);
                    if(uploadTask != null) {
                        checkRetries(uploadTask);
                    }
            }
        }

        @Override
        public void onSendingSupportError(SenderSupportTask supportTask, Throwable error) {
            mTrackerManager.log("Error at SenderSupportTask " + supportTask.getClass().getSimpleName(), error);

            SenderUploadTask uploadTask = finishSupportAndGetUploadTask(supportTask);

            if(error instanceof UserRecoverableAuthIOException || error instanceof UserRecoverableAuthException) {
                Intent intent = error instanceof UserRecoverableAuthIOException ? ((UserRecoverableAuthIOException) error).getIntent() : ((UserRecoverableAuthException) error).getIntent();
                startActivityForResult(supportTask, REQUEST_AUTHORIZATION_GOOGLE, intent);
                return;
            }

            int recoveryCode = tryToRecoverSupport(supportTask, error);

            switch (recoveryCode) {
                case RECOVERY_DONOTHING:
                    return;
                case RECOVERY_NOK:
                    supportTask.conclude(false);
                    if(uploadTask != null) {
                        doNotRecover(uploadTask);
                    }
                    return;
                default:
                    supportTask.conclude(true);
                    if(uploadTask != null) {
                        checkRetries(uploadTask);
                    }
            }
        }

        @Override
        public void onSendingSupportProgress(SenderSupportTask senderTask, float progressValue) { }
    };
}
