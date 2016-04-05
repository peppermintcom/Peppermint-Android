package com.peppermint.app.ui.base.activities;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.NavigationItem;
import com.peppermint.app.ui.base.NavigationListAdapter;
import com.peppermint.app.utils.Utils;

import java.util.List;

/**
 * Created by Nuno Luz on 22-09-2015.
 *
 * Abstract activity implementation that uses Peppermint's custom action bar.
 */
public abstract class CustomActionBarDrawerActivity extends CustomActionBarActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = CustomActionBarDrawerActivity.class.getSimpleName();

    private static final String SAVED_MENU_POSITION_KEY = TAG + "_SAVED_MENU_POSITION_KEY";

    protected SenderPreferences mPreferences;

    // list of items in the drawer menu
    private List<NavigationItem> mNavigationItemList;

    // drawer menu toggle button
    private ActionBarDrawerToggle mDrawerToggle;

    // drawer UI
    private DrawerLayout mLytDrawer;
    private ListView mLstDrawer;
    private ImageView mImgUserAvatar;
    private TextView mTxtUsername;

    private int mCheckedItemPosition = -1;

    // authentication data
    private AuthenticationData mAuthenticationData;

    protected List<NavigationItem> getNavigationItems() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = new SenderPreferences(this);

        final CustomActionBarView actionBar = getCustomActionBar();
        if(actionBar != null) {
            actionBar.getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mLytDrawer.closeDrawers();
                    return false;
                }
            });
        }

        mAuthenticationPolicyEnforcer.addAuthenticationDoneCallback(new AuthenticationPolicyEnforcer.AuthenticationDoneCallback() {
            @Override
            public void done(AuthenticationData data) {
                mAuthenticationData = data;
                refreshProfileData();
            }
        });

        mImgUserAvatar = (ImageView) findViewById(R.id.imgUserAvatar);
        mTxtUsername = (TextView) findViewById(R.id.txtUserName);

        mLytDrawer = (DrawerLayout) findViewById(R.id.drawer);
        mLstDrawer = (ListView) findViewById(R.id.list);

        mNavigationItemList = getNavigationItems();
        NavigationListAdapter adapter = new NavigationListAdapter(this, mNavigationItemList);
        mLstDrawer.setAdapter(adapter);

        // drawer Item click listeners
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

        if (actionBar != null) {
            actionBar.setDisplayMenuAsUpEnabled(false);
            actionBar.getMenuButton().setOnClickListener(new View.OnClickListener() {
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

        mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mLstDrawer.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mLstDrawer.setSelection(0);

        if (savedInstanceState != null) {
            int pos = savedInstanceState.getInt(SAVED_MENU_POSITION_KEY, -1);
            if(pos >= 0) {
                mCheckedItemPosition = pos;
                mLstDrawer.setItemChecked(pos, true);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // perform one action from the drawer (init the default fragment)
        selectItemFromDrawer(mLstDrawer.getCheckedItemPosition() >= 0 ? mLstDrawer.getCheckedItemPosition() : 0);

        if(mDrawerToggle != null) {
            try {
                mDrawerToggle.syncState();
            } catch(Throwable t) {
                // just ignore; Android 4 launches an exception because there's no actionbar
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        refreshFragment(intent);
    }

    @Override
    protected void onDestroy() {
        mPreferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(mLytDrawer != null && mLytDrawer.isDrawerOpen(GravityCompat.START)) {
            mLytDrawer.closeDrawers();
            return;
        }
        super.onBackPressed();
    }

    private void setFragmentArgumentsFromIntent(Fragment fragment, Intent intent) {
        if(intent != null && fragment != null) {
            Bundle bundle = new Bundle();

            if(getIntent().getExtras() != null) {
                bundle.putAll(getIntent().getExtras());
            }

            Uri uri = getIntent().getData();
            if(uri != null) {
                for(String paramName : uri.getQueryParameterNames()) {
                    String paramValue = uri.getQueryParameter(paramName);
                    if (paramValue != null) {
                        bundle.putString(paramName, paramValue);
                    }
                }
            }
            fragment.setArguments(bundle);
        }
    }

    protected void refreshFragment(Intent intent) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.container);
        if(fragment == null) {
            return;
        }
        try {
            fragment = fragment.getClass().newInstance();
            setFragmentArgumentsFromIntent(fragment, intent == null ? getIntent() : intent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(SenderPreferences.FIRST_NAME_KEY) || key.equals(SenderPreferences.LAST_NAME_KEY)) {
            refreshProfileData();
        }
    }

    protected boolean refreshProfileData() {
        String[] data = Utils.getUserData(this);
        if(data[1] != null) {
            mImgUserAvatar.setImageURI(Uri.parse(data[1]));
        } else {
            mImgUserAvatar.setImageResource(R.drawable.ic_anonymous_green_48dp);
        }

        data[0] = mPreferences.getFullName();

        if(Utils.isValidName(data[0])) {
            mTxtUsername.setText(data[0]);
        } else {
            if(mAuthenticationData == null) {
                Log.d(TAG, "No authentication data. Skipping username refresh...");
            } else {
                mTxtUsername.setText(mAuthenticationData.getEmail());
            }
        }

        return true;
    }

    protected boolean selectItemFromDrawer(final int position) {
        NavigationItem navItem = mNavigationItemList.get(position);

        mLstDrawer.setItemChecked(position, true);

        if(navItem.getFragmentClass() != null && mCheckedItemPosition == position) {
            // already selected
            mLytDrawer.closeDrawers();
            return false;
        }

        Fragment fragment = null;
        boolean isNewInstance = false;

        // switch fragment if available
        if(navItem.getFragmentClass() != null) {
            mCheckedItemPosition = position;

            fragment = getFragmentManager().findFragmentByTag(navItem.getTag());

            if (fragment == null || !fragment.isVisible()) {
                try {
                    fragment = navItem.getFragmentClass().newInstance();
                    if (getIntent() != null) {
                        fragment.setArguments(getIntent().getExtras());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                isNewInstance = true;
            }

            if(navItem.getAction() != null) {
                navItem.getAction().onPreFragmentInit(fragment, isNewInstance);
            }

            getFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment, navItem.getTag()).commit();
        } else {
            if(navItem.getAction() != null) {
                navItem.getAction().onPreFragmentInit(null, false);
            }
        }

        // execute runnable if available
        if (navItem.getAction() != null) {
            navItem.getAction().onPostFragmentInit(fragment, isNewInstance);
        }

        mLytDrawer.closeDrawers();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mLstDrawer != null) {
            outState.putInt(SAVED_MENU_POSITION_KEY, mLstDrawer.getCheckedItemPosition());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // pass the event to ActionBarDrawerToggle
        // if it returns true, then it has handled the nav drawer indicator touch event
        return (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) || super.onOptionsItemSelected(item);
    }

    public boolean isDrawerOpen() {
        return mLytDrawer.isDrawerOpen(GravityCompat.START);
    }

    public Fragment getCurrentFragment() {
        return getFragmentManager().findFragmentById(R.id.container);
    }
}
