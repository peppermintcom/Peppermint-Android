package com.peppermint.app.sending.mail.nativemail;

import android.content.Context;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingErrorHandler;
import com.peppermint.app.sending.SendingTask;
import com.peppermint.app.sending.mail.MailSenderPreferences;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for emails using a native email app.
 */
public class IntentMailSender extends Sender {

    private MailSenderPreferences mPreferences;
    private IntentMailSendingErrorHandler mErrorHandler;

    public IntentMailSender(Context context, SenderListener senderListener) {
        super(context, senderListener);
        mPreferences = new MailSenderPreferences(getContext());
    }

    @Override
    public SendingTask newTask(SendingRequest sendingRequest) {
        return new IntentMailSendingTask(this, sendingRequest, getSenderListener(), getParameters(), getSenderPreferences());
    }

    @Override
    public SendingErrorHandler getErrorHandler() {
        if(mErrorHandler == null) {
            mErrorHandler = new IntentMailSendingErrorHandler(getContext(), getSenderListener(), getParameters(), getSenderPreferences());
        }
        return mErrorHandler;
    }

    @Override
    public SenderPreferences getSenderPreferences() {
        return mPreferences;
    }
}
