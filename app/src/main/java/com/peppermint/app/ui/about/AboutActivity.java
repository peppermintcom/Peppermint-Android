package com.peppermint.app.ui.about;

import android.os.Bundle;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.base.NavigationItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 06-01-2016.
 *
 * About Activity.
 */
public class AboutActivity extends CustomActionBarActivity {

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("About", R.drawable.ic_settings_36dp, AboutFragment.class, false, false, 0, null));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.v_about_actionbar, null, false);
        getCustomActionBar().setContents(v, true);
    }

}
