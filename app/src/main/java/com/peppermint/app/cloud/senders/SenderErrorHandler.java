package com.peppermint.app.cloud.senders;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.peppermint.app.cloud.apis.exceptions.GoogleApiNoAuthorizationException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    protected static final int RECOVERY_RETRY = -1;
    protected static final int RECOVERY_NOK = -2;
    protected static final int RECOVERY_DONOTHING = 1;

    // allow the sender task to be executed up to MAX_RETRIES+1 times
    private static final int MAX_RETRIES = 9;

    // FIXME retry map gets filled and there's no mechanism to clean it up properly!
    private Map<UUID, Integer> mRetryMap;                               // map of upload task ids and their about of retries
    private Map<UUID, SenderUploadTask> mRecoveringTaskMap;             // map of upload tasks that are recovering

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
                }
            }
        }
    };
    private final String mBroadcastType = this.getClass().getSimpleName() + "-" + getId().toString();
    private final IntentFilter mFilter = new IntentFilter(mBroadcastType);

    public SenderErrorHandler(final Sender sender, final SenderUploadListener senderUploadListener) {
        super(sender);
        construct(sender, senderUploadListener);
    }

    private void construct(final Sender sender, final SenderUploadListener senderUploadListener) {
        this.mSender = sender;
        this.mSenderUploadListener = senderUploadListener;

        // upload task maps
        this.mRetryMap = new HashMap<>();
        this.mRecoveringTaskMap = new HashMap<>();
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
    }

    protected void onUploadTaskActivityResult(final SenderUploadTask recoveringTask, final int requestCode, final int resultCode, final Intent resultData) {
        if(requestCode == REQUEST_AUTHORIZATION_GOOGLE) {
            if(resultCode == Activity.RESULT_OK) {
                checkRetries(recoveringTask);
                return;
            }
        }

        doNotRecover(recoveringTask);   // nothing to recover; must be overriden to include additional behaviour
    }

    protected void startActivityForResult(SenderTask senderTask, int requestCode, Intent dataIntent) {
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
     * @param failedUploadTask the failed upload task
     * @param error the error thrown by the task
     * @return the action to perform with the failed upload task
     */
    protected int tryToRecover(final SenderUploadTask failedUploadTask, final Throwable error) {
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

        if(e instanceof UserRecoverableAuthIOException || e instanceof UserRecoverableAuthException) {
            Intent intent = e instanceof UserRecoverableAuthIOException ? ((UserRecoverableAuthIOException) e).getIntent() : ((UserRecoverableAuthException) e).getIntent();
            startActivityForResult(failedSenderTask, REQUEST_AUTHORIZATION_GOOGLE, intent);
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
     * Either this or {@link #doRecover(SenderUploadTask, boolean)} must be invoked
     * for each {@link SenderTask} sent to {@link #tryToRecover(SenderUploadTask)}. <br/>
     *
     * @param recoveringTask the failed/recovering task
     */
    protected void doNotRecover(SenderUploadTask recoveringTask) {
        mRetryMap.remove(recoveringTask.getMessage().getUUID());
        mRecoveringTaskMap.remove(recoveringTask.getId());
        if(mSenderUploadListener != null) {
            mSenderUploadListener.onSendingUploadRequestNotRecovered(recoveringTask, recoveringTask.getError());
        }
    }

    /**
     * Tells the {@link Sender} that the error thrown by the failed sender task has been
     * handled (recovered).
     *
     * Either this or {@link #doNotRecover(SenderUploadTask)} must be invoked
     * for each {@link SenderTask} sent to {@link #tryToRecover(SenderUploadTask)}. <br/>
     *
     * @param recoveringTask the failed/recovering task
     */
    protected void doRecover(SenderUploadTask recoveringTask, boolean cleanRetries) {
        if(cleanRetries) {
            mRetryMap.remove(recoveringTask.getMessage().getUUID());
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

}
