package com.peppermint.app.ui.chat;

import android.os.Bundle;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.views.NavigationItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * Activity for user chat list.
 */
public class ChatListActivity extends CustomActionBarActivity {

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("Conversations", R.drawable.ic_settings_36dp, ChatListFragment.class, false, false));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.v_chatlist_actionbar, null, false);
        getCustomActionBar().setContents(v, true);
    }

}
