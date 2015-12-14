package com.peppermint.app.sending.mail;

import android.content.Context;

import com.peppermint.app.R;
import com.peppermint.app.utils.ScriptFileReader;
import com.peppermint.app.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by Nuno Luz on 11-12-2015.
 *
 * Utility class for email senders.
 */
public class MailUtils {

    public static String buildEmailFromTemplate(Context context, int templateResId, String playUrl, long durationInMillis, String contentType, String replyName, String replyEmail)
            throws UnsupportedEncodingException {

        StringBuilder bodyBuilder = new StringBuilder();

        /*bodyBuilder.append(String.format(context.getString(R.string.default_mail_body), playUrl,
                Utils.getFriendlyDuration(durationInMillis),
                contentType,
                replyName == null ? "" : URLEncoder.encode(replyName, "UTF-8"),
                URLEncoder.encode(replyEmail, "UTF-8")));*/

        ScriptFileReader templateReader = new ScriptFileReader(context, templateResId);
        templateReader.open();

        try {
            String line = null;
            while((line = templateReader.nextLine()) != null) {
                if(line.contains("{@")) {
                    line = line.replace("{@PLAY_LINK}", playUrl)
                            .replace("{@DURATION}", Utils.getFriendlyDuration(durationInMillis))
                            .replace("{@MIME_TYPE}", contentType)
                            .replace("{@REPLY_NAME}", replyName == null ? "" : URLEncoder.encode(replyName, "UTF-8"))
                            .replace("{@REPLY_EMAIL}", URLEncoder.encode(replyEmail, "UTF-8"));
                }
                bodyBuilder.append(line);
                bodyBuilder.append("\n");
            }

        } finally {
            templateReader.close();
        }

        return bodyBuilder.toString();
    }

}
