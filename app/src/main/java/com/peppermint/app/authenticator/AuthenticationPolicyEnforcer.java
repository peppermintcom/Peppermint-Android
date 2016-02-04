package com.peppermint.app.authenticator;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.sending.api.exceptions.PeppermintApiNoAccountException;
import com.peppermint.app.tracking.TrackerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 04-02-2016.
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

    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallback = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if(activity.equals(mActivity)) {
                Log.d(TAG, "onActivityResumed()");
                if (isRequiresAuthentication()) {
                    getAuthenticationData();
                }
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            if(activity.equals(mActivity)) {
                Log.d(TAG, "onActivitySaveInstanceState()");
                saveInstanceState(outState);
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if(activity.equals(mActivity)) {
                Log.d(TAG, "onActivityDestroyed()");
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
        Log.d(TAG, "getAuthenticationData()");
        mAuthenticatorUtils.refreshAccount();
        try {
            AuthenticationData data = mAuthenticatorUtils.getAccountData();
            for(AuthenticationDoneCallback cb : mAuthenticationDoneCallbacks) {
                cb.done(data);
            }
            return data;
        } catch(PeppermintApiNoAccountException e) {
            Log.d(TAG, "No account!");
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
