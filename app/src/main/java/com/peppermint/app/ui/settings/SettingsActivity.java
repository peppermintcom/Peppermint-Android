package com.peppermint.app.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.views.NavigationItem;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends CustomActionBarActivity {

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("Settings", R.drawable.ic_drawer_settings, SettingsFragment.class, false, false, 0));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeppermintApp app = (PeppermintApp) getApplication();

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.v_settings_actionbar, null, false);

        TextView txtTitle = ((TextView) v.findViewById(R.id.txtTitle));
        txtTitle.setTypeface(app.getFontSemibold());

        getCustomActionBar().setContents(v, true);
    }

}
