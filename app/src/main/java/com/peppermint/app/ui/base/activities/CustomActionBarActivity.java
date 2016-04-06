package com.peppermint.app.ui.base.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.PermissionsPolicyEnforcer;
import com.peppermint.app.ui.TouchInterceptable;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.LoadingController;
import com.peppermint.app.ui.chat.head.ChatHeadServiceManager;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 22-09-2015.
 *
 * Abstract activity implementation that uses Peppermint's custom action bar.
 */
public abstract class CustomActionBarActivity extends FragmentActivity implements TouchInterceptable,
        ChatHeadServiceManager.ChatHeadServiceBinderListener {

    // loading controller
    protected LoadingController mLoadingController;

    // chat head overlay manager
    private ChatHeadServiceManager mChatHeadServiceManager;

    // activity overlay manager
    protected OverlayManager mOverlayManager;

    // authentication
    protected AuthenticationPolicyEnforcer mAuthenticationPolicyEnforcer;

    // permissions
    protected PermissionsPolicyEnforcer mPermissionsManager = new PermissionsPolicyEnforcer(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            "android.permission.READ_PROFILE",
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.GET_ACCOUNTS,
            "android.permission.USE_CREDENTIALS");

    // utility
    protected TrackerManager mTrackerManager;

    private List<View.OnTouchListener> mTouchEventInterceptorList = new ArrayList<>();

    protected String getTrackerLabel() { return null; }
    protected int getContainerViewLayoutId() { return 0; }
    protected int getContentViewLayoutId() { return R.layout.a_custom_actionbar_layout; }
    protected int getBackgroundResourceId() { return R.color.background0; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLoadingController = new LoadingController(this, R.id.fragmentProgressContainer, R.id.loading);

        mChatHeadServiceManager = new ChatHeadServiceManager(this);
        mChatHeadServiceManager.addServiceBinderListener(this);

        // init full content view
        setContentView(getContentViewLayoutId());

        int layoutResId = getContainerViewLayoutId();
        if(layoutResId > 0) {
            final FrameLayout lytContainer = (FrameLayout) findViewById(R.id.container);
            final LayoutInflater layoutInflater = LayoutInflater.from(this);
            layoutInflater.inflate(layoutResId, lytContainer, true);
        }

        // init action bar view
        final CustomActionBarView actionBar = getCustomActionBar();
        if(actionBar != null) {
            actionBar.initViews();
            actionBar.setDisplayMenuAsUpEnabled(true);
            actionBar.getMenuButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.hideKeyboard(CustomActionBarActivity.this);
                    finish();
                }
            });
        }

        // one more permission required
        mPermissionsManager.addPermission(Manifest.permission.SEND_SMS, true, PackageManager.FEATURE_TELEPHONY);

        mTrackerManager = TrackerManager.getInstance(getApplicationContext());

        mOverlayManager = new OverlayManager(this, null, (FrameLayout) findViewById(R.id.lytOverlay));

        mAuthenticationPolicyEnforcer = new AuthenticationPolicyEnforcer(this, savedInstanceState);

        final String trackerLabel = getTrackerLabel();
        if(trackerLabel != null) {
            mOverlayManager.setRootScreenId(trackerLabel);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setBackgroundDrawableResource(getBackgroundResourceId());
        mChatHeadServiceManager.startAndBind();
    }

    @Override
    protected void onResume() {
        super.onResume();
        final String trackerLabel = getTrackerLabel();
        if(trackerLabel != null) {
            mTrackerManager.trackScreenView(trackerLabel);
            mOverlayManager.setRootScreenId(trackerLabel);
        }
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
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        for(View.OnTouchListener listener : mTouchEventInterceptorList) {
            listener.onTouch(null, ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mAuthenticationPolicyEnforcer.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mAuthenticationPolicyEnforcer.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    public CustomActionBarView getCustomActionBar() {
        return (CustomActionBarView) findViewById(R.id.actionBar);
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

    public ViewGroup getContainerView() {
        return (ViewGroup) findViewById(R.id.container);
    }

    public LoadingController getLoadingController() {
        return mLoadingController;
    }
}
