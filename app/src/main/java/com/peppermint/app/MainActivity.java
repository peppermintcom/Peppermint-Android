package com.peppermint.app;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.peppermint.app.ui.RecipientsFragment;

public class MainActivity extends FragmentActivity {

    private static final String FRAGMENT_TAG = "RecipientsFragment";

    private RecipientsFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            return;
        }
        super.onBackPressed();
    }
}
