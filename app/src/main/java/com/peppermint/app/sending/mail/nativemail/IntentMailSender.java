package com.peppermint.app.sending.mail.nativemail;

import android.content.Context;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for emails using a native email app.
 */
public class IntentMailSender extends Sender {

    private IntentMailSenderPreferences mPreferences;
    private IntentMailSenderErrorHandler mErrorHandler;

    public IntentMailSender(Context context, SenderListener senderListener) {
        super(context, senderListener);
        mPreferences = new IntentMailSenderPreferences(getContext());
    }

    @Override
    public SenderTask newTask(SendingRequest sendingRequest) {
        return new IntentMailSenderTask(this, sendingRequest, getSenderListener(), getParameters(), getSenderPreferences());
    }

    @Override
    public SenderErrorHandler getErrorHandler() {
        if(mErrorHandler == null) {
            mErrorHandler = new IntentMailSenderErrorHandler(getContext(), getSenderListener(), getParameters(), getSenderPreferences());
        }
        return mErrorHandler;
    }

    @Override
    public SenderPreferences getSenderPreferences() {
        return mPreferences;
    }
}
