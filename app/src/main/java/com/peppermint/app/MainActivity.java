package com.peppermint.app;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.ui.RecipientsFragment;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends FragmentActivity {

    private static final String FRAGMENT_TAG = "RecipientsFragment";

    private RecipientsFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main_layout);

        if (savedInstanceState != null) {
            mFragment = (RecipientsFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
            return; // avoids duplicate fragments
        }

        // show intro screen
        mFragment = new RecipientsFragment();
        getFragmentManager().beginTransaction().add(R.id.container, mFragment, FRAGMENT_TAG).commit();
    }

    @Override
    public void onBackPressed() {
        if(mFragment.clearSearchFilter()) {
            Toast.makeText(MainActivity.this, R.string.msg_press_back_again_exit, Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}
