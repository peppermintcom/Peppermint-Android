package com.peppermint.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.sending.mail.MailSenderPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 21-06-2015.
 * <p>
 *     Facilitator class for shared preferences.
 * </p>
 */
public class PepperMintPreferences {

    public static final String RECENT_CONTACT_URIS_KEY = "recentContactUris";
    public static final String SHOWN_SMS_CONFIRMATION_KEY = "shownSmsConfirmation";

    public static final String IS_FIRST_RUN_KEY = "isFirstRun";
    public static final String HAS_SENT_KEY = "hasSentMessage";

    public static final String MAIL_SUBJECT_KEY = "mailSubject";
    public static final String FIRST_NAME_KEY = "firstName";
    public static final String LAST_NAME_KEY = "lastName";

    protected static final int RECENT_CONTACTS_LIST_LIMIT = 50;

    protected SharedPreferences mSettings;
    protected SharedPreferences.Editor mEditor;
    protected Context mContext;
    protected MailSenderPreferences mGmailPreferences;

    public PepperMintPreferences(Context context) {
        this.mContext = context;
        this.mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);

        mGmailPreferences = new MailSenderPreferences(mContext, mSettings);
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
        Log.d("PepperMintPreferences", "addRecentContactUri: " + id);
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

    public void setShownSmsConfirmation(boolean shown) {
        edit();
        mEditor.putBoolean(SHOWN_SMS_CONFIRMATION_KEY, shown);
        commit();
    }

    public boolean isShownSmsConfirmation() {
        return mSettings.getBoolean(SHOWN_SMS_CONFIRMATION_KEY, false);
    }

    public void setMailSubject(String subject) {
        edit();
        mEditor.putString(MAIL_SUBJECT_KEY, subject);
        commit();
    }

    public String getMailSubject() {
        return mSettings.getString(MAIL_SUBJECT_KEY, mContext.getString(R.string.sender_default_mail_subject));
    }

    public void setFirstName(String name) {
        edit();
        mEditor.putString(FIRST_NAME_KEY, name);
        commit();
    }

    public String getFirstName() {
        return mSettings.getString(FIRST_NAME_KEY, null);
    }

    public void setLastName(String name) {
        edit();
        mEditor.putString(LAST_NAME_KEY, name);
        commit();
    }

    public String getLastName() {
        return mSettings.getString(LAST_NAME_KEY, null);
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
        String name = "";

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
        return mSettings.getBoolean(IS_FIRST_RUN_KEY, true);
    }

    public void setFirstRun(boolean val) {
        edit();
        mEditor.putBoolean(IS_FIRST_RUN_KEY, val);
        commit();
    }

    public boolean hasSentMessage() {
        return mSettings.getBoolean(HAS_SENT_KEY, false);
    }

    public void setHasSentMessage(boolean val) {
        edit();
        mEditor.putBoolean(HAS_SENT_KEY, val);
        commit();
    }

    public MailSenderPreferences getGmailPreferences() {
        return mGmailPreferences;
    }
}
