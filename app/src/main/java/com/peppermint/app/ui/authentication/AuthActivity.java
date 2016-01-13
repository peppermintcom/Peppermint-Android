package com.peppermint.app.ui.authentication;

import android.app.Activity;
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
 * Activity for user authentication.
 */
public class AuthActivity extends CustomActionBarActivity {

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("Sign In", R.drawable.ic_settings_36dp, AuthFragment.class, false, false));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.v_authentication_actionbar, null, false);
        getCustomActionBar().setContents(v, true);

        // disable back/menu button
        getCustomActionBar().getMenuButton().setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        // only return RESULT_OK if successful (check AuthFragment)
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}
