package com.peppermint.app.cloud.senders.mail.nativemail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.cloud.apis.SparkPostApi;
import com.peppermint.app.cloud.senders.Sender;
import com.peppermint.app.cloud.senders.SenderUploadListener;
import com.peppermint.app.cloud.senders.SenderUploadTask;
import com.peppermint.app.data.Message;

import java.io.File;
import java.util.List;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SenderUploadTask that launches a native app to send the audio/video message through email.
 */
public class IntentMailSenderTask extends SenderUploadTask {

    public IntentMailSenderTask(IntentMailSenderTask uploadTask) {
        super(uploadTask);
    }

    public IntentMailSenderTask(Sender sender, Message message, SenderUploadListener senderUploadListener) {
        super(sender, message, senderUploadListener);
    }

    @Override
    protected void execute() throws Throwable {
        AuthenticationData data = setupPeppermintAuthentication();
        uploadPeppermintMessage();
        if(sendPeppermintMessage()) {
            return;
        }

        if(isCancelled()) { return; }

        String url = getMessage().getServerShortUrl();
        String displayName = getSenderPreferences().getFullName();

        // build the email body
        final SparkPostApi sparkPostApi = getSparkPostApi();
        getMessage().setEmailBody(sparkPostApi.buildEmailFromTemplate(getContext(), url, getMessage().getServerCanonicalUrl(), displayName, data.getEmail(), SparkPostApi.TYPE_TEXT, true, getId().toString()));

        File file = getMessage().getRecordingParameter().getFile();

        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getMessage().getChatParameter().getRecipientList().get(0).getVia()));
        i.putExtra(Intent.EXTRA_SUBJECT, getMessage().getEmailSubject());
        i.putExtra(Intent.EXTRA_TEXT, getMessage().getEmailBody());
        Uri fileUri = FileProvider.getUriForFile(getContext(), "com.peppermint.app", file);
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
