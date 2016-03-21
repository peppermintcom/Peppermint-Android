package com.peppermint.app.cloud.senders.sms.nativesms;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.peppermint.app.R;
import com.peppermint.app.cloud.senders.Sender;
import com.peppermint.app.cloud.senders.SenderUploadListener;
import com.peppermint.app.cloud.senders.SenderUploadTask;
import com.peppermint.app.data.Message;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SenderTask that launches a native app to send the audio/video message through SMS.
 */
public class IntentSMSSenderTask extends SenderUploadTask {

    public IntentSMSSenderTask(IntentSMSSenderTask uploadTask) {
        super(uploadTask);
    }

    public IntentSMSSenderTask(Sender sender, Message message, SenderUploadListener senderUploadListener) {
        super(sender, message, senderUploadListener);
    }

    @Override
    protected void execute() throws Throwable {
        setupPeppermintAuthentication();
        uploadPeppermintMessage();

        // send in-app
        sendPeppermintMessage();

        String url = getMessage().getServerShortUrl();

        Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("smsto", getMessage().getChatParameter().getRecipientList().get(0).getVia(), null));
        sendIntent.putExtra("sms_body", String.format(getSender().getContext().getString(R.string.sender_default_sms_body), url));
        sendIntent.setType("vnd.android-dir/mms-sms");
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            getSender().getContext().startActivity(sendIntent);
        } catch(ActivityNotFoundException e) {
            // try different action
            sendIntent = new Intent(Intent.ACTION_SENDTO);
            sendIntent.addCategory(Intent.CATEGORY_DEFAULT);
            sendIntent.setType("vnd.android-dir/mms-sms");
            sendIntent.setData(Uri.parse("sms:" + getMessage().getChatParameter().getRecipientList().get(0).getVia()));
            sendIntent.putExtra("sms_body", String.format(getSender().getContext().getString(R.string.sender_default_sms_body), url));
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getSender().getContext().startActivity(sendIntent);
        }
    }
}
