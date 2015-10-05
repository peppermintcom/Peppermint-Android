package com.peppermint.app.sending;

import android.content.Context;

import com.peppermint.app.data.SendingRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * A Sender implementation that sends Recording to Recipients through a particular medium/protocol.
 */
public abstract class Sender {

    private Context mContext;
    private SenderListener mSenderListener;
    private Map<String, Object> mParameters;
    private Sender mFailureChainSender;

    public Sender(Context context, SenderListener senderListener) {
        this.mParameters = new HashMap<>();
        this.mContext = context;
        this.mSenderListener = senderListener;
    }

    public Sender(Context context, SenderListener senderListener, Map<String, Object> parameters) {
        this.mContext = context;
        this.mSenderListener = senderListener;
        this.mParameters = parameters;
    }

    public void init() {
        SendingErrorHandler errorHandler = getErrorHandler();
        if(errorHandler != null) {
            errorHandler.init();
        }
    }

    public abstract SendingTask newTask(SendingRequest sendingRequest);
    public abstract SendingErrorHandler getErrorHandler();
    public abstract SenderPreferences getSenderPreferences();

    public void deinit() {
        SendingErrorHandler errorHandler = getErrorHandler();
        if(errorHandler != null) {
            errorHandler.deinit();
        }
    }

    public Context getContext() {
        return mContext;
    }

    public Sender setContext(Context mContext) {
        this.mContext = mContext;
        return this;
    }

    public SenderListener getSenderListener() {
        return mSenderListener;
    }

    public Sender setSenderListener(SenderListener senderListener) {
        this.mSenderListener = senderListener;
        return this;
    }

    public Map<String, Object> getParameters() {
        return mParameters;
    }

    public Sender setParameters(Map<String, Object> mParameters) {
        this.mParameters = mParameters;
        return this;
    }

    public Object getParameter(String key) {
        if(!mParameters.containsKey(key)) {
            return null;
        }
        return mParameters.get(key);
    }

    public Sender setParameter(String key, Object value) {
        mParameters.put(key, value);
        return this;
    }

    public Sender getFailureChainSender() {
        return mFailureChainSender;
    }

    public void setFailureChainSender(Sender mFailureChainSender) {
        this.mFailureChainSender = mFailureChainSender;
    }
}
