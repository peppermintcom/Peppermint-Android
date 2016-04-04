package com.peppermint.app.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.canvas.loading.LoadingView;
import com.peppermint.app.ui.chat.head.ChatHeadServiceManager;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.NavigationItem;
import com.peppermint.app.ui.base.NavigationListAdapter;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 22-09-2015.
 *
 * Abstract activity implementation that uses Peppermint's custom action bar.
 */
public abstract class CustomActionBarActivity extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener,
        TouchInterceptable, ChatHeadServiceManager.ChatHeadServiceBinderListener {

    private static final String TAG = CustomActionBarActivity.class.getSimpleName();

    private static final String SAVED_MENU_POSITION_KEY = TAG + "_SAVED_MENU_POSITION_KEY";

    private ChatHeadServiceManager mChatHeadServiceManager;

    private List<NavigationItem> mNavigationItemList, mVisibleNavigationItemList;

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

    private List<View.OnTouchListener> mTouchEventInterceptorList = new ArrayList<>();

    protected List<NavigationItem> getNavigationItems() {
        return null;
    }
    protected int getContentViewResourceId() {
        return R.layout.a_custom_actionbar_layout;
    }
    protected int getBackgroundResourceId() { return R.color.background0; }

    // fragment loading
    private final Handler mHandler = new Handler();
    private AnimatorBuilder mAnimatorBuilder = new AnimatorBuilder();
    private View mFragmentLoadingContainer;
    private LoadingView mFragmentLoadingView;
    private final Runnable mShowFragmentLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            doFragmentLoading(false);
        }
    };
    // fragment loading animations
    private AnimatorChain mFragmentLoadingAnimatorChain;
    private Animator.AnimatorListener mFragmentLoadingAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            mFragmentLoadingView.startAnimations();
            mFragmentLoadingView.startDrawingThread();
            mFragmentLoadingContainer.setVisibility(View.VISIBLE);
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            mFragmentLoadingContainer.setVisibility(View.INVISIBLE);
            mFragmentLoadingView.stopAnimations();
            mFragmentLoadingView.stopDrawingThread();
            mFragmentLoadingAnimatorChain = null;
        }
        @Override
        public void onAnimationCancel(Animator animation) { }
        @Override
        public void onAnimationRepeat(Animator animation) { }
    };

    public void startFragmentLoading(Fragment fragment) {
        startFragmentLoading(getLoadingTextResId(fragment));
    }

    private int getLoadingTextResId(Fragment fragment) {
        int loadingTextResId = 0;
        Class<?> fragmentClass = fragment.getClass();
        int count = mNavigationItemList.size();
        for(int i=0; i<count && loadingTextResId == 0; i++) {
            if(mNavigationItemList.get(i).getFragmentClass() != null && mNavigationItemList.get(i).getFragmentClass().equals(fragmentClass)) {
                loadingTextResId = mNavigationItemList.get(i).getLoadingTextResId();
                if(loadingTextResId <= 0) {
                    loadingTextResId = -1;
                }
            }
        }
        return loadingTextResId;
    }

    public void startFragmentLoading(int loadingTextResId) {
        if(loadingTextResId > 0) {
            mFragmentLoadingView.setProgressText(getString(loadingTextResId));
        }
        mHandler.postDelayed(mShowFragmentLoadingRunnable, 100);
    }

    private void doFragmentLoading(int loadingTextResId, boolean avoidIfLeqZero, boolean noFadeInAnimation) {
        if(!avoidIfLeqZero || loadingTextResId > 0) {
            if(loadingTextResId > 0) {
                mFragmentLoadingView.setProgressText(getString(loadingTextResId));
            }
            doFragmentLoading(noFadeInAnimation);
        }
    }

    protected void doFragmentLoading(boolean noFadeInAnimation) {
        if(mFragmentLoadingAnimatorChain != null) {
            return;
        }

        FrameLayout fragmentContainer = getFragmentContainer();

        Animator fadeOut = mAnimatorBuilder.buildFadeOutAnimator(400, fragmentContainer);
        Animator fadeIn = mAnimatorBuilder.buildFadeInAnimator(400, mFragmentLoadingContainer);
        AnimatorSet startLoadingSet = new AnimatorSet();
        startLoadingSet.playTogether(fadeOut, fadeIn);

        fadeOut = mAnimatorBuilder.buildFadeOutAnimator(600, mFragmentLoadingContainer);
        fadeIn = mAnimatorBuilder.buildFadeInAnimator(600, fragmentContainer);
        AnimatorSet stopLoadingSet = new AnimatorSet();
        stopLoadingSet.playTogether(fadeOut, fadeIn);

        mFragmentLoadingAnimatorChain = new AnimatorChain(startLoadingSet, stopLoadingSet);
        mFragmentLoadingAnimatorChain.setAnimatorListener(mFragmentLoadingAnimatorListener);

        if(noFadeInAnimation) {
            mFragmentLoadingContainer.setAlpha(1f);
            mFragmentLoadingAnimatorListener.onAnimationStart(null);
        } else {
            mFragmentLoadingAnimatorChain.start();
        }
    }

    public void stopFragmentLoading(boolean finishAnimation) {
        mHandler.removeCallbacks(mShowFragmentLoadingRunnable);
        if(finishAnimation && mFragmentLoadingAnimatorChain != null) {
            mFragmentLoadingAnimatorChain.allowNext(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mChatHeadServiceManager = new ChatHeadServiceManager(this);
        mChatHeadServiceManager.addServiceBinderListener(this);

        setContentView(getContentViewResourceId());
        CustomActionBarView actionBar = getCustomActionBar();

        if(actionBar != null) {
            actionBar.initViews();
            actionBar.getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mLytDrawer.closeDrawers();
                    return false;
                }
            });
        }

        mPreferences = new SenderPreferences(this);

        mTrackerManager = TrackerManager.getInstance(getApplicationContext());
        mOverlayManager = new OverlayManager(this, null, (FrameLayout) findViewById(R.id.lytOverlay));
        mAuthenticationPolicyEnforcer = new AuthenticationPolicyEnforcer(this, savedInstanceState);
        mAuthenticationPolicyEnforcer.addAuthenticationDoneCallback(new AuthenticationPolicyEnforcer.AuthenticationDoneCallback() {
            @Override
            public void done(AuthenticationData data) {
                mAuthenticationData = data;
                refreshProfileData();
            }
        });

        // init loading recipients view
        mFragmentLoadingContainer = findViewById(R.id.fragmentProgressContainer);
        mFragmentLoadingView = (LoadingView) findViewById(R.id.loading);

        mImgUserAvatar = (ImageView) findViewById(R.id.imgUserAvatar);
        mTxtUsername = (TextView) findViewById(R.id.txtUserName);
        mLytDrawer = (DrawerLayout) findViewById(R.id.drawer);

        mNavigationItemList = getNavigationItems();
        mVisibleNavigationItemList = new ArrayList<>();
        NavigationItem firstNavigationItem = null;

        if(mNavigationItemList != null && mNavigationItemList.size() > 0) {
            for (NavigationItem item : mNavigationItemList) {
                if (item.isVisible()) {
                    mVisibleNavigationItemList.add(item);
                }
            }
            firstNavigationItem = mNavigationItemList.get(0);
        }

        if(mLytDrawer != null) {

            if (mNavigationItemList == null || mVisibleNavigationItemList.size() <= 0) {
                mLytDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                if (actionBar != null) {
                    actionBar.setDisplayMenuAsUpEnabled(true);
                    actionBar.getMenuButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Utils.hideKeyboard(CustomActionBarActivity.this);
                            finish();
                        }
                    });
                }
            } else {
                mLstDrawer = (ListView) findViewById(R.id.list);
                NavigationListAdapter adapter = new NavigationListAdapter(this, mVisibleNavigationItemList);
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

                if (actionBar != null) {
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
            }
        }

        mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        if (savedInstanceState != null) {
            if(mLytDrawer != null) {
                int pos = savedInstanceState.getInt(SAVED_MENU_POSITION_KEY, -1);
                if(pos >= 0) {
                    selectItemFromDrawer(pos);
                } else if(mNavigationItemList != null && mNavigationItemList.size() > 0) {
                    doFragmentLoading(mNavigationItemList.get(0).getLoadingTextResId(), true, true);
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

            doFragmentLoading(firstNavigationItem.getLoadingTextResId(), true, true);
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
        getWindow().setBackgroundDrawableResource(getBackgroundResourceId());
        mChatHeadServiceManager.startAndBind();
    }

    @Override
    protected void onStop() {
        mChatHeadServiceManager.removeVisibleActivity(this.getClass().getName());
        mChatHeadServiceManager.unbind();
        super.onStop();
    }

    @Override
    public void onBoundChatHeadService() {
        mChatHeadServiceManager.addVisibleActivity(this.getClass().getName());
    }

    @Override
    protected void onDestroy() {
        mOverlayManager.destroyAllOverlays();
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        for(View.OnTouchListener listener : mTouchEventInterceptorList) {
            listener.onTouch(null, ev);
        }
        return super.dispatchTouchEvent(ev);
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

        doFragmentLoading(getLoadingTextResId(fragment), true, false);
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
        if(mVisibleNavigationItemList == null || mVisibleNavigationItemList.size() <= 0) {
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
        NavigationItem navItem = mVisibleNavigationItemList.get(position);

        Fragment fragment = null;
        boolean isNewInstance = false;

        if(navItem.getAction() != null) {
            navItem.getAction().onPreFragmentInit();
        }

        // switch fragment if available
        if(navItem.getFragmentClass() != null) {
            doFragmentLoading(navItem.getLoadingTextResId(), true, false);
            mLstDrawer.setItemChecked(position, true);

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

                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.container, fragment, navItem.getTag()).commit();

                isNewInstance = true;
            }
        }

        // execute runnable if available
        if(navItem.getAction() != null) {
            navItem.getAction().onPostFragmentInit(fragment, isNewInstance);
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

    @Override
    public void addTouchEventInterceptor(View.OnTouchListener interceptor) {
        mTouchEventInterceptorList.add(interceptor);
    }

    @Override
    public boolean removeTouchEventInterceptor(View.OnTouchListener mTouchEventInterceptor) {
        return mTouchEventInterceptorList.remove(mTouchEventInterceptor);
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

    public SenderPreferences getPreferences() {
        return mPreferences;
    }
}
