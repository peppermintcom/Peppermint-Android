package com.peppermint.app.sending;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.data.SendingRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     Handles all errors thrown by a {@link SenderTask}. <br />
 *     A {@link Sender} might be able to recover from some of these errors using a
 *     {@link SenderErrorHandler}.
 * </p>
 * <p>
 *     As with {@link Sender}s, error handlers can be configured through Parameters and Preferences.
 *     Preferences use the native Android Shared Preferences mechanism, so that data is saved
 *     across different executions of the app. Parameters are part of a key-value map passed to
 *     the error handler instance (each implementation may have its own parameters).
 * </p>
 */
public abstract class SenderErrorHandler {

    private Context mContext;

    private UUID mUuid = UUID.randomUUID();
    private Map<UUID, SenderTask> mRecoveringTaskMap;          // holds failed sender tasks that are recovering

    private SenderListener mSenderListener;

    private Map<String, Object> mParameters;
    private SenderPreferences mSenderPreferences;

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
            if(intent != null && intent.hasExtra(GetResultActivity.INTENT_ID) && mRecoveringTaskMap.containsKey(intent.getSerializableExtra(GetResultActivity.INTENT_ID))) {
                onActivityResult(mRecoveringTaskMap.get(intent.getSerializableExtra(GetResultActivity.INTENT_ID)),
                        intent.getIntExtra(GetResultActivity.INTENT_REQUESTCODE, -1),
                        intent.getIntExtra(GetResultActivity.INTENT_RESULTCODE, -1),
                        (Intent) intent.getParcelableExtra(GetResultActivity.INTENT_DATA));
            }
        }
    };
    private final String mBroadcastType = this.getClass().getSimpleName() + "-" + mUuid.toString();
    private final IntentFilter mFilter = new IntentFilter(mBroadcastType);

    public SenderErrorHandler(Context context, SenderListener senderListener) {
        this.mContext = context;
        this.mSenderListener = senderListener;
        this.mParameters = new HashMap<>();
        this.mRecoveringTaskMap = new HashMap<>();
        this.mRetryMap = new HashMap<>();
    }

    public SenderErrorHandler(Context context, SenderListener senderListener, Map<String, Object> parameters) {
        this.mContext = context;
        this.mSenderListener = senderListener;
        this.mRecoveringTaskMap = new HashMap<>();
        this.mRetryMap = new HashMap<>();
        if(parameters != null) {
            this.mParameters = new HashMap<>(parameters);
        }
    }

    public SenderErrorHandler(Context context, SenderListener senderListener, Map<String, Object> parameters, SenderPreferences preferences) {
        this.mContext = context;
        this.mSenderListener = senderListener;
        this.mSenderPreferences = preferences;
        this.mRecoveringTaskMap = new HashMap<>();
        this.mRetryMap = new HashMap<>();
        if(parameters != null) {
            this.mParameters = new HashMap<>(parameters);
        }
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
    }

    protected void onActivityResult(SenderTask recoveringTask, int requestCode, int resultCode, Intent data) {
        doNotRecover(recoveringTask);   // nothing to recover; must be overriden to include additional behaviour
    }

    protected void startActivityForResult(UUID uuid, int requestCode, Intent dataIntent) {
        Intent i = new Intent(mContext, GetResultActivity.class);
        i.putExtra(GetResultActivity.INTENT_ID, uuid);
        i.putExtra(GetResultActivity.INTENT_REQUESTCODE, requestCode);
        i.putExtra(GetResultActivity.INTENT_DATA, dataIntent);
        i.putExtra(GetResultActivity.INTENT_BROADCAST_TYPE, mBroadcastType);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    // this method must be overriden by subclasses to include the recovering code
    // super.tryToRecover(...) must always be invoked
    /**
     * Tries to recover from the error throw by the failed sender task.
     * @param failedSenderTask the failed sender task
     */
    public void tryToRecover(SenderTask failedSenderTask) {
        mRecoveringTaskMap.put(failedSenderTask.getId(), failedSenderTask);
    }

    /**
     * Tells the {@link Sender} that the error thrown by the failed sender task is not recoverable.
     *
     * Either {@link #doNotRecover(SenderTask)} or {@link #doRecover(SenderTask)} must be invoked
     * for each {@link SenderTask} sent to {@link #tryToRecover(SenderTask)}. <br/>
     *
     * @param recoveringTask the failed/recovering task
     */
    protected void doNotRecover(SenderTask recoveringTask) {
        mRecoveringTaskMap.remove(recoveringTask.getId());
        if(mSenderListener != null) {
            mSenderListener.onSendingRequestNotRecovered(recoveringTask, recoveringTask.getError());
        }
    }

    /**
     * Tells the {@link Sender} that the error thrown by the failed sender task has been
     * handled (recovered).
     *
     * Either {@link #doNotRecover(SenderTask)} or {@link #doRecover(SenderTask)} must be invoked
     * for each {@link SenderTask} sent to {@link #tryToRecover(SenderTask)}. <br/>
     *
     * @param recoveringTask the failed/recovering task
     */
    protected void doRecover(SenderTask recoveringTask) {
        mRecoveringTaskMap.remove(recoveringTask.getId());
        if(mSenderListener != null) {
            mSenderListener.onSendingRequestRecovered(recoveringTask);
        }
    }

    protected void checkRetries(SenderTask failedSendingTask) {
        SendingRequest request = failedSendingTask.getSendingRequest();

        if(!mRetryMap.containsKey(request.getId())) {
            mRetryMap.put(request.getId(), 1);
        } else {
            mRetryMap.put(request.getId(), mRetryMap.get(request.getId()) + 1);
        }

        // if it has failed MAX_RETRIES times, do not try it anymore
        int retryNum = mRetryMap.get(request.getId());
        if(retryNum > MAX_RETRIES) {
            mRetryMap.remove(request.getId());
            doNotRecover(failedSendingTask);
            return;
        }

        Crashlytics.log("Retry #" + retryNum + " due to " + failedSendingTask.getError());

        // just try again for MAX_RETRIES times tops
        doRecover(failedSendingTask);
    }

    public Context getContext() {
        return mContext;
    }

    public Map<String, Object> getParameters() {
        return mParameters;
    }

    public void setParameters(Map<String, Object> mParameters) {
        this.mParameters = mParameters;
    }

    public Object getParameter(String key) {
        if(!mParameters.containsKey(key)) {
            return null;
        }
        return mParameters.get(key);
    }

    public void setParameter(String key, Object value) {
        mParameters.put(key, value);
    }

    public SenderPreferences getSenderPreferences() {
        return mSenderPreferences;
    }

    public void setSenderPreferences(SenderPreferences mSenderPreferences) {
        this.mSenderPreferences = mSenderPreferences;
    }

    public UUID getUUID() {
        return mUuid;
    }
}
