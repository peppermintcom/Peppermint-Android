package com.peppermint.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.views.CustomActionBarView;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.ui.views.NavigationListAdapter;
import com.peppermint.app.utils.AnimatorBuilder;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nuno Luz on 22-09-2015.
 *
 * Abstract activity implementation that uses Peppermint's custom action bar.
 */
public abstract class CustomActionBarActivity  extends FragmentActivity {

    private static final String SAVED_MENU_POSITION_KEY = "DrawerActivity_SAVED_MENU_POSITION_KEY";

    private List<NavigationItem> mNavigationItemList;

    private FrameLayout mLytOverlay;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mLytDrawer;
    private ListView mLstDrawer;
    private ImageView mImgUserAvatar;
    private TextView mTxtUsername;
    protected PepperMintPreferences mPreferences;

    // Overlay
    private AnimatorBuilder mAnimatorBuilder;
    private Handler mDelayHandler = new Handler();
    private class OverlayWrapper {
        View view; boolean disableAllTouch, disableAutoScreenRotation;
        int requestedOrientation;
        Runnable onOverlayCancel;
    }
    private Map<String, OverlayWrapper> mOverlayMap = new HashMap<>();
    private Set<String> mOverlayHidding = new HashSet<>();
    private class OverlayHideAnimatorListener extends AnimatorListenerAdapter {
        private OverlayWrapper ow;
        private boolean isCancel = false;

        public OverlayHideAnimatorListener(OverlayWrapper ow, boolean isCancel) {
            this.ow = ow;
            this.isCancel = isCancel;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            ow.view.setVisibility(View.INVISIBLE);
            mOverlayHidding.remove(ow.view.getTag());
            if(ow.disableAllTouch) {
                enableDisableAllTouch(true);
            }
            if(ow.disableAutoScreenRotation) {
                setRequestedOrientation(ow.requestedOrientation);
            }
            if(isCancel && ow.onOverlayCancel != null) {
                ow.onOverlayCancel.run();
            }
            ow.onOverlayCancel = null;

            boolean allHidden = true;
            for(Map.Entry<String, OverlayWrapper> entry : mOverlayMap.entrySet()) {
                if(entry.getValue().view.getVisibility() == View.VISIBLE) {
                    allHidden = false;
                }
            }
            if (allHidden) {
                setFullscreen(false);
                mLytOverlay.setVisibility(View.GONE);
            }
        }
    }

