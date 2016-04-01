package com.peppermint.app.ui.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.authenticator.SignOutPeppermintTask;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.cloud.senders.SenderSupportListener;
import com.peppermint.app.cloud.senders.SenderSupportTask;
import com.peppermint.app.cloud.senders.exceptions.NoInternetConnectionException;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.chat.head.ChatHeadServiceManager;
import com.peppermint.app.utils.Utils;

import javax.net.ssl.SSLException;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, SenderSupportListener {

    private static final String SCREEN_ID = "Settings";

    private static final String PREF_SIGN_OUT_KEY = "signOut";
    private static final String PREF_DISPLAY_NAME_KEY = "displayName";
    private static final String PREF_CHAT_HEADS_ENABLED_KEY = "chatHeads";

    private static final String PREF_GMAIL_ENABLED_KEY = "GmailSenderPreferences_isEnabled";

    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 122;

    private CustomActionBarActivity mActivity;
    private SenderPreferences mPreferences;

    private ProgressDialog mProgressDialog;
    private SignOutPeppermintTask mSignOutTask;

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
                return false; // does not allow the preference to change
            }
        });

        final CheckBoxPreference mPrefChatHeads = (CheckBoxPreference) findPreference(PREF_CHAT_HEADS_ENABLED_KEY);
        mPrefChatHeads.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(mActivity)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + mActivity.getPackageName()));
                        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
                        return false;   // does not allow the preference to change
                    }
                }
                return true;
            }
        });

        mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        findPreference(PREF_SIGN_OUT_KEY).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showProgress();
                mSignOutTask = new SignOutPeppermintTask(mActivity, SettingsFragment.this);
                mSignOutTask.getIdentity().setTrackerManager(mActivity.getTrackerManager());
                mSignOutTask.getIdentity().setPreferences(mPreferences);
                mSignOutTask.execute((Void) null);
                return false;
            }
        });

        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setMessage(getText(R.string.signing_out_));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (mSignOutTask != null) {
                    mSignOutTask.cancel(true);
                    mSignOutTask = null;
                }
            }
        });
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // inflate and init custom action bar view
        TextView actionBarView = (TextView) LayoutInflater.from(mActivity).inflate(R.layout.v_settings_actionbar, null, false);
        mActivity.getCustomActionBar().setContents(actionBarView, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        TrackerManager.getInstance(getActivity().getApplicationContext()).trackScreenView(SCREEN_ID);

        // in case the user remove the permission outside the app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(mActivity)) {
                mPreferences.setChatHeadsEnabled(false);
            }
        }
    }

    @Override
    public void onDestroy() {
        hideProgress();

        if (mSignOutTask != null) {
            mSignOutTask.cancel(true);
            mSignOutTask = null;
        }

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

        mActivity = null;

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
            } else if(key.compareTo(PREF_CHAT_HEADS_ENABLED_KEY) == 0) {
                boolean isEnabled = mPreferences.areChatHeadsEnabled();
                CheckBoxPreference checkPref = (CheckBoxPreference) pref;
                checkPref.setChecked(isEnabled);
                if(isEnabled) {
                    ChatHeadServiceManager.startAndEnable(mActivity);
                } else {
                    ChatHeadServiceManager.startAndDisable(mActivity);
                }
            }
            mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check the result of the overlay permission request
        if(requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(mActivity)) {
                    mPreferences.setChatHeadsEnabled(true);
                }
            }
        }
    }

    @Override
    public void onSendingSupportStarted(SenderSupportTask supportTask) { /* nothing to do here */  }

    @Override
    public void onSendingSupportProgress(SenderSupportTask supportTask, float progressValue) { /* nothing to do here */ }

    @Override
    public void onSendingSupportCancelled(SenderSupportTask supportTask) {
        hideProgress();
    }

    @Override
    public void onSendingSupportFinished(SenderSupportTask supportTask) {
        hideProgress();
        mActivity.finish();
    }

    @Override
    public void onSendingSupportError(SenderSupportTask supportTask, Throwable error) {
        hideProgress();

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

    private void showProgress() {
        if(mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    private void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}
