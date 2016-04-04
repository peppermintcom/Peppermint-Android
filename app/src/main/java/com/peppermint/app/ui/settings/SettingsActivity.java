package com.peppermint.app.ui.settings;

import android.os.Bundle;

import com.peppermint.app.R;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.activities.CustomActionBarActivity;

public class SettingsActivity extends CustomActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CustomActionBarView actionBarView = getCustomActionBar();
        if(actionBarView != null) {
            actionBarView.setTitle(getString(R.string.drawer_menu_settings));
        }

        if (savedInstanceState != null) {
            return; // avoids duplicate fragments
        }

        getFragmentManager().beginTransaction().add(R.id.container, new SettingsFragment()).commit();
    }

}
