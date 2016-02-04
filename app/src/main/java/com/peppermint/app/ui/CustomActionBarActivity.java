package com.peppermint.app.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.views.CustomActionBarView;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.ui.views.NavigationListAdapter;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.utils.Utils;

import java.util.List;

/**
 * Created by Nuno Luz on 22-09-2015.
 *
 * Abstract activity implementation that uses Peppermint's custom action bar.
 */
public abstract class CustomActionBarActivity extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = CustomActionBarActivity.class.getSimpleName();

    private static final String SAVED_MENU_POSITION_KEY = TAG + "_SAVED_MENU_POSITION_KEY";

    private List<NavigationItem> mNavigationItemList;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mLytDrawer;
    private ListView mLstDrawer;
    private ImageView mImgUserAvatar;
    private TextView mTxtUsername;

    private OverlayManager mOverlayManager;
    protected SenderPreferences mPreferences;
    private TrackerManager mTrackerManager;
    private AuthenticationPolicyEnforcer mAuthenticationPolicyEnforcer;
    private AuthenticationData mAuthenticationData;

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

        getCustomActionBar().getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mLytDrawer.closeDrawers();
                return false;
            }
        });

        mPreferences = new SenderPreferences(this);

        mTrackerManager = TrackerManager.getInstance(getApplicationContext());
        mOverlayManager = new OverlayManager(this, R.id.lytOverlay);
        mAuthenticationPolicyEnforcer = new AuthenticationPolicyEnforcer(this, savedInstanceState);
        mAuthenticationPolicyEnforcer.addAuthenticationDoneCallback(new AuthenticationPolicyEnforcer.AuthenticationDoneCallback() {
            @Override
            public void done(AuthenticationData data) {
                mAuthenticationData = data;
                refreshProfileData();
            }
        });

        PeppermintApp app = (PeppermintApp) getApplication();

        mImgUserAvatar = (ImageView) findViewById(R.id.imgUserAvatar);
        mTxtUsername = (TextView) findViewById(R.id.txtUserName);
        mTxtUsername.setTypeface(app.getFontSemibold());
        mLytDrawer = (DrawerLayout) findViewById(R.id.drawer);

        mNavigationItemList = getNavigationItems();
        int amountVisible = 0;
        NavigationItem firstNavigationItem = null;

        if(mNavigationItemList != null && mNavigationItemList.size() > 0) {
            for (NavigationItem item : mNavigationItemList) {
                if (item.isVisible()) {
                    amountVisible++;
                }
            }
            firstNavigationItem = mNavigationItemList.get(0);
            if (!firstNavigationItem.isVisible()) {
                mNavigationItemList.remove(0);
            }
        }

        if(mNavigationItemList == null || amountVisible <= 0) {
            mLytDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            getCustomActionBar().setDisplayMenuAsUpEnabled(true);
            getCustomActionBar().getMenuButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.hideKeyboard(CustomActionBarActivity.this);
                    finish();
                }
            });
        } else {
            mLstDrawer = (ListView) findViewById(R.id.list);
            NavigationListAdapter adapter = new NavigationListAdapter(this, mNavigationItemList, app.getFontSemibold());
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

        mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        if (savedInstanceState != null) {
            if(mLytDrawer != null) {
                int pos = savedInstanceState.getInt(SAVED_MENU_POSITION_KEY, -1);
                if(pos >= 0) {
                    selectItemFromDrawer(pos);
                }
            }
            return; // avoids duplicate fragments
        }

        if(firstNavigationItem != null) {
            // show intro screen
            Fragment introScreenFragment;
            try {
                introScreenFragment = firstNavigationItem.getFragmentClass().newInstance();
                setFragmentArgumentsFromIntent(introScreenFragment, getIntent());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            getFragmentManager().beginTransaction().add(R.id.container, introScreenFragment).commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        refreshFragment(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setBackgroundDrawableResource(R.color.background0);
    }

    @Override
    protected void onDestroy() {
        mPreferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mAuthenticationPolicyEnforcer.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(SenderPreferences.FIRST_NAME_KEY) || key.equals(SenderPreferences.LAST_NAME_KEY)) {
            refreshProfileData();
        }
    }

    protected boolean refreshProfileData() {
        if(mNavigationItemList == null || mNavigationItemList.size() <= 0) {
            Log.d(TAG, "Drawer is locked and profile is hidden. Skipping profile info refresh...");
            return false;
        }

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

    protected void selectItemFromDrawer(final int position) {
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
        mAuthenticationPolicyEnforcer.saveInstanceState(outState);
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

    public TrackerManager getTrackerManager() {
        return mTrackerManager;
    }

    public OverlayManager getOverlayManager() {
        return mOverlayManager;
    }

    public AuthenticationPolicyEnforcer getAuthenticationPolicyEnforcer() {
        return mAuthenticationPolicyEnforcer;
    }
}
