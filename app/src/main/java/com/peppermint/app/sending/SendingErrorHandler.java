package com.peppermint.app.sending;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     Handles all errors thrown by a {@link SendingTask}. <br />
 *     A {@link Sender} might be able to recover from some of these errors using a
 *     {@link SendingErrorHandler}.
 * </p>
 * <p>
 *     As with {@link Sender}s, error handlers can be configured through Parameters and Preferences.
 *     Preferences use the native Android Shared Preferences mechanism, so that data is saved
 *     across different executions of the app. Parameters are part of a key-value map passed to
 *     the error handler instance (each implementation may have its own parameters).
 * </p>
 */
public abstract class SendingErrorHandler {

    private Context mContext;

    private UUID mUuid = UUID.randomUUID();
    private Map<UUID, SendingTask> mRecoveringTaskMap;          // holds failed sending tasks that are recovering

    private SenderListener mSenderListener;

    private Map<String, Object> mParameters;
    private SenderPreferences mSenderPreferences;

    /** this broadcast receiver gets results from {@link GetResultActivity} **/
    // it allows any SendingErrorHandler to recover from an error by triggering activities
    // this is useful for using APIs that request permissions, such as the Gmail API
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

    public SendingErrorHandler(Context context, SenderListener senderListener) {
        this.mContext = context;
        this.mSenderListener = senderListener;
        this.mParameters = new HashMap<>();
        this.mRecoveringTaskMap = new HashMap<>();
    }

    public SendingErrorHandler(Context context, SenderListener senderListener, Map<String, Object> parameters) {
        this.mContext = context;
        this.mSenderListener = senderListener;
        this.mRecoveringTaskMap = new HashMap<>();
        if(parameters != null) {
            this.mParameters = new HashMap<>(parameters);
        }
    }

    public SendingErrorHandler(Context context, SenderListener senderListener, Map<String, Object> parameters, SenderPreferences preferences) {
        this.mContext = context;
        this.mSenderListener = senderListener;
        this.mSenderPreferences = preferences;
        this.mRecoveringTaskMap = new HashMap<>();
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

    protected void onActivityResult(SendingTask recoveringTask, int requestCode, int resultCode, Intent data) {
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
     * Tries to recover from the error throw by the failed sending task.
     * @param failedSendingTask the failed sending task
     */
    public void tryToRecover(SendingTask failedSendingTask) {
        mRecoveringTaskMap.put(failedSendingTask.getSendingRequest().getId(), failedSendingTask);
    }

    /**
     * Tells the {@link Sender} that the error thrown by the failed sending task is not recoverable.
     *
     * Either {@link #doNotRecover(SendingTask)} or {@link #doRecover(SendingTask)} must be invoked
     * for each {@link SendingTask} sent to {@link #tryToRecover(SendingTask)}. <br/>
     *
     * @param recoveringTask the failed/recovering task
     */
    protected void doNotRecover(SendingTask recoveringTask) {
        mRecoveringTaskMap.remove(recoveringTask.getSendingRequest().getId());
        if(mSenderListener != null) {
            mSenderListener.onSendingRequestNotRecovered(recoveringTask, recoveringTask.getSendingRequest(), recoveringTask.getError());
        }
    }

    /**
     * Tells the {@link Sender} that the error thrown by the failed sending task has been
     * handled (recovered).
     *
     * Either {@link #doNotRecover(SendingTask)} or {@link #doRecover(SendingTask)} must be invoked
     * for each {@link SendingTask} sent to {@link #tryToRecover(SendingTask)}. <br/>
     *
     * @param recoveringTask the failed/recovering task
     */
    protected void doRecover(SendingTask recoveringTask) {
        mRecoveringTaskMap.remove(recoveringTask.getSendingRequest().getId());
        if(mSenderListener != null) {
            mSenderListener.onSendingRequestRecovered(recoveringTask, recoveringTask.getSendingRequest());
        }
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
