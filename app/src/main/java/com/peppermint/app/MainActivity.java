package com.peppermint.app;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.recipients.RecipientsFragment;
import com.peppermint.app.ui.tutorial.TutorialActivity;
import com.peppermint.app.ui.views.NavigationItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends CustomActionBarActivity {

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem(getString(R.string.menu_contacts), R.drawable.ic_drawer_contacts, RecipientsFragment.class));
        navItems.add(new NavigationItem(getString(R.string.menu_settings), R.drawable.ic_drawer_settings, RecipientsFragment.class));
        navItems.add(new NavigationItem(getString(R.string.menu_tutorial), R.drawable.ic_drawer_help, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, TutorialActivity.class);
                startActivity(intent);
            }
        }, true));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(mPreferences.isFirstRun()) {
            // shortcut intent
            final Intent shortcutIntent = new Intent(this, MainActivity.class);
            final Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher));
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            sendBroadcast(intent);

            mPreferences.setFirstRun(false);
            Intent tutorialIntent = new Intent(this, TutorialActivity.class);
            startActivity(tutorialIntent);
        }
    }

    @Override
    public void onBackPressed() {
        if(isDrawerOpen()) {
            super.onBackPressed();
            return;
        }

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
