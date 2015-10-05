package com.peppermint.app.sending.sms;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingTask;

import java.util.Map;

/**
 * Created by Nuno Luz on 02-10-2015.
 */
public class SMSSendingTask extends SendingTask {

    public SMSSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        super(sender, sendingRequest, listener);
    }

    public SMSSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, sendingRequest, listener, parameters, preferences);
    }

    public SMSSendingTask(SendingTask sendingTask) {
        super(sendingTask);
    }

    @Override
    protected void send() throws Throwable {
        // TODO implement SMS send
    }
}
