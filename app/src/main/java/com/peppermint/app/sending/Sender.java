package com.peppermint.app.sending;

import android.content.Context;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.rest.HttpClientManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     A Sender abstract implementation that sends {@link com.peppermint.app.data.Recording} to
 *     {@link com.peppermint.app.data.Recipient} through a particular medium/protocol.
 *</p>
 * <p>
 *     Senders can be configured through Parameters and Preferences. Preferences use the native
 *     Android Shared Preferences mechanism, so that data is saved across different executions
 *     of the app. Parameters are part of a key-value map passed to the sender instance (each
 *     implementation may have its own parameters).
 *</p>
 * <p>
 *     If a sender fails to execute a request, its failure chain sender can be used to try and
 *     do it afterwards. Several senders can be setup to form a failure chain. *
 * </p>
 */
public abstract class Sender {

    protected static final String PARAM_HTTP_CLIENT_MANAGER = "Sender_ParamHttpClientManager";

    private Context mContext;
    private SenderListener mSenderListener;
    private Map<String, Object> mParameters;
    private Sender mFailureChainSender;
    private Sender mSuccessChainSender;

    private HttpClientManager mHttpManager;
    private boolean mUseHttpManager = false;

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

    /**
     * Initializes the sender and its error handler.
     */
    public void init() {
        SendingErrorHandler errorHandler = getErrorHandler();
        if(errorHandler != null) {
            errorHandler.init();
        }

        if(mUseHttpManager) {
            mHttpManager = new HttpClientManager(mContext);
            mHttpManager.start();

            setParameter(PARAM_HTTP_CLIENT_MANAGER, mHttpManager);
        }
    }

    /**
     * Creates a new {@link SendingTask} instance for the specified sending request.
     * @param sendingRequest the sending request
     * @return the asynchronous task that will try to execute the sending request
     */
    public abstract SendingTask newTask(SendingRequest sendingRequest);

    /**
     * Gets the sender's error handler. Useful to check if the sender can recover from
     * an error triggered during the sending task execution. If it recovers, a new sending task
     * for the same request can be launched to see if it everything runs smoothly the second time.
     *
     * @return the error handler
     */
    public abstract SendingErrorHandler getErrorHandler();

    /**
     * Android shared preferences used by this sender.
     *
     * @return the sender preferences instance
     */
    public abstract SenderPreferences getSenderPreferences();

    /**
     * De-initializes the sender and its error handler.
     */
    public void deinit() {
        SendingErrorHandler errorHandler = getErrorHandler();
        if(errorHandler != null) {
            errorHandler.deinit();
        }

        if(mHttpManager != null && mHttpManager.isBound()) {
            mHttpManager.unbind();
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

    public Sender getSuccessChainSender() {
        return mSuccessChainSender;
    }

    public void setSuccessChainSender(Sender mSuccessChainSender) {
        this.mSuccessChainSender = mSuccessChainSender;
    }

    public boolean isUseHttpManager() {
        return mUseHttpManager;
    }

    public void setUseHttpManager(boolean mUseHttpManager) {
        this.mUseHttpManager = mUseHttpManager;
    }
}
