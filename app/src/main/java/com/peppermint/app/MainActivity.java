package com.peppermint.app;

import android.Manifest;
import android.app.Activity;
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
import com.peppermint.app.ui.authentication.AuthFragment;
import com.peppermint.app.ui.recipients.RecipientsFragment;
import com.peppermint.app.ui.settings.SettingsActivity;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends CustomActionBarActivity {

    private static final int AUTHENTICATION_REQUEST = 110;
    private static final int PERMISSION_REQUEST = 109;
    private static final String[] PERMISSIONS = {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        "android.permission.READ_PROFILE",
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,

        Manifest.permission.GET_ACCOUNTS,
        "android.permission.USE_CREDENTIALS",
        Manifest.permission.READ_PHONE_STATE
    };
    private static final String SUPPORT_EMAIL = "support@peppermint.com";
    private static final String SUPPORT_SUBJECT = "Feedback or question about Peppermint Android app";
    private static final String SUPPORT_BODY = "\n\n\n\nNote regarding this feedback. Was provided by %1$s running %2$s with Peppermint v" + BuildConfig.VERSION_NAME;

    private List<String> mPermissionsToAsk;
    private boolean mNeedsToAuthorize = true;

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
        /*navItems.add(new NavigationItem(getString(R.string.menu_tutorial), R.drawable.ic_drawer_help, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, TutorialActivity.class);
                startActivity(intent);
            }
        }, true));*/
        navItems.add(new NavigationItem(getString(R.string.menu_help_feedback), R.drawable.ic_drawer_feedback, new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + SUPPORT_EMAIL));
                i.putExtra(Intent.EXTRA_SUBJECT, SUPPORT_SUBJECT);
                i.putExtra(Intent.EXTRA_TEXT, String.format(SUPPORT_BODY, Utils.getDeviceName(), Utils.getAndroidVersion()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(i, getString(R.string.send_email)));
            }
        }, true));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*if(mPreferences.isFirstRun()) {
            // launch tutorial
            Intent tutorialIntent = new Intent(this, TutorialActivity.class);
            startActivity(tutorialIntent);
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("TAG", "onResume");

        mPermissionsToAsk = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this,
                    permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionsToAsk.add(permission);
            }
        }

        // extra conditional permission SMS_SEND
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                mPermissionsToAsk.add(Manifest.permission.SEND_SMS);
            }
        }

        // first request all permissions
        if (mPermissionsToAsk.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    mPermissionsToAsk.toArray(new String[mPermissionsToAsk.size()]),
                    PERMISSION_REQUEST);
        } else {
            // afterwards, request authentication
            AuthFragment.startAuthentication(this, AUTHENTICATION_REQUEST, mNeedsToAuthorize);
        }

        mNeedsToAuthorize = false;

        /*if(mPreferences.isFirstRun()) {
            mPreferences.setFirstRun(false);
        }*/
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
                    refreshFragment(null);
                }
            } else {
                Toast.makeText(this, R.string.must_supply_mandatory_permissions, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == AUTHENTICATION_REQUEST) {
            if(resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, R.string.must_authenticate_using_account, Toast.LENGTH_LONG).show();
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
