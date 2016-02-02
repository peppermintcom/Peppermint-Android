package com.peppermint.app.sending;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.peppermint.app.authenticator.AuthenticatorActivity;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.api.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.sending.api.exceptions.PeppermintApiNoAccountException;

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

    public static boolean IS_APP_ON_BACKGROUND = false;

    private static final int REQUEST_CODE_AUTH = 98765;

    protected static final int RECOVERY_RETRY = -1;
    protected static final int RECOVERY_NOK = -2;
    protected static final int RECOVERY_DONOTHING = 1;

    private Map<UUID, SenderUploadTask> mRecoveringTaskMap;          // holds failed sender tasks that are recovering

    private ThreadPoolExecutor mExecutor;                           // a thread pool for sending tasks
    private Map<UUID, SenderSupportTask> mSupportTaskMap;           // map of support tasks under execution
    private Map<UUID, SenderSupportTask> mSupportTaskActivityResultMap;

    private SenderUploadListener mSenderUploadListener;
    private Sender mSender;

    // allow the sender task to be executed up to MAX_RETRIES+1 times
    private static final int MAX_RETRIES = 9;
    protected Map<UUID, Integer> mRetryMap;

    /**
     * This broadcast receiver gets results from {@link GetResultActivity}<br />
     * It allows any SenderErrorHandler to recover from an error by triggering activities.<br />
     * This is useful for using APIs that request permissions through another Activity, such as the Gmail API
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

                recoveringTask = null;
                SenderSupportTask supportTask = mSupportTaskActivityResultMap.remove(taskId);
                if(supportTask != null) {
                    if(supportTask.getSendingRequest() != null && mRecoveringTaskMap.containsKey(supportTask.getSendingRequest().getId())) {
                        recoveringTask = mRecoveringTaskMap.get(supportTask.getSendingRequest().getId());
                    }
                    handleOnSupportTaskActivityResult(supportTask, recoveringTask,
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
        this.mRecoveringTaskMap = new HashMap<>();
        this.mRetryMap = new HashMap<>();
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
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, mFilter);
    }

    /**
     * De-initializes the error handler.
     */
    public void deinit() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        mSupportTaskActivityResultMap.clear();
    }

    public void authorizePeppermint(SendingRequest sendingRequest) {
        PeppermintAuthorizationSupportTask task = new PeppermintAuthorizationSupportTask(getSender(), sendingRequest, null);
        launchSupportTask(task);
    }

    public void authorize(SendingRequest sendingRequest) {
        /* nothing to do here */
    }

    protected UUID launchSupportTask(SenderSupportTask task) {
        mSupportTaskMap.put(task.getId(), task);
        task.setSenderSupportListener(mSupportListener);
        task.executeOnExecutor(mExecutor);
        return task.getId();
    }

    protected void onUploadTaskActivityResult(SenderUploadTask recoveringTask, int requestCode, int resultCode, Intent data) {
         doNotRecover(recoveringTask);   // nothing to recover; must be overriden to include additional behaviour
    }

    private void handleOnSupportTaskActivityResult(SenderSupportTask supportTask, SenderUploadTask recoveringTask, int requestCode, int resultCode, Intent data) {
        mSupportTaskMap.remove(supportTask.getId());

        if(requestCode == REQUEST_CODE_AUTH) {
            if(recoveringTask != null) {
                if(resultCode == Activity.RESULT_OK) {
                    checkRetries(recoveringTask);
                } else {
                    doNotRecover(recoveringTask);
                }
            }
            return;
        }

        onSupportTaskActivityResult(supportTask, recoveringTask, requestCode, resultCode, data);
    }

    protected void onSupportTaskActivityResult(SenderSupportTask supportTask, SenderUploadTask recoveringTask, int requestCode, int resultCode, Intent data) {
        if(recoveringTask != null) {
            // nothing to recover; must be overriden to include additional behaviour
            doNotRecover(recoveringTask);
        }

        // just ignore the support task in this case
        // sub-classes should override this method so that this never happens
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
     * Tries to recover from the error throw by the failed sender task.<br />
     * <b>Do NOT override this method. Instead override {@link #tryToRecover(SenderUploadTask, Throwable)}</b>
     * @param failedSenderTask the failed sender task
     */
    public void tryToRecover(SenderUploadTask failedSenderTask) {
        mRecoveringTaskMap.put(failedSenderTask.getId(), failedSenderTask);

        Throwable e = failedSenderTask.getError();

        // in this case just re-new the access token
        if(e instanceof PeppermintApiInvalidAccessTokenException) {
            authorizePeppermint(failedSenderTask.getSendingRequest());
            return;
        }

        if(e instanceof PeppermintApiNoAccountException) {
            if(!IS_APP_ON_BACKGROUND) {
                Intent intent = new Intent(getContext(), AuthenticatorActivity.class);
                startActivityForResult(failedSenderTask, REQUEST_CODE_AUTH, intent);
                return;
            } else {
                doNotRecover(failedSenderTask);
                return;
            }
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
        mRetryMap.remove(recoveringTask.getId());
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
        mRecoveringTaskMap.remove(recoveringTask.getId());
        if(mSenderUploadListener != null) {
            mSenderUploadListener.onSendingUploadRequestRecovered(recoveringTask);
        }
    }

    protected void checkRetries(SenderUploadTask failedSendingTask) {

        if(!mRetryMap.containsKey(failedSendingTask.getId())) {
            mRetryMap.put(failedSendingTask.getId(), 1);
        } else {
            mRetryMap.put(failedSendingTask.getId(), mRetryMap.get(failedSendingTask.getId()) + 1);
        }

        // if it has failed MAX_RETRIES times, do not try it anymore
        int retryNum = mRetryMap.get(failedSendingTask.getId());
        if(retryNum > MAX_RETRIES) {
            mRetryMap.remove(failedSendingTask.getId());
            doNotRecover(failedSendingTask);
            return;
        }

        mTrackerManager.log("Retry #" + retryNum + " for SenderUploadTask " + failedSendingTask.getId());

        // just try again for MAX_RETRIES times tops
        doRecover(failedSendingTask);
    }

    public Sender getSender() {
        return mSender;
    }

    public void setSender(Sender mSender) {
        this.mSender = mSender;
    }

    protected PeppermintApi getPeppermintApi() {
        return (PeppermintApi) getParameter(Sender.PARAM_PEPPERMINT_API);
    }

    protected GoogleApi getGoogleApi() {
        return (GoogleApi) getParameter(Sender.PARAM_GOOGLE_API);
    }

    protected int tryToRecoverSupport(SenderSupportTask supportTask, Throwable error) {
        return RECOVERY_RETRY;
    }

    protected int supportFinishedOk(SenderSupportTask supportTask) {
        return RECOVERY_RETRY;
    }

    private SenderUploadTask finishSupportAndGetUploadTask(SenderSupportTask supportTask) {
        mSupportTaskMap.remove(supportTask.getId());

        if(supportTask.getSendingRequest() == null) {
            return null;
        }

        SenderUploadTask failedUploadTask = mRecoveringTaskMap.get(supportTask.getSendingRequest().getId());
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

            if(error instanceof PeppermintApiNoAccountException) {
                if(!IS_APP_ON_BACKGROUND) {
                    Intent intent = new Intent(getContext(), AuthenticatorActivity.class);
                    startActivityForResult(supportTask, REQUEST_CODE_AUTH, intent);
                    return;
                } else {
                    supportTask.conclude(false);
                    if(uploadTask != null) {
                        doNotRecover(uploadTask);
                    }
                    return;
                }
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
