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

    // GmailSenderTask shared preference keys
    public static final String ACCOUNT_NAME_KEY = "prefAccountName";
    public static final String FIRST_NAME_KEY = "firstName";
    public static final String LAST_NAME_KEY = "lastName";

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

    public void setFirstName(String name) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(FIRST_NAME_KEY, name);
        editor.commit();
    }

    public String getFirstName() {
        return getSharedPreferences().getString(FIRST_NAME_KEY, null);
    }

    public void setLastName(String name) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(LAST_NAME_KEY, name);
        editor.commit();
    }

    public String getLastName() {
        return getSharedPreferences().getString(LAST_NAME_KEY, null);
    }

    public void setFullName(String name) {
        String[] names = Utils.getFirstAndLastNames(name);
        setFirstName(names[0]);
        if(names[1] != null) {
            setLastName(names[1]);
        }
    }

    public String getFullName() {
        String firstName = getFirstName();
        String lastName = getLastName();
        String name = null;

        if(firstName == null && lastName == null) {
            String[] data = Utils.getUserData(getContext());
            if(data[0] != null && Utils.isValidName(data[0])) {
                name = Utils.capitalizeFully(data[0]);
                setFullName(data[0]);
            }
        } else {
            name = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        }

        return name;
    }

}
