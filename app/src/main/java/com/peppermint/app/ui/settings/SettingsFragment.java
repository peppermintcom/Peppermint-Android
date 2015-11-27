package com.peppermint.app.ui.settings;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.sending.mail.MailSenderPreferences;
import com.peppermint.app.sending.mail.gmail.GmailSender;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.utils.PepperMintPreferences;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private static final String PREF_DISPLAY_NAME_KEY = "displayName";
    private static final String PREF_SUBJECT_KEY = "mailSubject";
    private static final String PREF_GMAIL_ENABLED_KEY = "GmailSenderPreferences_isEnabled";
    private static final String PREF_GMAIL_ACCOUNT = "prefAccountName";

    private static final int PREF_GMAIL_ACCOUNT_REQUEST = 1199;

    private CustomPreference mPrefGmailAccount;
    private Activity mActivity;
    private PepperMintPreferences mPreferences;

    public SettingsFragment() {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        mPreferences = new PepperMintPreferences(mActivity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_global);

        String preferredAccountName = mPreferences.getGmailPreferences().getPreferredAccountName();
        mPrefGmailAccount = (CustomPreference) findPreference(MailSenderPreferences.PREF_ACCOUNT_NAME_KEY);
        mPrefGmailAccount.setContent(preferredAccountName);
        mPrefGmailAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GmailSender sender = new GmailSender(mActivity, null);
                sender.init();
                startActivityForResult(sender.getCredential().newChooseAccountIntent(), PREF_GMAIL_ACCOUNT_REQUEST);
                sender.deinit();
                return true;
            }
        });

        String displayName = mPreferences.getDisplayName();
        final Preference mPrefDisplayName = findPreference(PREF_DISPLAY_NAME_KEY);
        if(displayName != null && displayName.length() >= 0) {
            mPrefDisplayName.setTitle(displayName);
        }
        mPrefDisplayName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().trim().length() > 0) {
                    return true;
                }

                Toast.makeText(mActivity, R.string.msg_insert_name, Toast.LENGTH_LONG).show();
                return false;
            }
        });

        String mailSubject = mPreferences.getMailSubject();
        final CustomEditTextPreference prefMailSubject = (CustomEditTextPreference) findPreference(PREF_SUBJECT_KEY);
        prefMailSubject.setContent(mailSubject);

        mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PeppermintApp app = (PeppermintApp) mActivity.getApplication();

        // inflate and init custom action bar view
        TextView actionBarView = (TextView) LayoutInflater.from(mActivity).inflate(R.layout.v_settings_actionbar, null, false);
        actionBarView.setTypeface(app.getFontSemibold());
        ((CustomActionBarActivity) mActivity).getCustomActionBar().setContents(actionBarView, false);
    }

    @Override
    public void onDestroy() {
        mPreferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PREF_GMAIL_ACCOUNT_REQUEST) {
            // the user has picked one of the multiple available google accounts to use...
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    MailSenderPreferences prefs = new MailSenderPreferences(mActivity);
                    prefs.setPreferredAccountName(accountName);

                    SenderServiceManager senderManager = new SenderServiceManager(mActivity.getApplicationContext());
                    senderManager.startAndAuthorize();
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            mPreferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

            if (key.compareTo(PREF_GMAIL_ENABLED_KEY) == 0) {
                CheckBoxPreference checkPref = (CheckBoxPreference) pref;
                checkPref.setChecked(sharedPreferences.getBoolean(PREF_GMAIL_ENABLED_KEY, true));
            } else if(key.compareTo(PREF_DISPLAY_NAME_KEY) == 0) {
                pref.setTitle(sharedPreferences.getString(PREF_DISPLAY_NAME_KEY, getString(R.string.pref_title_displayname)));
            } else if(key.compareTo(PREF_SUBJECT_KEY) == 0) {
                ((CustomEditTextPreference) pref).setContent(sharedPreferences.getString(PREF_SUBJECT_KEY, null));
            } else if(key.compareTo(PREF_GMAIL_ACCOUNT) == 0) {
                ((CustomPreference) pref).setContent(sharedPreferences.getString(PREF_GMAIL_ACCOUNT, null));
            }
            mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
    }
}
