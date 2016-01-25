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

    // GmailSenderTask shared preference keys
    public static final String ACCOUNT_NAME_KEY = "prefAccountName";

    public MailSenderPreferences(Context context) {
        super(context);
    }

    public MailSenderPreferences(Context context, SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
    }

    public void setPreferredAccountName(String accountName) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(ACCOUNT_NAME_KEY, accountName);
        editor.commit();
    }

    public String getPreferredAccountName() {
        return getSharedPreferences().getString(ACCOUNT_NAME_KEY, null);
    }

    @Override
    public String getFullName() {
        String fullName = super.getFullName();
        if(fullName.length() <= 0) {
            return null;
        }
        return fullName;
    }
}
