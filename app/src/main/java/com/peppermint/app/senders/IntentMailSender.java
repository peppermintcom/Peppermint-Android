package com.peppermint.app.senders;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;

import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;

import de.greenrobot.event.EventBus;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for Emails using the GMail API.
 */
public class IntentMailSender extends Sender {

    public IntentMailSender(Context context, EventBus eventBus, ThreadPoolExecutor executor) {
        super(context, eventBus, executor);
    }

    @Override
    public void send(String toName, String to, String subject, String bodyText, String filePath, String contentType) throws Throwable {
        File file = validateFile(filePath);

        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + to));
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(bodyText));
        Uri uri = Uri.parse("file://" + file.getAbsolutePath());
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            mContext.startActivity(i);
        } catch (android.content.ActivityNotFoundException ex) {
            throw new RuntimeException("There are no email clients installed!");
        }
    }
}
