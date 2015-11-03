package com.peppermint.app;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.recipients.RecipientsFragment;
import com.peppermint.app.ui.settings.SettingsActivity;
import com.peppermint.app.ui.tutorial.TutorialActivity;
import com.peppermint.app.ui.views.NavigationItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends CustomActionBarActivity {

    private static final int PERMISSION_REQUEST = 109;
    private static final String[] PERMISSIONS = {
        Manifest.permission.READ_CONTACTS,
        "android.permission.READ_PROFILE",
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,

        Manifest.permission.GET_ACCOUNTS,
        "android.permission.USE_CREDENTIALS",
        Manifest.permission.INSTALL_SHORTCUT,
        Manifest.permission.READ_PHONE_STATE
    };
    private static final String SUPPORT_EMAIL = "support@peppermint.com";
    private static final String SUPPORT_SUBJECT = "Feedback or question about Peppermint Android app";

    private List<String> mPermissionsToAsk;

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem(getString(R.string.menu_contacts), R.drawable.ic_drawer_contacts, RecipientsFragment.class, false, false));
        navItems.add(new NavigationItem(getString(R.string.menu_settings), R.drawable.ic_drawer_settings, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        }, true));
        navItems.add(new NavigationItem(getString(R.string.menu_tutorial), R.drawable.ic_drawer_help, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, TutorialActivity.class);
                startActivity(intent);
            }
        }, true));
        navItems.add(new NavigationItem(getString(R.string.menu_help_feedback), R.drawable.ic_drawer_feedback, new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + SUPPORT_EMAIL));
                i.putExtra(Intent.EXTRA_SUBJECT, SUPPORT_SUBJECT);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(i, getString(R.string.send_email)));
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
    protected void onResume() {
        super.onResume();
        Log.d("TAG", "onResume");

        mPermissionsToAsk = new ArrayList<>();
        for(int i=0; i<PERMISSIONS.length; i++) {
            if(ContextCompat.checkSelfPermission(this,
                    PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionsToAsk.add(PERMISSIONS[i]);
            }
        }

        // extra conditional permission SMS_SEND
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                mPermissionsToAsk.add(Manifest.permission.SEND_SMS);
            }
        }

        if(mPermissionsToAsk.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    mPermissionsToAsk.toArray(new String[mPermissionsToAsk.size()]),
                    PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST) {
            if (grantResults.length == mPermissionsToAsk.size()) {
                boolean permissionsGranted = true;
                for(int i=0; i<grantResults.length && permissionsGranted; i++) {
                    if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        permissionsGranted = false;
                    }
                }

                if(!permissionsGranted) {
                    Toast.makeText(this, R.string.must_supply_mandatory_permissions, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    // easy way to refresh (not optimized)
                    refreshFragment();
                }
            } else {
                Toast.makeText(this, R.string.must_supply_mandatory_permissions, Toast.LENGTH_LONG).show();
                finish();
            }
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
