/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.peppermint.app.ui.authentication;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.peppermint.app.R;
import com.peppermint.app.cloud.apis.google.GoogleApiDeniedAuthorizationException;
import com.peppermint.app.cloud.apis.google.GoogleApiNoAuthorizationException;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApi;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.services.authenticator.AuthenticatorConstants;
import com.peppermint.app.services.authenticator.AuthenticatorUtils;
import com.peppermint.app.services.messenger.MessengerServiceManager;
import com.peppermint.app.services.messenger.handlers.NoInternetConnectionException;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.services.messenger.handlers.SenderSupportListener;
import com.peppermint.app.services.messenger.handlers.SenderSupportTask;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.PermissionsPolicyEnforcer;
import com.peppermint.app.ui.base.activities.CustomAuthenticatorActivity;
import com.peppermint.app.ui.base.views.CustomActionBarView;
import com.peppermint.app.ui.base.views.CustomFontTextView;
import com.peppermint.app.ui.base.views.CustomNoScrollListView;
import com.peppermint.app.utils.Utils;

import javax.net.ssl.SSLException;

/**
 * Activity which displays the sign in screen to the user.
 */
public class AuthenticatorActivity extends CustomAuthenticatorActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();
    private static final int PLAY_SERVICES_RESOLUTION_CODE = 9000;

    private static final int NEW_ACCOUNT_CODE = 1234;
    private static final int AUTHORIZATION_CODE = 1235;

    public static final String PARAM_FORWARD_TO = TAG + "_forwardToIntent";

    private static final String KEY_DO_AUTH = TAG + "_DoAuth";
    private static final String KEY_SEL_ACCOUNT = TAG + "_SelectedAccount";

    private static final String KEY_DEVICE_ID = TAG + "_DeviceId";
    private static final String KEY_DEVICE_KEY = TAG + "_DeviceKey";

    private static final String KEY_PASSWORD = TAG + "_Password";
    private static final String KEY_ACCOUNT_TYPE = TAG + "_AccountType";
    private static final String KEY_DEVICE_SERVER_ID = TAG + "_DeviceServerId";
    private static final String KEY_ACCOUNT_SERVER_ID = TAG + "_AccountServerId";

    // the ID of the screen for the Tracker API
    @Override
    protected final String getTrackerLabel() {
        return "Authentication";
    }

    // general use
    private TrackerManager mTrackerManager;
    private AuthenticatorUtils mAuthenticatorUtils;
    private SenderPreferences mPreferences;

    // account list
    private CustomNoScrollListView mListView;
    private AuthenticatorArrayAdapter mAdapter;
    private ViewGroup mLytEmpty, mLytBottom, lytContactUs;

    private Account[] mAccounts;
    private String mSelectedAccount;

    private Button mBtnSignIn;
    private ProgressDialog mProgressDialog;

    // authentication process tasks
    private AuthenticationPeppermintTask mAuthenticationPeppermintTask;
    private AuthenticationGoogleApiTask mAuthenticationGoogleApiTask;

    // authentication process status variables
    private boolean mDoingAuth = false;
    private String mDeviceId, mDeviceKey;
    private String mPassword;
    private int mAccountType = PeppermintApi.ACCOUNT_TYPE_GOOGLE;
    private String mDeviceServerId, mAccountServerId;

    private final SenderSupportListener mGoogleApiTaskListener = new SenderSupportListener() {
        @Override
        public void onSendingSupportStarted(SenderSupportTask supportTask) {
            mDoingAuth = true;
            mProgressDialog.setMessage(getString(R.string.authenticating_));
        }

        @Override
        public void onSendingSupportCancelled(SenderSupportTask supportTask) {
            mDoingAuth = false;
            hideProgress();
        }

        @Override
        public void onSendingSupportFinished(SenderSupportTask supportTask) {
            AuthenticationGoogleApiTask task = ((AuthenticationGoogleApiTask) supportTask);
            mAccountType = PeppermintApi.ACCOUNT_TYPE_GOOGLE;
            mPassword = task.getGoogleToken();
            startPeppermintAuthentication();
        }

        @Override
        public void onSendingSupportError(SenderSupportTask supportTask, Throwable error) {
            mDoingAuth = false;
            handleError(error);
        }

        @Override
        public void onSendingSupportProgress(SenderSupportTask supportTask, float progressValue) { }
    };

    private final SenderSupportListener mPeppermintTaskListener = new SenderSupportListener() {
        @Override
        public void onSendingSupportStarted(SenderSupportTask supportTask) {
            mDoingAuth = true;
            mProgressDialog.setMessage(getString(R.string.authenticating_));
        }

        @Override
        public void onSendingSupportCancelled(SenderSupportTask supportTask) {
            mDoingAuth = false;
            hideProgress();
        }

        @Override
        public void onSendingSupportFinished(SenderSupportTask supportTask) {
            AuthenticationPeppermintTask task = (AuthenticationPeppermintTask) supportTask;
            mDeviceServerId = task.getDeviceServerId();
            mAccountServerId = task.getAccountServerId();
            mAuthenticatorUtils.createAccount(task.getAccessToken(), mSelectedAccount, mAccountServerId, mPassword, mDeviceServerId, mDeviceId, mDeviceKey, mAccountType);

            try {
                mAuthenticatorUtils.requestSync();
            } catch (Exception e) {
                Log.e(TAG, "Error requesting synchronization...", e);
            }

            finishAuthentication();
            mDoingAuth = false;
            hideProgress();
        }

        @Override
        public void onSendingSupportError(SenderSupportTask supportTask, Throwable error) {
            mDoingAuth = false;
            handleError(error);
        }

        @Override
        public void onSendingSupportProgress(SenderSupportTask supportTask, float progressValue) { }
    };

    private void startGoogleAuthentication() {
        showProgress();
        mAuthenticationGoogleApiTask = new AuthenticationGoogleApiTask(this, mSelectedAccount, mGoogleApiTaskListener);
        mAuthenticationGoogleApiTask.getIdentity().setTrackerManager(mTrackerManager);
        mAuthenticationGoogleApiTask.getIdentity().setPreferences(mPreferences);
        mAuthenticationGoogleApiTask.execute((Void) null);
    }

    private void startPeppermintAuthentication() {
        showProgress();
        String[] keys = PeppermintApi.getKeys(this);
        mDeviceId = keys[0];
        mDeviceKey = keys[1];

        mAuthenticationPeppermintTask = new AuthenticationPeppermintTask(this, mAccountType, mDeviceId, mDeviceKey, mSelectedAccount, mPassword, mPeppermintTaskListener);
        mAuthenticationPeppermintTask.getIdentity().setTrackerManager(mTrackerManager);
        mAuthenticationPeppermintTask.getIdentity().setPreferences(mPreferences);
        mAuthenticationPeppermintTask.execute((Void) null);
    }

    private void handleError(Throwable error) {

        if(error instanceof GoogleApiNoAuthorizationException) {
            Intent intent = ((GoogleApiNoAuthorizationException) error).getHandleIntent();
            try {
                startActivityForResult(intent, AUTHORIZATION_CODE);
                return;
            } catch(ActivityNotFoundException e) {
                Toast.makeText(this, R.string.msg_no_gplay, Toast.LENGTH_LONG).show();
            }
        }

        hideProgress();

        if(error instanceof GoogleApiDeniedAuthorizationException) {
            Toast.makeText(AuthenticatorActivity.this, R.string.msg_google_api_denied, Toast.LENGTH_LONG).show();
            return;
        }

        if(error instanceof NoInternetConnectionException) {
            Toast.makeText(AuthenticatorActivity.this, R.string.msg_no_internet_try_again, Toast.LENGTH_LONG).show();
            return;
        }

        if(error.getCause() != null && error.getCause() instanceof SSLException) {
            Toast.makeText(AuthenticatorActivity.this, R.string.msg_secure_connection, Toast.LENGTH_LONG).show();
            return;
        }

        if(error instanceof PeppermintApiInvalidAccessTokenException) {
            Toast.makeText(AuthenticatorActivity.this, R.string.msg_invalid_credentials, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(AuthenticatorActivity.this, R.string.msg_authentication_error, Toast.LENGTH_LONG).show();
        mTrackerManager.logException(error);
    }

    private void finishAuthentication() {
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, AuthenticatorConstants.ACCOUNT_NAME);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AuthenticatorConstants.ACCOUNT_TYPE);
        intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, true);
        setAccountAuthenticatorResult(intent.getExtras());

        setResult(RESULT_OK, intent);
        finish();

        // start the service so that we can receive GCM notifications
        MessengerServiceManager messengerServiceManager = new MessengerServiceManager(this);
        messengerServiceManager.start();

        if(getIntent() != null) {
            Intent activityIntent = getIntent().getParcelableExtra(PARAM_FORWARD_TO);
            if(activityIntent != null) {
                startActivity(activityIntent);
            }
        }
    }

    @Override
    protected void onSetupPermissions(PermissionsPolicyEnforcer permissionsPolicyEnforcer) {
        super.onSetupPermissions(permissionsPolicyEnforcer);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.INTERNET, false);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.ACCESS_NETWORK_STATE, false);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.GET_ACCOUNTS, false);
        permissionsPolicyEnforcer.addPermission("android.permission.USE_CREDENTIALS", false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = new SenderPreferences(this);

        FrameLayout lytContainer = (FrameLayout) findViewById(R.id.container);
        View v = getLayoutInflater().inflate(R.layout.f_authentication, null, false);
        lytContainer.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mAuthenticatorUtils = new AuthenticatorUtils(this);
        mTrackerManager = TrackerManager.getInstance(getApplicationContext());

        // inflate custom action bar
        final CustomActionBarView actionBarView = getCustomActionBar();
        if(actionBarView != null) {
            actionBarView.setTitle(getString(R.string.peppermint_sign_in));
            // disable back/menu button
            actionBarView.getMenuButton().setVisibility(View.GONE);
        }

        // init layout components
        mBtnSignIn = (Button) findViewById(R.id.btnSignIn);
        mBtnSignIn.setOnClickListener(this);

        mListView = (CustomNoScrollListView) findViewById(android.R.id.list);
        mLytEmpty = (ViewGroup) findViewById(android.R.id.empty);
        mLytBottom = (ViewGroup) findViewById(R.id.lytBottom);
        lytContactUs = (ViewGroup) findViewById(R.id.lytContactUs);
        mLytBottom.setOnClickListener(this);
        lytContactUs.setOnClickListener(this);

        CustomFontTextView txtAddGoogleAccount = (CustomFontTextView) findViewById(R.id.txtAddGoogleAccount);
        txtAddGoogleAccount.setOnClickListener(this);

        mListView.setOnItemClickListener(this);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getText(R.string.authenticating_));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mDoingAuth = false;
                if (mAuthenticationPeppermintTask != null) {
                    mAuthenticationPeppermintTask.cancel(true);
                }
                if (mAuthenticationGoogleApiTask != null) {
                    mAuthenticationGoogleApiTask.cancel(true);
                }
            }
        });

        if(savedInstanceState != null) {
            mSelectedAccount = savedInstanceState.getString(KEY_SEL_ACCOUNT);
            mDeviceKey = savedInstanceState.getString(KEY_DEVICE_KEY);
            mDeviceId = savedInstanceState.getString(KEY_DEVICE_ID);
            mPassword = savedInstanceState.getString(KEY_PASSWORD);
            mAccountType = savedInstanceState.getInt(KEY_ACCOUNT_TYPE, PeppermintApi.ACCOUNT_TYPE_GOOGLE);
            mDeviceServerId = savedInstanceState.getString(KEY_DEVICE_SERVER_ID);
            mAccountServerId = savedInstanceState.getString(KEY_ACCOUNT_SERVER_ID);
            mDoingAuth = savedInstanceState.getBoolean(KEY_DO_AUTH);
        }

        if(mDoingAuth) {
            startGoogleAuthentication();
        }

        checkPlayServices();
    }

    @Override
    protected void onDestroy() {
        hideProgress();

        if(mAuthenticationPeppermintTask != null) {
            mAuthenticationPeppermintTask.cancel(true);
        }
        if(mAuthenticationGoogleApiTask != null) {
            mAuthenticationGoogleApiTask.cancel(true);
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // only return RESULT_OK if successful (check AuthFragment)
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        hideProgress();
        outState.putString(KEY_SEL_ACCOUNT, mSelectedAccount);
        outState.putString(KEY_DEVICE_ID, mDeviceId);
        outState.putString(KEY_DEVICE_KEY, mDeviceKey);
        outState.putString(KEY_PASSWORD, mPassword);
        outState.putInt(KEY_ACCOUNT_TYPE, mAccountType);
        outState.putString(KEY_DEVICE_SERVER_ID, mDeviceServerId);
        outState.putString(KEY_ACCOUNT_SERVER_ID, mAccountServerId);
        outState.putBoolean(KEY_DO_AUTH, mDoingAuth);
        super.onSaveInstanceState(outState);
    }

    private void refreshAccountList() {
        mAccounts = AccountManager.get(this).getAccountsByType("com.google");
        mAdapter = new AuthenticatorArrayAdapter(this, mAccounts);
        mListView.setAdapter(mAdapter);

        if(mAccounts != null && mAccounts.length > 0) {
            mListView.setVisibility(View.VISIBLE);
            mLytEmpty.setVisibility(View.GONE);
            mLytBottom.setVisibility(View.VISIBLE);
            if(mAccounts.length > 1) {
                mBtnSignIn.setVisibility(View.GONE);
            } else {
                mBtnSignIn.setVisibility(View.VISIBLE);
            }
            int pos = 0;

            if(mSelectedAccount != null) {
                pos = -1;
                for(int i=0; i<mAccounts.length && pos < 0; i++) {
                    if(mAccounts[i].name.compareTo(mSelectedAccount) == 0) {
                        pos = i;
                    }
                }
                if(pos < 0) {
                    pos = 0;
                }
            }

            mSelectedAccount = mAccounts[pos].name;
            mListView.setItemChecked(pos, true);
        } else {
            mListView.setVisibility(View.GONE);
            mLytEmpty.setVisibility(View.VISIBLE);
            mBtnSignIn.setVisibility(View.GONE);
            mLytBottom.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == NEW_ACCOUNT_CODE && resultCode == Activity.RESULT_OK) {
            refreshAccountList();
            return;
        }

        // authorization from Google API
        if(requestCode == AUTHORIZATION_CODE) {
            hideProgress();
            if(resultCode != RESULT_OK) {
                handleError(new GoogleApiDeniedAuthorizationException());
            } else {
                startGoogleAuthentication();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAccountList();
        if((mAuthenticationPeppermintTask != null && mAuthenticationPeppermintTask.getStatus() != AsyncTask.Status.FINISHED) ||
                (mAuthenticationGoogleApiTask != null && mAuthenticationGoogleApiTask.getStatus() != AsyncTask.Status.FINISHED)) {
            showProgress();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideProgress();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btnSignIn) {
            mSelectedAccount = mAccounts[0].name;
            startGoogleAuthentication();
            return;
        }

        if(v.getId() == R.id.lytBottom || v.getId() == R.id.txtAddGoogleAccount) {
            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {"com.google"});
            startActivityForResult(intent, NEW_ACCOUNT_CODE);
            return;
        }

        if(v.getId() == R.id.lytContactUs) {
            Utils.triggerSupportEmail(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSelectedAccount = mAccounts[position].name;
        mListView.setItemChecked(position, true);
        startGoogleAuthentication();
    }

    private void showProgress() {
        if(mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    private void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_CODE)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
