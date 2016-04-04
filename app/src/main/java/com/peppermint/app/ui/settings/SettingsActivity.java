package com.peppermint.app.ui.settings;

import android.os.Bundle;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.base.NavigationItem;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends CustomActionBarActivity {

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("Settings", R.drawable.ic_drawer_settings, SettingsFragment.class, false, false, 0, null));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.v_settings_actionbar, null, false);
        getCustomActionBar().setContents(v, true);
    }

}
