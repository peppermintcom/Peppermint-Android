package com.peppermint.app.sending.mail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * Preferences for Google Account.
 */
public class MailSenderPreferences extends SenderPreferences {

    // GmailSendingTask shared preference keys
    public static final String PREF_ACCOUNT_NAME_KEY = "prefAccountName";
    public static final String DISPLAY_NAME_KEY = "displayName";

    public MailSenderPreferences(Context context) {
        super(context);
    }

    public MailSenderPreferences(Context context, SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
    }

    public void setPreferredAccountName(String accountName) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(PREF_ACCOUNT_NAME_KEY, accountName);
        editor.commit();
    }

    public String getPreferredAccountName() {
        return getSharedPreferences().getString(PREF_ACCOUNT_NAME_KEY, null);
    }

    public void setDisplayName(String name) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(DISPLAY_NAME_KEY, name);
        editor.commit();
    }

    public String getDisplayName() {
        String name = getSharedPreferences().getString(DISPLAY_NAME_KEY, null);
        if(name == null) {
            String[] data = Utils.getUserData(getContext());
            if(data[0] != null) {
                name = data[0];
                setDisplayName(data[0]);
            }
        }
        return name;
    }
}
