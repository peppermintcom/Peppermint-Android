package com.peppermint.app.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.views.CustomActionBarView;
import com.peppermint.app.ui.views.DrawerListAdapter;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

import java.util.List;

/**
 * Created by Nuno Luz on 22-09-2015.
 */
public abstract class CustomActionBarActivity  extends FragmentActivity {

    private static final String SAVED_MENU_POSITION_KEY = "DrawerActivity_SAVED_MENU_POSITION_KEY";

    private List<NavigationItem> mNavigationItemList;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mLytDrawer;
    private ListView mLstDrawer;

    protected List<NavigationItem> getNavigationItems() {
        return null;
    }
    protected int getContentViewResourceId() {
        return R.layout.a_custom_actionbar_layout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewResourceId());
        getCustomActionBar().initViews();

        PepperMintPreferences preferences = new PepperMintPreferences(this);
        PeppermintApp app = (PeppermintApp) getApplication();

        mNavigationItemList = getNavigationItems();

        String[] data = Utils.getUserData(this);
        ImageView imgUserAvatar = (ImageView) findViewById(R.id.imgUserAvatar);
        TextView txtUserName = (TextView) findViewById(R.id.txtUserName);
        txtUserName.setTypeface(app.getFontBold());
        if(data[1] != null) {
            imgUserAvatar.setImageURI(Uri.parse(data[1]));
        } else {
            imgUserAvatar.setImageResource(R.drawable.ic_anonymous_green_30dp);
        }
        data[0] = preferences.getDisplayName();
        if(data[0] != null) {
            txtUserName.setText(data[0]);
        } else {
            txtUserName.setText(R.string.app_name);
        }

        mLytDrawer = (DrawerLayout) findViewById(R.id.drawer);
        if(mNavigationItemList == null || mNavigationItemList.size() <= 1) {
            mLytDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            getCustomActionBar().setDisplayMenuAsUpEnabled(true);
            getCustomActionBar().getMenuButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            mLstDrawer = (ListView) findViewById(R.id.list);
            DrawerListAdapter adapter = new DrawerListAdapter(this, mNavigationItemList, app.getFontSemibold());
            mLstDrawer.setAdapter(adapter);

            // Drawer Item click listeners
            mLstDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectItemFromDrawer(position);
                }
            });

            mDrawerToggle = new ActionBarDrawerToggle(this, mLytDrawer, null, R.string.drawer_open_desc, R.string.drawer_closed_desc) {
                @Override
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    invalidateOptionsMenu();
                }

                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    invalidateOptionsMenu();
                }
            };
            mLytDrawer.setDrawerListener(mDrawerToggle);

            getCustomActionBar().getMenuButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mLytDrawer.isDrawerOpen(GravityCompat.START)) {
                        mLytDrawer.closeDrawers();
                    } else {
                        mLytDrawer.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        if (savedInstanceState != null) {
            if(mLytDrawer != null) {
                int pos = savedInstanceState.getInt(SAVED_MENU_POSITION_KEY, -1);
                if(pos >= 0) {
                    selectItemFromDrawer(pos);
                }
            }
            return; // avoids duplicate fragments
        }

        // show intro screen
        Fragment introScreenFragment = null;
        try {
            introScreenFragment = mNavigationItemList.get(0).getFragmentClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        getFragmentManager().beginTransaction().add(R.id.container, introScreenFragment).commit();
    }

    private void selectItemFromDrawer(final int position) {
        NavigationItem navItem = mNavigationItemList.get(position);

        Fragment currentFragment = getFragmentManager().findFragmentByTag(navItem.getTag());
        if (currentFragment != null && currentFragment.isVisible()) {
            mLytDrawer.closeDrawers();
            return;
        }

        Fragment fragment = null;
        try {
            fragment = navItem.getFragmentClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, fragment, navItem.getTag()).commit();

        mLstDrawer.setItemChecked(position, true);
        mLytDrawer.closeDrawers();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(mLstDrawer != null) {
            outState.putInt(SAVED_MENU_POSITION_KEY, mLstDrawer.getSelectedItemPosition());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle
        // If it returns true, then it has handled the nav drawer indicator touch event
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onBackPressed() {
        if(mLytDrawer.isDrawerOpen(GravityCompat.START)) {
            mLytDrawer.closeDrawers();
            return;
        }
        super.onBackPressed();
    }

    public CustomActionBarView getCustomActionBar() {
        return (CustomActionBarView) findViewById(R.id.actionBar);
    }

    public FrameLayout getFragmentContainer() {
        return (FrameLayout) findViewById(R.id.container);
    }

    public Fragment getCurrentFragment() {
        return getFragmentManager().findFragmentById(R.id.container);
    }

    public View getTouchInterceptor() {
        return findViewById(R.id.touchInterceptor);
    }

}
