package com.peppermint.app.sending.gmail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.sending.GoogleSenderPreferences;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * Preferences for the {@link GmailSender}.
 */
public class GmailSenderPreferences extends GoogleSenderPreferences {

    public static final String DISPLAY_NAME_KEY = "displayName";

    public GmailSenderPreferences(Context context) {
        super(context);
    }

    public GmailSenderPreferences(Context context, SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
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
