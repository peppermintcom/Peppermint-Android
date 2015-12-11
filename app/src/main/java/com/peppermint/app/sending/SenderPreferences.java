package com.peppermint.app.sending;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Nuno Luz on 02-10-2015.
 * <p>
 * Android SharedPreferences available to a Sender.
 * </p>
 * All have the isEnabled and isAuthorized preferences.<br />
 * The isEnabled allows you to completely disable the sender (it remains in the workflow, though).<br />
 * The isAuthorized can be used by senders that require external API authorization, such as the Gmail API.
 */
public class SenderPreferences {

    public static String getEnabledPreferenceKey(Class<? extends SenderPreferences> prefClass) {
        return prefClass.getSimpleName() + "_isEnabled";
    }

    public static String getAuthorizedPreferenceKey(Class<? extends  SenderPreferences> prefClass) {
        return prefClass.getSimpleName() + "_isAuthorized";
    }

    private SharedPreferences mSharedPreferences;
    private Context mContext;

    public SenderPreferences(Context context) {
        this.mContext = context;
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public SenderPreferences(Context context, SharedPreferences sharedPreferences) {
        this.mContext = context;
        this.mSharedPreferences = sharedPreferences;
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    public void setEnabled(boolean val) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(getEnabledPreferenceKey(this.getClass()), val);
        editor.commit();
    }

    public boolean isEnabled() {
        return getSharedPreferences().getBoolean(getEnabledPreferenceKey(this.getClass()), true);
    }

    public void setAuthorized(boolean val) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(getAuthorizedPreferenceKey(this.getClass()), val);
        editor.commit();
    }

    public boolean isAuthorized() {
        return getSharedPreferences().getBoolean(getAuthorizedPreferenceKey(this.getClass()), true);
    }

    public Context getContext() {
        return mContext;
    }
}
