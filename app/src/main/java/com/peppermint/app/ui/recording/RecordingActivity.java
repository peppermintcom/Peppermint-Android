package com.peppermint.app.ui.recording;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.views.NavigationItem;

import java.util.ArrayList;
import java.util.List;

public class RecordingActivity extends CustomActionBarActivity {

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("Recording", R.drawable.ic_recipienttype_allcontacts, RecordingFragment.class));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeppermintApp app = (PeppermintApp) getApplication();

        Recipient recipient = (Recipient) getIntent().getExtras().get(RecordingFragment.INTENT_RECIPIENT_EXTRA);

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.f_recording_actionbar, null, false);

        ((TextView) v.findViewById(R.id.txtRecordMessage)).setTypeface(app.getFontSemibold());
        TextView txtRecipient = ((TextView) v.findViewById(R.id.txtRecipient));
        txtRecipient.setText(getString(R.string._for) + " " + recipient.getName());
        txtRecipient.setTypeface(app.getFontSemibold());

        getCustomActionBar().setContents(v, true);
    }

}
