package com.peppermint.app.sending;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * Preferences for Google Account.
 */
public class GoogleSenderPreferences extends SenderPreferences {

    // GmailSendingTask shared preference keys
    public static final String PREF_ACCOUNT_NAME_KEY = "prefAccountName";

    public GoogleSenderPreferences(Context context) {
        super(context);
    }

    public GoogleSenderPreferences(Context context, SharedPreferences sharedPreferences) {
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
}
