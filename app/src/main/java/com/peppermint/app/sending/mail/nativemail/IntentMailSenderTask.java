package com.peppermint.app.sending.mail.nativemail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;
import com.peppermint.app.sending.mail.MailSenderPreferences;
import com.peppermint.app.sending.mail.MailUtils;
import com.peppermint.app.sending.server.ServerSenderTask;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SenderTask that launches a native app to send the audio/video recording through email.
 */
public class IntentMailSenderTask extends SenderTask {

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

        String displayName = ((MailSenderPreferences) getSenderPreferences()).getFullName();

        // build the email body
        String url = (String) getSendingRequest().getParameter(ServerSenderTask.PARAM_SHORT_URL);
        getSendingRequest().setBody(MailUtils.buildEmailFromTemplate(getContext(), R.raw.email_template_simple, url,
                getSendingRequest().getRecording().getDurationMillis(),
                getSendingRequest().getRecording().getContentType(),
                displayName, preferredAccountName, false));

        File file = getSendingRequest().getRecording().getValidatedFile();

        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getSendingRequest().getRecipient().getVia()));
        i.putExtra(Intent.EXTRA_SUBJECT, getSendingRequest().getSubject());
        i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(getSendingRequest().getBody()));
        Uri fileUri = FileProvider.getUriForFile(getContext(), "com.peppermint.app", file);
       /* i.addFlags();*/
        Log.d(IntentMailSenderTask.class.getSimpleName(), "$$ " + fileUri.toString());
        i.putExtra(Intent.EXTRA_STREAM, fileUri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

        grantReadPermission(i, fileUri);

        try {
            getSender().getContext().startActivity(i);
        } catch (android.content.ActivityNotFoundException ex) {
            throw new RuntimeException("There are no email clients installed!", ex);
        }
    }

    private void grantReadPermission(Intent i, Uri uri) {
        List<ResolveInfo> resInfoList = getContext().getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            getContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }
}
