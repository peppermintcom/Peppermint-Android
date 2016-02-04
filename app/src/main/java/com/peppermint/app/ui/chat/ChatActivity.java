package com.peppermint.app.ui.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.views.NavigationItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * Activity for user authentication.
 */
public class ChatActivity extends CustomActionBarActivity {

    private Button mBtnSave;

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("Chat", R.drawable.ic_settings_36dp, ChatFragment.class, false, false));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeppermintApp app = (PeppermintApp) getApplication();

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.v_newcontact_actionbar, null, false);

        TextView txtTitle = ((TextView) v.findViewById(R.id.txtTitle));
        txtTitle.setTypeface(app.getFontSemibold());

        mBtnSave = (Button) v.findViewById(R.id.btnSave);
        mBtnSave.setTypeface(app.getFontSemibold());

        getCustomActionBar().setContents(v, false);

        // cancel new contact icon
        getCustomActionBar().getMenuButton().setImageResource(R.drawable.ic_cancel_14dp);
    }

}
