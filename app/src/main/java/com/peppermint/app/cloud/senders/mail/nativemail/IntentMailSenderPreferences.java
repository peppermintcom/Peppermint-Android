package com.peppermint.app.cloud.senders.mail.nativemail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.cloud.senders.SenderPreferences;

/**
 * Created by Nuno Luz on 25-11-2015.
 *
 * Sender preferences for the IntentMailSender.
 */
public class IntentMailSenderPreferences extends SenderPreferences {
    public IntentMailSenderPreferences(Context context) {
        super(context);
    }

    public IntentMailSenderPreferences(Context context, SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
    }
}
