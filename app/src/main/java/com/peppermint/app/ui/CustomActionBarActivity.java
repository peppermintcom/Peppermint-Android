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
 *
 * Abstract activity implementation that uses Peppermint's custom action bar.
 */
public abstract class CustomActionBarActivity  extends FragmentActivity {

    private static final String SAVED_MENU_POSITION_KEY = "DrawerActivity_SAVED_MENU_POSITION_KEY";

    private List<NavigationItem> mNavigationItemList;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mLytDrawer;
    private ListView mLstDrawer;
    protected PepperMintPreferences mPreferences;

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

        mPreferences = new PepperMintPreferences(this);
        PeppermintApp app = (PeppermintApp) getApplication();

        mNavigationItemList = getNavigationItems();

        String[] data = Utils.getUserData(this);
        ImageView imgUserAvatar = (ImageView) findViewById(R.id.imgUserAvatar);
        TextView txtUserName = (TextView) findViewById(R.id.txtUserName);
        txtUserName.setTypeface(app.getFontBold());
        if(data[1] != null) {
            imgUserAvatar.setImageURI(Uri.parse(data[1]));
        } else {
            imgUserAvatar.setImageResource(R.drawable.ic_anonymous_green_48dp);
        }
        data[0] = mPreferences.getDisplayName();
        txtUserName.setText(data[0]);

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
        Fragment introScreenFragment;
        try {
            introScreenFragment = mNavigationItemList.get(0).getFragmentClass().newInstance();
            if(getIntent() != null) {
                introScreenFragment.setArguments(getIntent().getExtras());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        getFragmentManager().beginTransaction().add(R.id.container, introScreenFragment).commit();
    }

    private void selectItemFromDrawer(final int position) {
        NavigationItem navItem = mNavigationItemList.get(position);

        if(navItem.getFragmentClass() != null) {
            Fragment currentFragment = getFragmentManager().findFragmentByTag(navItem.getTag());
            if (currentFragment != null && currentFragment.isVisible()) {
                mLytDrawer.closeDrawers();
                return;
            }

            Fragment fragment;
            try {
                fragment = navItem.getFragmentClass().newInstance();
                if(getIntent() != null) {
                    fragment.setArguments(getIntent().getExtras());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.container, fragment, navItem.getTag()).commit();

            mLstDrawer.setItemChecked(position, true);
        } else {
            navItem.getRunnable().run();
        }

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
        // pass the event to ActionBarDrawerToggle
        // if it returns true, then it has handled the nav drawer indicator touch event
        return (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(mDrawerToggle != null) {
            try {
                mDrawerToggle.syncState();
            } catch(Throwable t) {
                // just ignore; Android 4 launches an exception because there's no actionbar
            }
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

    public boolean isDrawerOpen() {
        return mLytDrawer.isDrawerOpen(GravityCompat.START);
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
