package com.peppermint.app.sending;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SharedPreferences available to a Sender.
 */
public class SenderPreferences {

    private SharedPreferences mSharedPreferences;
    private Context mContext;

    public SenderPreferences(Context context) {
        this.mContext = context;
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

}
