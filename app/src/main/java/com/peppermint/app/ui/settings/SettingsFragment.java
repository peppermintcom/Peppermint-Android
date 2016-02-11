package com.peppermint.app.ui.settings;

import android.app.Activity;
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
import com.peppermint.app.authenticator.SignOutPeppermintTask;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.api.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.utils.Utils;

import javax.net.ssl.SSLException;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, SenderSupportListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();
    private static final String SCREEN_ID = "Settings";

    private static final String PREF_SIGN_OUT_KEY = "signOut";
    private static final String PREF_DISPLAY_NAME_KEY = "displayName";
    private static final String PREF_GMAIL_ENABLED_KEY = "GmailSenderPreferences_isEnabled";

    private CustomActionBarActivity mActivity;
    private SenderPreferences mPreferences;

    public SettingsFragment() {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (CustomActionBarActivity) activity;
        mPreferences = new SenderPreferences(mActivity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_global);

        String displayName = mPreferences.getFullName();
        final Preference mPrefDisplayName = findPreference(PREF_DISPLAY_NAME_KEY);
        if(displayName != null && displayName.length() >= 0) {
            mPrefDisplayName.setTitle(displayName);
        }
        mPrefDisplayName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().trim().length() > 0 && Utils.isValidName(newValue.toString().trim())) {
                    return true;
                }

                Toast.makeText(mActivity, R.string.msg_insert_name, Toast.LENGTH_LONG).show();
                return false;
            }
        });

        mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        findPreference(PREF_SIGN_OUT_KEY).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SignOutPeppermintTask task = new SignOutPeppermintTask(mActivity, SettingsFragment.this);
                task.getIdentity().setTrackerManager(mActivity.getTrackerManager());
                task.getIdentity().setPreferences(mPreferences);
                task.execute((Void) null);
                return false;
            }
        });
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PeppermintApp app = (PeppermintApp) mActivity.getApplication();

        // inflate and init custom action bar view
        TextView actionBarView = (TextView) LayoutInflater.from(mActivity).inflate(R.layout.v_settings_actionbar, null, false);
        actionBarView.setTypeface(app.getFontSemibold());
        mActivity.getCustomActionBar().setContents(actionBarView, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        TrackerManager.getInstance(getActivity().getApplicationContext()).trackScreenView(SCREEN_ID);
    }

    @Override
    public void onDestroy() {
        mPreferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        // custom dialog preferences can't listen to activity events
        // so invoke the onActivityDestroy from here (this dismisses the dialog)
        int prefCount = getPreferenceScreen().getRootAdapter().getCount();
        for(int i=0; i<prefCount; i++) {
            Object pref = getPreferenceScreen().getRootAdapter().getItem(i);
            if(pref instanceof CustomDialogPreference) {
                ((CustomDialogPreference) pref).onActivityDestroy();
            }
        }

        super.onDestroy();
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
            }
            mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSendingSupportStarted(SenderSupportTask supportTask) {

    }

    @Override
    public void onSendingSupportCancelled(SenderSupportTask supportTask) {

    }

    @Override
    public void onSendingSupportFinished(SenderSupportTask supportTask) {
        mActivity.finish();
    }

    @Override
    public void onSendingSupportError(SenderSupportTask supportTask, Throwable error) {
        if(error instanceof NoInternetConnectionException) {
            Toast.makeText(mActivity, R.string.msg_no_internet_try_again, Toast.LENGTH_LONG).show();
            return;
        }

        if(error.getCause() != null && error.getCause() instanceof SSLException) {
            Toast.makeText(mActivity, R.string.msg_secure_connection, Toast.LENGTH_LONG).show();
            return;
        }

        if(error instanceof PeppermintApiInvalidAccessTokenException) {
            Toast.makeText(mActivity, R.string.msg_invalid_credentials, Toast.LENGTH_LONG).show();
            mActivity.finish();
            return;
        }

        Toast.makeText(mActivity, R.string.msg_authentication_error, Toast.LENGTH_LONG).show();
        mActivity.getTrackerManager().logException(error);
    }

    @Override
    public void onSendingSupportProgress(SenderSupportTask supportTask, float progressValue) {

    }
}
