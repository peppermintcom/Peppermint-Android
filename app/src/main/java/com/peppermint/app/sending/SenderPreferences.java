package com.peppermint.app.sending;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * Android SharedPreferences available to a Sender.
 */
public class SenderPreferences {

    public static String getEnabledPreferenceKey(Class<? extends SenderPreferences> prefClass) {
        return prefClass.getSimpleName() + "_isEnabled";
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

    public Context getContext() {
        return mContext;
    }
}
