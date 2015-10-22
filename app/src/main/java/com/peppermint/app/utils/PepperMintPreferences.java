package com.peppermint.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.peppermint.app.R;

import java.util.ArrayList;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 21-06-2015.
 * <p>
 *     Facilitator class for shared preferences.
 * </p>
 */
public class PepperMintPreferences {

    public static final String RECENT_CONTACT_URIS_KEY = "recentContactUris";

    public static final String HIDE_NONMAIN_CONTACTS_KEY = "hideNonMainContacts";
    protected static final boolean HIDE_NONMAIN_CONTACTS_DEFAULT = false;

    public static final String IS_FIRST_RUN_KEY = "isFirstRun";

    public static final String MAIL_SUBJECT_KEY = "mailSubject";
    public static final String MAIL_BODY_KEY = "mailBody";
    public static final String DISPLAY_NAME_KEY = "displayName";

    protected static final int RECENT_CONTACTS_LIST_LIMIT = 50;

    protected SharedPreferences mSettings;
    protected SharedPreferences.Editor mEditor;
    protected Context mContext;

    public PepperMintPreferences(Context context) {
        this.mContext = context;
        this.mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public SharedPreferences getSharedPreferences() {
        return mSettings;
    }

    /**
     * Starts the edition process of the shared preferences.<br />
     * <strong>This must be invoked before setting any preference.</strong>
     */
    protected void edit() {
        if (mEditor == null) {
            mEditor = mSettings.edit();
        }
    }

    /**
     * Commits any changes to the shared preferences.<br />
     * <strong>This must be invoked for any changes to be saved.</strong>
     */
    protected void commit() {
        if (mEditor != null) {
            mEditor.commit();
            mEditor = null;
        }
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

    public void setRecentContactUris(ArrayList<Long> recentContactIds) {
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

        edit();
        mEditor.putString(RECENT_CONTACT_URIS_KEY, contactsStr);
        commit();
    }

    public ArrayList<Long> getRecentContactUris() {
        ArrayList<Long> recentContactUris = null;

        String contactsStr = mSettings.getString(RECENT_CONTACT_URIS_KEY, null);
        if(contactsStr != null) {
                String[] ids = contactsStr.split(",");
                recentContactUris = new ArrayList<>();
                for(String strId : ids) {
                    recentContactUris.add(Long.parseLong(strId));
                }
        }

        return recentContactUris;
    }

    public void setHideNonMainContacts(boolean hide) {
        edit();
        mEditor.putBoolean(HIDE_NONMAIN_CONTACTS_KEY, hide);
        commit();
    }

    public boolean isHideNonMainContacts() {
        return mSettings.getBoolean(HIDE_NONMAIN_CONTACTS_KEY, HIDE_NONMAIN_CONTACTS_DEFAULT);
    }

    public void setMailSubject(String subject) {
        edit();
        mEditor.putString(MAIL_SUBJECT_KEY, subject);
        commit();
    }

    public String getMailSubject() {
        return mSettings.getString(MAIL_SUBJECT_KEY, mContext.getString(R.string.default_mail_subject));
    }

    public void setMailBody(String body) {
        edit();
        mEditor.putString(MAIL_BODY_KEY, body);
        commit();
    }

    public String getMailBody() {
        return mSettings.getString(MAIL_BODY_KEY, "");
    }

    public void setDisplayName(String name) {
        edit();
        mEditor.putString(DISPLAY_NAME_KEY, name);
        commit();
    }

    public String getDisplayName() {
        String name = mSettings.getString(DISPLAY_NAME_KEY, null);
        if(name == null) {
            String[] data = Utils.getUserData(mContext);
            if(data[0] != null) {
                name = data[0];
                setDisplayName(data[0]);
            } else {
                name = mContext.getString(R.string.peppermint_user);
                //setDisplayName(name);
            }
        }
        return name;
    }

    public boolean isFirstRun() {
        return mSettings.getBoolean(IS_FIRST_RUN_KEY, true);
    }

    public void setFirstRun(boolean val) {
        edit();
        mEditor.putBoolean(IS_FIRST_RUN_KEY, val);
        commit();
    }
}
