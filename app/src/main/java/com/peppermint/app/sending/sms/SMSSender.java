package com.peppermint.app.sending.sms;

import android.content.Context;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingErrorHandler;
import com.peppermint.app.sending.SendingTask;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for SMS/text messages using the native Android API.
 */
public class SMSSender extends Sender {

    public SMSSender(Context context, SenderListener senderListener) {
        super(context, senderListener);
    }

    @Override
    public SendingTask newTask(SendingRequest sendingRequest) {
        return new SMSSendingTask(this, sendingRequest, getSenderListener(), getParameters(), getSenderPreferences());
    }

    @Override
    public SendingErrorHandler getErrorHandler() {
        return null;
    }

    @Override
    public SenderPreferences getSenderPreferences() {
        return null;
    }
}
