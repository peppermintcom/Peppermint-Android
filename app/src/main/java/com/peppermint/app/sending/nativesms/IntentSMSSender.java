package com.peppermint.app.sending.nativesms;

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
 * Sender messages using a native SMS app.
 */
public class IntentSMSSender extends Sender {

    public IntentSMSSender(Context context, SenderListener senderListener) {
        super(context, senderListener);
    }

    @Override
    public SenderTask newTask(SendingRequest sendingRequest) {
        return new IntentSMSSenderTask(this, sendingRequest, getSenderListener(), getParameters(), getSenderPreferences());
    }

    @Override
    public SenderErrorHandler getErrorHandler() {
        return null;
    }

    @Override
    public SenderPreferences getSenderPreferences() {
        return null;
    }
}
