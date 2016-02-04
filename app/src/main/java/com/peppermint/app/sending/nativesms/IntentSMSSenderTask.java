package com.peppermint.app.sending.nativesms;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.peppermint.app.R;
import com.peppermint.app.data.Message;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SenderTask that launches a native app to send the audio/video recording through SMS.
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
        String url = getMessage().getServerShortUrl();

        Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("smsto", getMessage().getRecipient().getVia(), null));
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
            sendIntent.setData(Uri.parse("sms:" + getMessage().getRecipient().getVia()));
            sendIntent.putExtra("sms_body", String.format(getSender().getContext().getString(R.string.sender_default_sms_body), url));
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getSender().getContext().startActivity(sendIntent);
        }
    }
}
