package com.peppermint.app.sending.nativemail;

import android.content.Intent;
import android.net.Uri;
import android.text.Html;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingTask;

import java.io.File;
import java.util.Map;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SendingTask that launches a native app to send the audio/video recording through email.
 */
public class IntentMailSendingTask extends SendingTask {

    public IntentMailSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        super(sender, sendingRequest, listener);
    }

    public IntentMailSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, sendingRequest, listener, parameters, preferences);
    }

    public IntentMailSendingTask(SendingTask sendingTask) {
        super(sendingTask);
    }

    @Override
    protected void send() throws Throwable {
        File file = getSendingRequest().getRecording().getValidatedFile();

        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getSendingRequest().getRecipient().getVia()));
        i.putExtra(Intent.EXTRA_SUBJECT, getSendingRequest().getSubject());
        i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(getSendingRequest().getBody()));
        Uri uri = Uri.parse("file://" + file.getAbsolutePath());
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            getSender().getContext().startActivity(i);
        } catch (android.content.ActivityNotFoundException ex) {
            throw new RuntimeException("There are no email clients installed!");
        }
    }
}
