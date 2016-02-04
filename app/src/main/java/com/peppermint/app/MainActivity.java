package com.peppermint.app;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.PermissionsPolicyEnforcer;
import com.peppermint.app.ui.about.AboutActivity;
import com.peppermint.app.ui.chat.ChatListActivity;
import com.peppermint.app.ui.recipients.RecipientsFragment;
import com.peppermint.app.ui.settings.SettingsActivity;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends CustomActionBarActivity {

    private static final String SUPPORT_EMAIL = "support@peppermint.com";
    private static final String SUPPORT_SUBJECT = "Feedback or question about Peppermint Android app";
    private static final String SUPPORT_BODY = "\n\n\n\nNote regarding this feedback. Was provided by %1$s running %2$s with Peppermint v" + BuildConfig.VERSION_NAME;

    private PermissionsPolicyEnforcer mPermissionsManager = new PermissionsPolicyEnforcer(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            "android.permission.READ_PROFILE",
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,

            Manifest.permission.GET_ACCOUNTS,
            "android.permission.USE_CREDENTIALS");

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem(getString(R.string.drawer_menu_contacts), R.drawable.ic_drawer_contacts, RecipientsFragment.class, false, false));
        navItems.add(new NavigationItem(getString(R.string.chats), R.drawable.ic_drawer_chats, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, ChatListActivity.class);
                startActivity(intent);
            }
        }, true));
        navItems.add(new NavigationItem(getString(R.string.drawer_menu_settings), R.drawable.ic_drawer_settings, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        }, true));
        navItems.add(new NavigationItem(getString(R.string.drawer_menu_help_feedback), R.drawable.ic_drawer_feedback, new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + SUPPORT_EMAIL));
                i.putExtra(Intent.EXTRA_SUBJECT, SUPPORT_SUBJECT);
                i.putExtra(Intent.EXTRA_TEXT, String.format(SUPPORT_BODY, Utils.getDeviceName(), Utils.getAndroidVersion()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(i, getString(R.string.send_email)));
            }
        }, true));
        navItems.add(new NavigationItem(getString(R.string.drawer_menu_about), R.drawable.ic_drawer_help, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        }, true));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPermissionsManager.addPermission(Manifest.permission.SEND_SMS, true, PackageManager.FEATURE_TELEPHONY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPermissionsManager.requestPermissions(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(mPermissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            // easy way to refresh (not optimized)
            refreshFragment(null);
        } else {
            Toast.makeText(this, R.string.msg_must_supply_mandatory_permissions, Toast.LENGTH_LONG).show();
            finish();
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