    protected List<NavigationItem> getNavigationItems() {
        return null;
    }
    protected int getContentViewResourceId() {
        return R.layout.a_custom_actionbar_layout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAnimatorBuilder = new AnimatorBuilder();
        setContentView(getContentViewResourceId());
        getCustomActionBar().initViews();

        getCustomActionBar().getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mLytDrawer.closeDrawers();
                return false;
            }
        });

        mPreferences = new PepperMintPreferences(this);
        PeppermintApp app = (PeppermintApp) getApplication();

        mLytOverlay = (FrameLayout) findViewById(R.id.lytOverlay);

        mNavigationItemList = getNavigationItems();
        int amountVisible = 0;
        for(NavigationItem item : mNavigationItemList) {
            if(item.isVisible()) {
                amountVisible++;
            }
        }
        NavigationItem firstNavigationItem = mNavigationItemList.get(0);
        if(!firstNavigationItem.isVisible()) {
            mNavigationItemList.remove(0);
        }

        mImgUserAvatar = (ImageView) findViewById(R.id.imgUserAvatar);
        mTxtUsername = (TextView) findViewById(R.id.txtUserName);
        mTxtUsername.setTypeface(app.getFontBold());

        mLytDrawer = (DrawerLayout) findViewById(R.id.drawer);
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
            introScreenFragment = firstNavigationItem.getFragmentClass().newInstance();
            setFragmentArgumentsFromIntent(introScreenFragment, getIntent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        getFragmentManager().beginTransaction().add(R.id.container, introScreenFragment).commit();
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        refreshFragment(intent);
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
    protected void onResume() {
        super.onResume();

        // set the drawer profile data
        String[] data = Utils.getUserData(this);
        if(data[1] != null) {
            mImgUserAvatar.setImageURI(Uri.parse(data[1]));
        } else {
            mImgUserAvatar.setImageResource(R.drawable.ic_anonymous_green_48dp);
        }
        data[0] = mPreferences.getFullName();
        mTxtUsername.setText(data[0]);
    }

    /**
     * Create the overlay layout (inflated from the layout resource id)
     * @param layoutRes the layout resource id
     * @param tag the tag of the overlay
     * @return the root view containing the inflated layout
     */
    public View createOverlay(int layoutRes, String tag, boolean disableAllTouch, boolean disableAutoScreenRotation) {
        if(mOverlayMap.containsKey(tag)) {
            return mOverlayMap.get(tag).view;
        }

        LayoutInflater li = getLayoutInflater();
        View overlayView =  li.inflate(layoutRes, null);
        return createOverlay(overlayView, tag, disableAllTouch, disableAutoScreenRotation);
    }

    public View createOverlay(View overlayView, String tag, boolean disableAllTouch, boolean disableAutoScreenRotation) {
        if(mOverlayMap.containsKey(tag)) {
            return mOverlayMap.get(tag).view;
        }

        ViewGroup rootView = mLytOverlay;//(ViewGroup) getWindow().getDecorView();
        overlayView.setTag(tag);
        overlayView.setVisibility(View.INVISIBLE);
        OverlayWrapper ow = new OverlayWrapper();
        ow.view = overlayView;
        ow.disableAllTouch = disableAllTouch;
        ow.disableAutoScreenRotation = disableAutoScreenRotation;
        mOverlayMap.put(tag, ow);

        rootView.addView(overlayView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return overlayView;
    }

    /**
     * Sets the overlay associated with the tag to visible
     * @return true if the visibility of the overlay changed
     */
    public boolean showOverlay(String tag, boolean animated, Runnable onOverlayCancel) {
        if(!mOverlayMap.containsKey(tag)) {
            throw new RuntimeException("Overlay with tag " + tag + " does not exist!");
        }

        OverlayWrapper ow = mOverlayMap.get(tag);
        ow.onOverlayCancel = onOverlayCancel;
        if(ow.view.getVisibility() == View.VISIBLE) {
            return false;
        }

        setFullscreen(true);
        mLytOverlay.setVisibility(View.VISIBLE);
        ow.view.setVisibility(View.VISIBLE);

        if(ow.disableAllTouch) {
            enableDisableAllTouch(false);
        }

        if(ow.disableAutoScreenRotation) {
            ow.requestedOrientation = getRequestedOrientation();
            //noinspection ResourceType
            setRequestedOrientation(getActivityInfoOrientation());
        }

        if(animated) {
            Animator anim = mAnimatorBuilder.buildFadeInAnimator(ow.view);
            anim.setDuration(400);
            anim.start();
        }

        return true;
    }

    private int getActivityInfoOrientation() {
        // rotation depends on devices natural orientation (in tablets it's landscape; portrait on phones)
        // thus, 0 rotation is landscape on tablets and portrait on phones
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
            return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }

        if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    }

    /**
     * Shows all overlays. See {@link #showOverlay(String, boolean, Runnable)} for more information.
     * @return true if at least one overlay was shown; false otherwise
     */
    public boolean showAllOverlays(boolean animated, Runnable onOverlayCancel) {
        boolean changed = false;

        for(String tag : mOverlayMap.keySet()) {
            if(showOverlay(tag, animated, onOverlayCancel)) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Sets the overlay associated with the tag to invisible
     * @return true if the visibility of the overlay changed
     */
    public boolean hideOverlay(String tag, long delay, final boolean animated) {
        return hideOverlay(tag, delay, animated, false);
    }

    protected boolean hideOverlay(String tag, long delay, final boolean animated, boolean isCancel) {
        if(!mOverlayMap.containsKey(tag)) {
            throw new RuntimeException("Overlay with tag " + tag + " does not exist!");
        }

        final OverlayWrapper ow = mOverlayMap.get(tag);
        if(ow.view.getVisibility() == View.INVISIBLE || mOverlayHidding.contains(tag)) {
            return false;
        }

        final OverlayHideAnimatorListener overlayListener = new OverlayHideAnimatorListener(ow, isCancel);
        mOverlayHidding.add(tag);

        mDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(animated) {
                    Animator anim = mAnimatorBuilder.buildFadeOutAnimator(ow.view);
                    anim.setDuration(400);
                    anim.addListener(overlayListener);
                    anim.start();
                } else {
                    overlayListener.onAnimationEnd(null);
                }
            }
        }, delay);

        return true;
    }

    /**
     * Hides all overlays. See {@link #hideOverlay(String, long, boolean)} for more information.
     * @param delay a delay before starting the hidding animation
     * @return true if at least one overlay was hidden; false otherwise
     */
    public boolean hideAllOverlays(long delay, boolean animated) {
        return hideAllOverlays(delay, animated, false);
    }

    public boolean hideAllOverlays(long delay, boolean animated, boolean isCancel) {
        boolean changed = false;

        for(String tag : mOverlayMap.keySet()) {
            if(hideOverlay(tag, delay, animated, isCancel)) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Destroys the overlay associated with the tag
     * @param tag the tag of the overlay
     */
    public void destroyOverlay(String tag) {
        if(!mOverlayMap.containsKey(tag)) {
            throw new RuntimeException("Overlay view with tag " + tag + " does not exist!");
        }

        View v = mOverlayMap.get(tag).view;
        mOverlayMap.remove(tag);
        mOverlayHidding.remove(tag);
        ViewGroup rootView = (ViewGroup) getWindow().getDecorView();
        rootView.removeView(v);
    }

    /**
     * Enables/disables touch capture events in the whole screen.
     * @param enabled true to enable; false to disable
     */
    public void enableDisableAllTouch(boolean enabled) {
        enableDisableTouch((ViewGroup) mLytDrawer.getParent(), enabled);
    }

    /**
     * Enables/disables touch capture in the specified ViewGroup
     * @param vg the viewgroup to be enabled/disabled
     * @param enabled true to enable; false to disable
     */
    protected void enableDisableTouch(ViewGroup vg, boolean enabled) {
        for (int i = 0; i < vg.getChildCount(); i++){
            View child = vg.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup){
                enableDisableTouch((ViewGroup) child, enabled);
            }
        }
    }

    private void setFullscreen(boolean fullscreen)
    {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen)
        {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        else
        {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
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

    public View getOverlayLayout() {
        return findViewById(R.id.lytOverlay);
    }

}
