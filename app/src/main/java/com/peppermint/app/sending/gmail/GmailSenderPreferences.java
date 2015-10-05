package com.peppermint.app.sending.gmail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.sending.SenderPreferences;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * GmailSender SharedPreferences.
 */
public class GmailSenderPreferences extends SenderPreferences {

    // GmailSendingTask Shared Preference Keys
    public static final String PREF_ACCOUNT_NAME_KEY = "prefAccountName";
    public static final String PREF_SKIP_IF_PERMISSION_REQ_KEY = "prefSkipIfPermissionRequired";

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

    public void setSkipIfPermissionRequired(boolean doSkip) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(PREF_SKIP_IF_PERMISSION_REQ_KEY, doSkip);
        editor.commit();
    }

    public boolean getSkipIfPermissionRequired() {
        return getSharedPreferences().getBoolean(PREF_SKIP_IF_PERMISSION_REQ_KEY, false);
    }
}
