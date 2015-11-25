package com.peppermint.app.sending.mail.nativemail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.sending.mail.MailSenderPreferences;

/**
 * Created by Nuno Luz on 25-11-2015.
 */
public class IntentMailSenderPreferences extends MailSenderPreferences {
    public IntentMailSenderPreferences(Context context) {
        super(context);
    }

    public IntentMailSenderPreferences(Context context, SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
    }
}
