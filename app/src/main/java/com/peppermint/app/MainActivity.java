package com.peppermint.app;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.peppermint.app.ui.recipients.RecipientsFragment;

public class MainActivity extends FragmentActivity {

    private static final String FRAGMENT_TAG = "RecipientsFragment";

    private RecipientsFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main_layout);

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
        int stepsLeft = mFragment.clearFilters();
        if(stepsLeft <= 0) {
            super.onBackPressed();
            return;
        }

        if(stepsLeft <= 1) {
            Toast.makeText(MainActivity.this, R.string.msg_press_back_again_exit, Toast.LENGTH_SHORT).show();
        }
    }
}
