package com.peppermint.app.sending.sms;

import android.content.Context;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.utils.Utils;

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
    public SenderTask newTask(SendingRequest sendingRequest) {
        if (!Utils.isSimAvailable(getContext())) {
            Toast.makeText(getContext(), R.string.msg_message_sms_disabled, Toast.LENGTH_LONG).show();
            throw new UnsupportedSMSException();
        }
        return new SMSSenderTask(this, sendingRequest, getSenderListener(), getParameters(), getSenderPreferences());
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
