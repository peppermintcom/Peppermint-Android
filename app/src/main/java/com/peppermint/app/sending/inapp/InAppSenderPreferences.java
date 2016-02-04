package com.peppermint.app.sending.inapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.sending.mail.MailSenderPreferences;

/**
 * Created by Nuno Luz on 25-11-2015.
 */
public class InAppSenderPreferences extends MailSenderPreferences {
    public InAppSenderPreferences(Context context) {
        super(context);
    }

    public InAppSenderPreferences(Context context, SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
    }
}
