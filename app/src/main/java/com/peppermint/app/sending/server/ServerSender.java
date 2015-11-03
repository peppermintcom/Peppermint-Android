package com.peppermint.app.sending.server;

import android.content.Context;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingErrorHandler;
import com.peppermint.app.sending.SendingTask;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Sender that uses the GMail API to send audio messages through email.
 */
public class ServerSender extends Sender {

    protected static final String PARAM_MANAGER = "ServerSender_ParamManager";

    protected ServerClientManager mManager;

    private ServerSendingErrorHandler mErrorHandler;

    public ServerSender(Context context, SenderListener senderListener) {
        super(context, senderListener);
    }

    @Override
    public void init() {
        // initialize the Gmail API objects and pass them as parameters to the error handler
        // and to all associated sending tasks
        mManager = new ServerClientManager(getContext());
        mManager.start();

        setParameter(PARAM_MANAGER, mManager);

        mErrorHandler = new ServerSendingErrorHandler(getContext(), getSenderListener(), getParameters(), getSenderPreferences());

        super.init();
    }

    @Override
    public void deinit() {
        if(mManager != null && mManager.isBound()) {
            mManager.unbind();
        }

        super.deinit();
    }

    @Override
    public SendingTask newTask(SendingRequest sendingRequest) {
        return new ServerSendingTask(this, sendingRequest, getSenderListener(), getParameters(), getSenderPreferences());
    }

    @Override
    public SendingErrorHandler getErrorHandler() {
        return mErrorHandler;
    }

    @Override
    public SenderPreferences getSenderPreferences() {
        return null;
    }

    public ServerClientManager getManager() {
        return mManager;
    }
}
