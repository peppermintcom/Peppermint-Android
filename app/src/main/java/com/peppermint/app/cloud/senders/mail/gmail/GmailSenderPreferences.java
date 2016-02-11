package com.peppermint.app.cloud.senders.mail.gmail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.cloud.senders.SenderPreferences;

/**
 * Created by Nuno Luz on 25-11-2015.
 *
 * Sender preferences for the GmailSender.
 */
public class GmailSenderPreferences extends SenderPreferences {

    public GmailSenderPreferences(Context context) {
        super(context);
    }

    public GmailSenderPreferences(Context context, SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
    }
}
