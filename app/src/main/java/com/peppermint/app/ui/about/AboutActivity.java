package com.peppermint.app.ui.about;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.views.NavigationItem;

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
        navItems.add(new NavigationItem("About", R.drawable.ic_settings_36dp, AboutFragment.class, false, false));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeppermintApp app = (PeppermintApp) getApplication();

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.v_about_actionbar, null, false);

        TextView txtTitle = ((TextView) v.findViewById(R.id.txtTitle));
        txtTitle.setTypeface(app.getFontSemibold());

        getCustomActionBar().setContents(v, true);
    }

}
