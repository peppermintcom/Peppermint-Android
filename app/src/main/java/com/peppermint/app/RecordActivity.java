package com.peppermint.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.peppermint.app.ui.RecordFragment;

public class RecordActivity extends Activity {

    private static final String FRAGMENT_TAG = "RecordFragment";

    private RecordFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_layout);

        if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            mFragment = (RecordFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
            return; // avoids duplicate fragments
        }

        // show intro screen
        mFragment = new RecordFragment();
        getFragmentManager().beginTransaction().add(R.id.container, mFragment, FRAGMENT_TAG).commit();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        mFragment.onBackPressed();
    }
}
