package com.peppermint.app.cloud.senders;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

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

    public static final String CHAT_TIP_SHOWN_KEY = "chatTipShown";

    public static final String RECENT_CONTACT_URIS_KEY = "recentContactUris";
    public static final String SHOWN_SMS_CONFIRMATION_KEY = "shownSmsConfirmation";

    public static final String IS_FIRST_RUN_KEY = "isFirstRun";
    public static final String HAS_SENT_KEY = "hasSentMessage";

    public static final String FIRST_NAME_KEY = "firstName";
    public static final String LAST_NAME_KEY = "lastName";

    protected static final int RECENT_CONTACTS_LIST_LIMIT = 50;

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

    public void addRecentContactUri(long id) {
        ArrayList<Long> idList = getRecentContactUris();

        if(idList == null) {
            idList = new ArrayList<>();
            idList.add(id);
        } else {
            if(idList.contains(id)) {
                idList.remove(id);
            }
            idList.add(0, id);
        }

        while(idList.size() > RECENT_CONTACTS_LIST_LIMIT) {
            idList.remove(idList.size()-1);
        }

        setRecentContactUris(idList);
    }

    public void setRecentContactUris(List<Long> recentContactIds) {
        String contactsStr = null;
        int size;
        if(recentContactIds != null && (size = recentContactIds.size()) > 0) {
            StringBuilder builder = new StringBuilder();
            for(int i=0; i<size; i++) {
                if(i>0) {
                    builder.append(",");
                }
                builder.append(recentContactIds.get(i));
            }
            contactsStr = builder.toString();
        }

        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(RECENT_CONTACT_URIS_KEY, contactsStr);
        editor.commit();
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

    public void setShownSmsConfirmation(boolean shown) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(SHOWN_SMS_CONFIRMATION_KEY, shown);
        editor.commit();
    }

    public boolean isShownSmsConfirmation() {
        return getSharedPreferences().getBoolean(SHOWN_SMS_CONFIRMATION_KEY, false);
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

    public boolean isChatTipShown() {
        return getSharedPreferences().getBoolean(CHAT_TIP_SHOWN_KEY, false);
    }

    public void setChatTipShown(boolean val) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(CHAT_TIP_SHOWN_KEY, val);
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

    public void clearAll() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.clear();
        editor.commit();
    }

    public Context getContext() {
        return mContext;
    }
}
