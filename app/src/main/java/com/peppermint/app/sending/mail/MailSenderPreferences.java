package com.peppermint.app.sending.mail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.sending.SenderPreferences;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * Preferences for Google Account.
 */
public class MailSenderPreferences extends SenderPreferences {

    public MailSenderPreferences(Context context) {
        super(context);
    }

    public MailSenderPreferences(Context context, SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
    }
}
