package com.peppermint.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
    public static final boolean HIDE_NONMAIN_CONTACTS_DEFAULT = false;

    protected SharedPreferences mSettings = null;
    protected SharedPreferences.Editor mEditor = null;
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

        while(idList.size() > 20) {
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

}
