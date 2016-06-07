package com.peppermint.app.ui.base.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.services.authenticator.AuthenticationData;
import com.peppermint.app.ui.authentication.AuthenticatorActivity;
import com.peppermint.app.services.authenticator.AuthenticatorUtils;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiNoAccountException;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.OverlayManager;
import com.peppermint.app.ui.base.PermissionsPolicyEnforcer;
import com.peppermint.app.ui.base.TouchInterceptable;
import com.peppermint.app.ui.base.views.CustomActionBarView;
import com.peppermint.app.ui.base.LoadingController;
import com.peppermint.app.ui.chat.head.ChatHeadServiceManager;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 22-09-2015.
 * <p/>
 * Abstract activity implementation that uses Peppermint's custom action bar.
 */
public abstract class CustomActionBarActivity extends FragmentActivity implements TouchInterceptable,
        ChatHeadServiceManager.ChatHeadServiceBinderListener {

    // loading controller
    protected LoadingController mLoadingController;

    // chat head overlay manager
    protected ChatHeadServiceManager mChatHeadServiceManager;

    // activity overlay manager
    protected OverlayManager mOverlayManager;

    // authentication
    protected AuthenticatorUtils mAuthenticatorUtils;

    // permissions
    protected PermissionsPolicyEnforcer mPermissionsManager;

    // utility
    protected TrackerManager mTrackerManager;

    private List<View.OnTouchListener> mTouchEventInterceptorList = new ArrayList<>();

    /**
     * Override to add permissions to the {@link PermissionsPolicyEnforcer}
     * @param permissionsPolicyEnforcer the permissions policy enforcer instance
     */
    protected void onSetupPermissions(PermissionsPolicyEnforcer permissionsPolicyEnforcer) { /* nothing to do here */ }

    protected String getTrackerLabel() {
        return null;
    }

    protected int getContainerViewLayoutId() {
        return 0;
    }

    protected int getContentViewLayoutId() {
        return R.layout.a_custom_actionbar_layout;
    }

    protected int getBackgroundResourceId() {
        return R.color.background0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissionsManager = new PermissionsPolicyEnforcer();
        onSetupPermissions(mPermissionsManager);
        mLoadingController = new LoadingController(this, R.id.fragmentProgressContainer, R.id.loading);

        mChatHeadServiceManager = new ChatHeadServiceManager(this);
        mChatHeadServiceManager.addServiceBinderListener(this);

        // init full content view
        setContentView(getContentViewLayoutId());

        int layoutResId = getContainerViewLayoutId();
        if (layoutResId > 0) {
            final FrameLayout lytContainer = (FrameLayout) findViewById(R.id.container);
            final LayoutInflater layoutInflater = LayoutInflater.from(this);
            layoutInflater.inflate(layoutResId, lytContainer, true);
        }

        // init action bar view
        final CustomActionBarView actionBar = getCustomActionBar();
        if (actionBar != null) {
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

        mTrackerManager = TrackerManager.getInstance(getApplicationContext());

        mOverlayManager = new OverlayManager(this, null, (FrameLayout) findViewById(R.id.lytOverlay));

        mAuthenticatorUtils = new AuthenticatorUtils(this);

        final String trackerLabel = getTrackerLabel();
        if (trackerLabel != null) {
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
        if (trackerLabel != null) {
            mTrackerManager.trackScreenView(trackerLabel);
            mOverlayManager.setRootScreenId(trackerLabel);
        }

        mPermissionsManager.requestPermissions(this);
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

    /**
     * Override to take action whenever the user accepts all mandatory permissions.
     */
    protected void onPermissionsAccepted() { /* nothing to do here */ }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(mPermissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            onPermissionsAccepted();
        } else {
            Toast.makeText(this, R.string.msg_must_supply_mandatory_permissions, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        for (View.OnTouchListener listener : mTouchEventInterceptorList) {
            listener.onTouch(null, ev);
        }
        return super.dispatchTouchEvent(ev);
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

    protected Intent getIntentReplica() {
        final Intent intent = new Intent(getApplicationContext(), getClass());
        if(getIntent() != null) {
            if(getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }
            if(getIntent().getData() != null) {
                intent.setData(getIntent().getData());
            }
        }
        return intent;
    }

    public AuthenticationData getAuthenticationData() {
        return getAuthenticationData(getIntentReplica());
    }

    public AuthenticationData getAuthenticationData(Intent forwardAfterAuthIntent) {
        // check if the account has changed (or if there's a new account)
        mAuthenticatorUtils.refreshAccount();

        AuthenticationData authenticationData = null;

        try {
            authenticationData = mAuthenticatorUtils.getAccountData();
        } catch (PeppermintApiNoAccountException e) {
            /* nothing to do here */
        }

        if (forwardAfterAuthIntent != null && authenticationData == null && !isFinishing()) {
            Intent intent = new Intent(this, AuthenticatorActivity.class);
            intent.putExtra(AuthenticatorActivity.PARAM_FORWARD_TO, forwardAfterAuthIntent);
            startActivity(intent);

            Toast.makeText(this, R.string.msg_must_authenticate_using_account, Toast.LENGTH_LONG).show();
            finish();
        }

        return authenticationData;
    }
}
