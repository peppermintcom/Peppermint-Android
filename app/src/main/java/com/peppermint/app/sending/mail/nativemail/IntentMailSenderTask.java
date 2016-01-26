package com.peppermint.app.sending.mail.nativemail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.text.Html;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;
import com.peppermint.app.sending.mail.MailSenderPreferences;
import com.peppermint.app.sending.mail.MailUtils;

import java.io.File;
import java.util.List;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SenderTask that launches a native app to send the audio/video recording through email.
 */
public class IntentMailSenderTask extends SenderUploadTask {

    public IntentMailSenderTask(IntentMailSenderTask uploadTask) {
        super(uploadTask);
    }

    public IntentMailSenderTask(Sender sender, SendingRequest sendingRequest, SenderUploadListener senderUploadListener) {
        super(sender, sendingRequest, senderUploadListener);
    }

    @Override
    protected void execute() throws Throwable {
        String preferredAccountName = ((MailSenderPreferences) getSenderPreferences()).getPreferredAccountName();
        if(preferredAccountName == null) {
            throw new MailPreferredAccountNotSetException();
        }

        uploadPeppermintMessageDoChecks();
        uploadPeppermintMessage();
        String url = getSendingRequest().getServerShortUrl();
        String displayName = ((MailSenderPreferences) getSenderPreferences()).getFullName();

        // build the email body
        getSendingRequest().setBody(MailUtils.buildEmailFromTemplate(getContext(), R.raw.email_template_simple, url,
                getSendingRequest().getRecording().getDurationMillis(),
                getSendingRequest().getRecording().getContentType(),
                displayName, preferredAccountName, false));

        File file = getSendingRequest().getRecording().getFile();

        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getSendingRequest().getRecipient().getVia()));
        i.putExtra(Intent.EXTRA_SUBJECT, getSendingRequest().getSubject());
        i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(getSendingRequest().getBody()));
        Uri fileUri = FileProvider.getUriForFile(getContext(), "com.peppermint.app", file);
       /* i.addFlags();*/
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
            getContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }
}
