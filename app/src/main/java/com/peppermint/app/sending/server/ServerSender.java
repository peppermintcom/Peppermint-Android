package com.peppermint.app.sending.server;

import android.content.Context;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.rest.HttpAsyncTask;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.mail.MailSenderPreferences;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Sender that interacts with Peppermint's backend.
 */
public class ServerSender extends Sender {

    protected static final String PARAM_MANAGER = "ServerSender_ParamManager";

    protected ServerClientManager mManager;

    private MailSenderPreferences mPreferences;
    private ServerSenderErrorHandler mErrorHandler;

    public ServerSender(Context context, SenderListener senderListener) {
        super(context, senderListener);
        mPreferences = new MailSenderPreferences(getContext());
    }

    @Override
    public void init() {
        // initialize the ServerClientManager
        mManager = new ServerClientManager(getContext());
        mManager.start();

        setParameter(PARAM_MANAGER, mManager);
        setParameter(HttpAsyncTask.PARAM_HTTP_CLIENT_MANAGER, mManager);

        mErrorHandler = new ServerSenderErrorHandler(getContext(), getSenderListener(), getParameters(), getSenderPreferences());

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
    public SenderTask newTask(SendingRequest sendingRequest) {
        return new ServerSenderTask(this, sendingRequest, getSenderListener(), getParameters(), getSenderPreferences());
    }

    @Override
    public SenderErrorHandler getErrorHandler() {
        return mErrorHandler;
    }

    @Override
    public SenderPreferences getSenderPreferences() {
        return mPreferences;
    }

    public ServerClientManager getManager() {
        return mManager;
    }
}
