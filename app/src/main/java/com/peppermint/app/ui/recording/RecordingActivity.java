package com.peppermint.app.ui.recording;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.utils.Utils;

import org.w3c.dom.Text;

public class RecordingActivity extends Activity {

    private static final String FRAGMENT_TAG = "RecordingFragment";

    private RecordingFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_record_layout);

        PeppermintApp app = (PeppermintApp) getApplication();

        /*if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }*/

        // bo: adjust status bar height
        int statusBarHeight = Utils.getStatusBarHeight(this);
        findViewById(R.id.customActionBarTopSpace).getLayoutParams().height = statusBarHeight;
        View lytActionBar = findViewById(R.id.customActionBar);
        lytActionBar.getLayoutParams().height = lytActionBar.getLayoutParams().height + statusBarHeight;
        // eo: adjust status bar height

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Recipient recipient = (Recipient) getIntent().getExtras().get(RecordingFragment.INTENT_RECIPIENT_EXTRA);

        ((TextView) findViewById(R.id.txtRecordMessage)).setTypeface(app.getFontSemibold());
        TextView txtRecipient = ((TextView) findViewById(R.id.txtRecipient));
        txtRecipient.setText(recipient.getName());
        txtRecipient.setTypeface(app.getFontSemibold());

        if (savedInstanceState != null) {
            mFragment = (RecordingFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
            return; // avoids duplicate fragments
        }

        // show intro screen
        mFragment = new RecordingFragment();
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

}
