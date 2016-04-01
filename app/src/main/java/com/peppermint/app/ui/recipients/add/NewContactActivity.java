package com.peppermint.app.ui.recipients.add;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.views.NavigationItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * Activity for user authentication.
 */
public class NewContactActivity extends CustomActionBarActivity {

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("New Contact", R.drawable.ic_settings_36dp, NewContactFragment.class, false, false, 0));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.v_newcontact_actionbar, null, false);
        getCustomActionBar().setContents(v, false);

        // cancel new contact icon
        getCustomActionBar().getMenuButton().setImageResource(R.drawable.ic_cancel_14dp);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}
