package com.peppermint.app;

import android.app.Fragment;
import android.widget.Toast;

import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.recipients.RecipientsFragment;
import com.peppermint.app.ui.views.NavigationItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends CustomActionBarActivity {

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("Contacts", R.drawable.ic_drawer_contacts, RecipientsFragment.class));
        navItems.add(new NavigationItem("Settings", R.drawable.ic_drawer_settings, RecipientsFragment.class));
        navItems.add(new NavigationItem("Help & Feedback", R.drawable.ic_drawer_help, RecipientsFragment.class, true));
        return navItems;
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getCurrentFragment();

        if(fragment instanceof RecipientsFragment) {
            int stepsLeft = ((RecipientsFragment) fragment).clearFilters();
            if (stepsLeft <= 0) {
                super.onBackPressed();
                return;
            }

            if (stepsLeft <= 1) {
                Toast.makeText(MainActivity.this, R.string.msg_press_back_again_exit, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        super.onBackPressed();
    }
}
