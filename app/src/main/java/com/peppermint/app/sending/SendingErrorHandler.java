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
 *
 * Handles all errors thrown by a SenderTask.
 * The sender might be able to recover from some of these errors. Here lies the code responsible for handling these.
 */
public abstract class SendingErrorHandler {

    private UUID mUuid = UUID.randomUUID();
    private Context mContext;
    private Map<UUID, SendingTask> mRecoveringTaskMap;
    private Map<String, Object> mParameters;
    private SenderListener mSenderListener;
    private SenderPreferences mSenderPreferences;

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

    public void init() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, mFilter);
    }

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

    public void tryToRecover(SendingTask failedSendingTask) {
        mRecoveringTaskMap.put(failedSendingTask.getSendingRequest().getId(), failedSendingTask);
    }

    protected void doNotRecover(SendingTask recoveringTask) {
        mRecoveringTaskMap.remove(recoveringTask.getSendingRequest().getId());
        if(mSenderListener != null) {
            mSenderListener.onSendingRequestNotRecovered(recoveringTask, recoveringTask.getSendingRequest());
        }
    }

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
