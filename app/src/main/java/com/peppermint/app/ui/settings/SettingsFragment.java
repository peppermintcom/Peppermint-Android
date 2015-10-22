package com.peppermint.app.ui.settings;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.sending.gmail.GmailSender;
import com.peppermint.app.sending.gmail.GmailSenderPreferences;
import com.peppermint.app.ui.CustomActionBarActivity;

public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private static final int PREF_GMAIL_ACCOUNT_REQUEST = 1199;

    private Preference mPrefGmailAccount;

    public SettingsFragment() {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_global);

        mPrefGmailAccount = findPreference(GmailSenderPreferences.PREF_ACCOUNT_NAME_KEY);
        mPrefGmailAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GmailSender sender = new GmailSender(getActivity(), null);
                sender.init();
                startActivityForResult(sender.getCredential().newChooseAccountIntent(), PREF_GMAIL_ACCOUNT_REQUEST);
                sender.deinit();
                return true;
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PeppermintApp app = (PeppermintApp) getActivity().getApplication();

        // inflate and init custom action bar view
        TextView actionBarView = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.v_settings_actionbar, null, false);
        actionBarView.setTypeface(app.getFontSemibold());
        ((CustomActionBarActivity) getActivity()).getCustomActionBar().setContents(actionBarView, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PREF_GMAIL_ACCOUNT_REQUEST) {
            // the user has picked one of the multiple available google accounts to use...
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    GmailSenderPreferences prefs = new GmailSenderPreferences(getActivity());
                    prefs.setPreferredAccountName(accountName);
                }
            }
        }
    }
}
