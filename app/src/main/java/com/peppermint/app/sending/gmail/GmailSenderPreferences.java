package com.peppermint.app.sending.gmail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.sending.SenderPreferences;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * Preferences for the {@link GmailSender}.
 */
public class GmailSenderPreferences extends SenderPreferences {

    // GmailSendingTask shared preference keys
    public static final String PREF_ACCOUNT_NAME_KEY = "prefAccountName";

    public GmailSenderPreferences(Context context) {
        super(context);
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
