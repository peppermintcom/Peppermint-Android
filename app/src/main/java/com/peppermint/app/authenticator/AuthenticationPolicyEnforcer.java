package com.peppermint.app.authenticator;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiNoAccountException;
import com.peppermint.app.tracking.TrackerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 04-02-2016.
 * <p>
 *      Defines an authentication policy for an Activity.<br />
 *      If set to require authentication, the policy will check if there's a valid account every time
 *      the Activity is resumed. If no account exists, new credentials are requested to the user.<br />
 *      If new credentials are refused and the policy enforces authentication
 *      (see {@link #setRequiresAuthentication(boolean)}), the Activity is forcefully finished.
 * </p>
 * <p><strong>This doesn't check if the credentials are valid! Only if the account exists.</strong></p>
 */
public class AuthenticationPolicyEnforcer {

    public interface AuthenticationDoneCallback {
        void done(AuthenticationData data);
    }

    private static final String TAG = AuthenticationPolicyEnforcer.class.getSimpleName();

    private static final String PARAM_IS_AUTHENTICATING = TAG + "_paramIsAuthenticating";

    private static final int AUTH_REQUEST_CODE = 3432;

    private Activity mActivity;
    private TrackerManager mTrackerManager;
    private AuthenticatorUtils mAuthenticatorUtils;

    private boolean mRequiresAuthentication = true;
    private List<AuthenticationDoneCallback> mAuthenticationDoneCallbacks;
    private boolean mIsAuthenticating = false;

    // listens for Activity lifecycle events
    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallback = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            /* this is never called since the policy enforced is only set afterwards */
        }

        @Override
        public void onActivityStarted(Activity activity) { }

        @Override
        public void onActivityPaused(Activity activity) { }

        @Override
        public void onActivityStopped(Activity activity) { }

        @Override
        public void onActivityResumed(Activity activity) {
            if(activity.equals(mActivity)) {
                if (isRequiresAuthentication()) {
                    getAuthenticationData();
                }
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            if(activity.equals(mActivity)) {
                saveInstanceState(outState);
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if(activity.equals(mActivity)) {
                mActivity.getApplication().unregisterActivityLifecycleCallbacks(this);
                mAuthenticationDoneCallbacks.clear();
            }
        }
    };

    public AuthenticationPolicyEnforcer(Activity activityWithAuthentication, Bundle savedInstanceState) {
        this.mAuthenticationDoneCallbacks = new ArrayList<>();
        this.mActivity = activityWithAuthentication;
        this.mTrackerManager = TrackerManager.getInstance(activityWithAuthentication.getApplicationContext());
        this.mAuthenticatorUtils = new AuthenticatorUtils(activityWithAuthentication);

        restoreInstanceState(savedInstanceState);

        this.mActivity.getApplication().registerActivityLifecycleCallbacks(mActivityLifecycleCallback);
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        if(savedInstanceState == null) {
            return;
        }
        mIsAuthenticating = savedInstanceState.getBoolean(PARAM_IS_AUTHENTICATING, false);
    }

    public void saveInstanceState(Bundle outState) {
        outState.putBoolean(PARAM_IS_AUTHENTICATING, mIsAuthenticating);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == AUTH_REQUEST_CODE && mIsAuthenticating) {
            // check authorization result
            if(resultCode == Activity.RESULT_OK) {
                mAuthenticatorUtils.refreshAccount();
                try {
                    AuthenticationData authenticationData = mAuthenticatorUtils.getAccountData();
                    for(AuthenticationDoneCallback cb : mAuthenticationDoneCallbacks) {
                        cb.done(authenticationData);
                    }
                } catch(PeppermintApiNoAccountException e) {
                    mTrackerManager.log("No account after authentication successful! This should never happen!");
                    mTrackerManager.logException(e);

                    if(isRequiresAuthentication()) {
                        Toast.makeText(mActivity, R.string.msg_must_authenticate_using_account, Toast.LENGTH_LONG).show();
                        mActivity.finish();
                    }
                }
            } else if(isRequiresAuthentication()) {
                Toast.makeText(mActivity, R.string.msg_must_authenticate_using_account, Toast.LENGTH_LONG).show();
                mActivity.finish();
            }
            mIsAuthenticating = false;
        }

        return true;
    }

    public AuthenticatorUtils getAuthenticatorUtils() {
        return mAuthenticatorUtils;
    }

    public AuthenticationData getAuthenticationData() {
        // check if the account has changed (or if there's a new account)
        mAuthenticatorUtils.refreshAccount();

        try {
            AuthenticationData data = mAuthenticatorUtils.getAccountData();
            for(AuthenticationDoneCallback cb : mAuthenticationDoneCallbacks) {
                cb.done(data);
            }
            return data;
        } catch(PeppermintApiNoAccountException e) {
            if(!mIsAuthenticating) {
                mIsAuthenticating = true;
                Intent intent = new Intent(mActivity, AuthenticatorActivity.class);
                mActivity.startActivityForResult(intent, AUTH_REQUEST_CODE);
            }
            return null;
        }
    }

    public void addAuthenticationDoneCallback(AuthenticationDoneCallback mAuthenticationDoneCallback) {
        mAuthenticationDoneCallbacks.add(mAuthenticationDoneCallback);
    }

    public boolean removeAuthenticationDoneCallback(AuthenticationDoneCallback authenticationDoneCallback) {
        return mAuthenticationDoneCallbacks.remove(authenticationDoneCallback);
    }

    public boolean isRequiresAuthentication() {
        return mRequiresAuthentication;
    }

    public void setRequiresAuthentication(boolean mRequiresAuthentication) {
        this.mRequiresAuthentication = mRequiresAuthentication;
    }

}
