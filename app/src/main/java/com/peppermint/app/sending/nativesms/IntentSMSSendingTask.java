package com.peppermint.app.sending.nativesms;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingTask;
import com.peppermint.app.sending.server.ServerSendingTask;

import java.util.Map;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SendingTask that launches a native app to send the audio/video recording through SMS.
 */
public class IntentSMSSendingTask extends SendingTask {

    public IntentSMSSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        super(sender, sendingRequest, listener);
    }

    public IntentSMSSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, sendingRequest, listener, parameters, preferences);
    }

    public IntentSMSSendingTask(SendingTask sendingTask) {
        super(sendingTask);
    }

    @Override
    protected void send() throws Throwable {
        String url = (String) getSendingRequest().getParameter(ServerSendingTask.PARAM_SHORT_URL);
        Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("smsto", getSendingRequest().getRecipient().getVia(), null));
        sendIntent.putExtra("sms_body", String.format(getSender().getContext().getString(R.string.default_sms_body), url));
        sendIntent.setType("vnd.android-dir/mms-sms");
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            getSender().getContext().startActivity(sendIntent);
        } catch(ActivityNotFoundException e) {
            sendIntent = new Intent(Intent.ACTION_SENDTO);
            sendIntent.addCategory(Intent.CATEGORY_DEFAULT);
            sendIntent.setType("vnd.android-dir/mms-sms");
            sendIntent.setData(Uri.parse("sms:" + getSendingRequest().getRecipient().getVia()));
            sendIntent.putExtra("sms_body", String.format(getSender().getContext().getString(R.string.default_sms_body), url));
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getSender().getContext().startActivity(sendIntent);
        }
    }
}
