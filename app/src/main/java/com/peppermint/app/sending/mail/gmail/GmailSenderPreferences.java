package com.peppermint.app.sending.mail.gmail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.sending.mail.MailSenderPreferences;

/**
 * Created by Nuno Luz on 25-11-2015.
 */
public class GmailSenderPreferences extends MailSenderPreferences {
    public GmailSenderPreferences(Context context) {
        super(context);
    }

    public GmailSenderPreferences(Context context, SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
    }
}
