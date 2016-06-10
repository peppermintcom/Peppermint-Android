package com.peppermint.app.services.messenger.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Nuno Luz on 02-10-2015.
 * <p>
 *      Android SharedPreferences available to a Sender. It's also the main app preferences manager.
 * </p>
 * <p>
 *      All SenderPreferences have the <i>isEnabled</i> preference.<br />
 *      The <i>isEnabled</i> allows you to completely disable the sender (it remains in the workflow, though).
 * </p>
 */
public class SenderPreferences {

    public static final String RECENT_CONTACT_URIS_KEY = "recentContactUris";

    public static final String IS_FIRST_RUN_KEY = "isFirstRun";
    public static final String HAS_SENT_KEY = "hasSentMessage";

    public static final String FIRST_NAME_KEY = "firstName";
    public static final String LAST_NAME_KEY = "lastName";

    public static final String ARE_CHATHEADS_ENABLED_KEY = "chatHeads";

    public static final String AUTOMATIC_TRANSCRIPTION = "automaticTranscription";
    public static final String TRANSCRIPTION_LANGUAGE_CODE = "transcriptionLanguageCode";

    private SharedPreferences mSharedPreferences;
    private Context mContext;

    public SenderPreferences(final Context context) {
        this.mContext = context;
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public SenderPreferences(final Context context, final SharedPreferences sharedPreferences) {
        this.mContext = context;
        this.mSharedPreferences = sharedPreferences;
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    public ArrayList<Long> getRecentContactUris() {
        ArrayList<Long> recentContactUris = null;

        String contactsStr = getSharedPreferences().getString(RECENT_CONTACT_URIS_KEY, null);
        if(contactsStr != null) {
            String[] ids = contactsStr.split(",");
            recentContactUris = new ArrayList<>();
            for(String strId : ids) {
                recentContactUris.add(Long.parseLong(strId));
            }
        }

        return recentContactUris;
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
            String[] data = Utils.getUserData(mContext);
            if(data[0] != null && Utils.isValidName(data[0])) {
                name = Utils.capitalizeFully(data[0]);
                setFullName(data[0]);
            }
        } else {
            name = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        }

        return name;
    }

    public boolean isFirstRun() {
        return getSharedPreferences().getBoolean(IS_FIRST_RUN_KEY, true);
    }

    public void setFirstRun(boolean val) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(IS_FIRST_RUN_KEY, val);
        editor.commit();
    }

    public boolean hasSentMessage() {
        return getSharedPreferences().getBoolean(HAS_SENT_KEY, false);
    }

    public void setHasSentMessage(boolean val) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(HAS_SENT_KEY, val);
        editor.commit();
    }

    public boolean areChatHeadsEnabled() {
        return getSharedPreferences().getBoolean(ARE_CHATHEADS_ENABLED_KEY, false);
    }

    public void setChatHeadsEnabled(boolean val) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(ARE_CHATHEADS_ENABLED_KEY, val);
        editor.commit();
    }

    public void setTranscriptionLanguageCode(String languageCode) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(TRANSCRIPTION_LANGUAGE_CODE, languageCode);
        editor.commit();
    }

    public String getTranscriptionLanguageCode() {
        return getSharedPreferences().getString(TRANSCRIPTION_LANGUAGE_CODE, Utils.toBcp47LanguageTag(Locale.getDefault()));
    }

    public boolean isAutomaticTranscription() {
        return getSharedPreferences().getBoolean(AUTOMATIC_TRANSCRIPTION, true);
    }

    public void setAutomaticTranscription(boolean val) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(AUTOMATIC_TRANSCRIPTION, val);
        editor.commit();
    }

    public void clearAll() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.clear();
        editor.commit();
    }

    public Context getContext() {
        return mContext;
    }
}
