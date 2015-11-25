package com.peppermint.app.sending.mail.nativemail;

import android.content.Intent;
import android.net.Uri;
import android.text.Html;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;
import com.peppermint.app.sending.mail.MailSenderPreferences;
import com.peppermint.app.sending.server.ServerSenderTask;

import java.io.File;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SenderTask that launches a native app to send the audio/video recording through email.
 */
public class IntentMailSenderTask extends SenderTask {

    // FIXME the content type value should be stored in the Recording instance to avoid redundancy
    private static final String CONTENT_TYPE_AUDIO = "audio/mp4";
    private static final String CONTENT_TYPE_VIDEO = "video/mp4";

    public IntentMailSenderTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        super(sender, sendingRequest, listener);
    }

    public IntentMailSenderTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, sendingRequest, listener, parameters, preferences);
    }

    public IntentMailSenderTask(IntentMailSenderTask sendingTask) {
        super(sendingTask);
    }

    @Override
    protected void send() throws Throwable {
        String preferredAccountName = ((MailSenderPreferences) getSenderPreferences()).getPreferredAccountName();
        if(preferredAccountName == null) {
            throw new MailPreferredAccountNotSetException();
        }

        String displayName = ((MailSenderPreferences) getSenderPreferences()).getDisplayName();

        // build the email body
        String url = (String) getSendingRequest().getParameter(ServerSenderTask.PARAM_SHORT_URL);
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("<p>");
        bodyBuilder.append(String.format(getSender().getContext().getString(R.string.default_mail_body_url), url,
                (getSendingRequest().getRecording().hasVideo() ? CONTENT_TYPE_VIDEO : CONTENT_TYPE_AUDIO)));
        bodyBuilder.append("</p><br />");
        bodyBuilder.append(String.format(getSender().getContext().getString(R.string.default_mail_body_reply),
                displayName == null ? "" : URLEncoder.encode(displayName, "UTF-8"),
                URLEncoder.encode(preferredAccountName, "UTF-8")));
        getSendingRequest().setBody(bodyBuilder.toString());

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
