package com.peppermint.app.ui.settings;

import android.app.Activity;
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
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiNoAccountException;
import com.peppermint.app.cloud.apis.speech.GoogleSpeechRecognizeClient;
import com.peppermint.app.services.authenticator.AuthenticatorUtils;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.activities.CustomActionBarActivity;
import com.peppermint.app.ui.chat.head.ChatHeadServiceManager;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String SCREEN_ID = "Settings";

    private static final String PREF_SIGN_OUT_KEY = "signOut";
    private static final String PREF_DISPLAY_NAME_KEY = "displayName";
    private static final String PREF_CHAT_HEADS_ENABLED_KEY = "chatHeads";
    private static final String PREF_TRANSCRIPTION_LANGUAGE_CODE = "transcriptionLanguageCode";

    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 122;

    private CustomActionBarActivity mActivity;
    private SenderPreferences mPreferences;
    private List<CustomListViewPreference.ListItem> mListItems;

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

        final String displayName = mPreferences.getFullName();

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

        findPreference(PREF_SIGN_OUT_KEY).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AuthenticatorUtils authenticatorUtils = new AuthenticatorUtils(mActivity);
                try {
                    authenticatorUtils.signOut();
                } catch (PeppermintApiNoAccountException e) {
                    /* just logout normally */
                }
                mActivity.finish();
                return false;
            }
        });

        // TRANSCRIPTION PREFS
        final String selectedLanguageCode = mPreferences.getTranscriptionLanguageCode();
        String selectedLanguage = getString(R.string.pref_default_transcription_language_code);
        mListItems = new ArrayList<>();
        mListItems.add(new CustomListViewPreference.ListItem(null, getString(R.string.pref_default_transcription_language_code)));
        final String packageName = mActivity.getPackageName();
        for(int i=0; i< GoogleSpeechRecognizeClient.SUPPORTED_LANGUAGE_CODES.length; i++) {
            final String key = GoogleSpeechRecognizeClient.SUPPORTED_LANGUAGE_CODES[i];
            final int resId = getResources().getIdentifier("lang_" + key.replace("-", "_"), "string", packageName);
            CustomListViewPreference.ListItem listItem = new CustomListViewPreference.ListItem(key, getString(resId));
            mListItems.add(listItem);
            if(selectedLanguageCode != null && selectedLanguageCode.equals(key)) {
                selectedLanguage = listItem.value;
            }
        }

        final CustomListViewPreference prefTranscriptionLanguageCode = (CustomListViewPreference) findPreference(PREF_TRANSCRIPTION_LANGUAGE_CODE);
        prefTranscriptionLanguageCode.setItems(mListItems);
        prefTranscriptionLanguageCode.setTitle(selectedLanguage);

        mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // inflate and init custom action bar view
        final TextView actionBarView = (TextView) LayoutInflater.from(mActivity).inflate(R.layout.v_settings_actionbar, null, false);
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
        mPreferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        // custom dialog preferences can't listen to activity events
        // so invoke the onActivityDestroy from here (this dismisses the dialog)
        final int prefCount = getPreferenceScreen().getRootAdapter().getCount();
        for(int i=0; i<prefCount; i++) {
            final Object pref = getPreferenceScreen().getRootAdapter().getItem(i);
            if(pref instanceof CustomDialogPreference) {
                ((CustomDialogPreference) pref).onActivityDestroy();
            }
        }

        mActivity = null;

        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Preference pref = findPreference(key);
        if (pref != null) {
            mPreferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            if(key.compareTo(PREF_DISPLAY_NAME_KEY) == 0) {
                pref.setTitle(sharedPreferences.getString(PREF_DISPLAY_NAME_KEY, getString(R.string.pref_title_displayname)));
            } else if(key.compareTo(PREF_CHAT_HEADS_ENABLED_KEY) == 0) {
                final boolean isEnabled = mPreferences.areChatHeadsEnabled();
                CheckBoxPreference checkPref = (CheckBoxPreference) pref;
                checkPref.setChecked(isEnabled);
                if(isEnabled) {
                    ChatHeadServiceManager.startAndEnable(mActivity);
                } else {
                    ChatHeadServiceManager.startAndDisable(mActivity);
                }
            } else if(key.compareTo(PREF_TRANSCRIPTION_LANGUAGE_CODE) == 0) {
                final String selectedKey = mPreferences.getTranscriptionLanguageCode();
                if(selectedKey == null) {
                    pref.setTitle(getString(R.string.pref_default_transcription_language_code));
                } else {
                    boolean done = false;
                    final int listItemAmount = mListItems.size();
                    for(int i=0; i< listItemAmount && !done; i++) {
                        final CustomListViewPreference.ListItem listItem = mListItems.get(i);
                        if(selectedKey.equals(listItem.key)) {
                            pref.setTitle(listItem.value);
                            done = true;
                        }
                    }
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

}
