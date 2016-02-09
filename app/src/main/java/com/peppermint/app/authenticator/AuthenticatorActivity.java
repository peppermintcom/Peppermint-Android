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

package com.peppermint.app.authenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.api.exceptions.GoogleApiDeniedAuthorizationException;
import com.peppermint.app.sending.api.exceptions.GoogleApiNoAuthorizationException;
import com.peppermint.app.sending.api.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.CustomAuthenticatorActivity;
import com.peppermint.app.ui.views.simple.CustomNoScrollListView;

import javax.net.ssl.SSLException;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends CustomAuthenticatorActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    private static final int NEW_ACCOUNT_CODE = 1234;
    private static final int AUTHORIZATION_CODE = 1235;

    private static final String KEY_DO_AUTH = TAG + "_DoAuth";
    private static final String KEY_SEL_ACCOUNT = TAG + "_SelectedAccount";

    private static final String KEY_DEVICE_ID = TAG + "_DeviceId";
    private static final String KEY_DEVICE_KEY = TAG + "_DeviceKey";

    private static final String KEY_PASSWORD = TAG + "_Password";
    private static final String KEY_ACCOUNT_TYPE = TAG + "_AccountType";
    private static final String KEY_DEVICE_SERVER_ID = TAG + "_DeviceServerId";

    // the ID of the screen for the Tracker API
    private static final String SCREEN_ID = "Authentication";

    // general use
    private TrackerManager mTrackerManager;
    private AuthenticatorUtils mAuthenticatorUtils;

    // account list
    private CustomNoScrollListView mListView;
    private AuthenticatorArrayAdapter mAdapter;
    private ViewGroup mLytEmpty;

    private Account[] mAccounts;
    private String mSelectedAccount;

    // buttons
    private Button mBtnAddAccount;

    // authentication
    private ProgressDialog mProgressDialog;

    private AuthenticationPeppermintTask mAuthenticationPeppermintTask;
    private AuthenticationGoogleApiTask mAuthenticationGoogleApiTask;

    private boolean mDoingAuth = false;
    private String mDeviceId, mDeviceKey;
    private String mPassword;
    private int mAccountType = PeppermintApi.ACCOUNT_TYPE_GOOGLE;
    private String mDeviceServerId;

    private final SenderSupportListener mGoogleApiTaskListener = new SenderSupportListener() {
        @Override
        public void onSendingSupportStarted(SenderSupportTask supportTask) {
            mDoingAuth = true;
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
            finishAuthentication(task.getAccessToken());
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
            startActivityForResult(intent, AUTHORIZATION_CODE);
            return;
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

    private void finishAuthentication(String accessToken) {
        mAuthenticatorUtils.createAccount(accessToken, mSelectedAccount, mPassword, mDeviceServerId, mDeviceId, mDeviceKey, mAccountType);

        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, AuthenticatorConstants.ACCOUNT_NAME);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AuthenticatorConstants.ACCOUNT_TYPE);
        intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, true);
        setAccountAuthenticatorResult(intent.getExtras());

        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // do not enforce an authentication policy for the authenticator activity
        getAuthenticationPolicyEnforcer().setRequiresAuthentication(false);

        FrameLayout lytContainer = (FrameLayout) findViewById(R.id.container);
        View v = getLayoutInflater().inflate(R.layout.f_authentication, null, false);
        lytContainer.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mAuthenticatorUtils = new AuthenticatorUtils(this);
        mTrackerManager = TrackerManager.getInstance(getApplicationContext());

        // inflate custom action bar
        v = getLayoutInflater().inflate(R.layout.v_authentication_actionbar, null, false);
        getCustomActionBar().setContents(v, true);

        // disable back/menu button
        getCustomActionBar().getMenuButton().setVisibility(View.GONE);

        // init layout components
        mBtnAddAccount = (Button) findViewById(R.id.btnAddAccount);
        mBtnAddAccount.setOnClickListener(this);

        mListView = (CustomNoScrollListView) findViewById(android.R.id.list);
        mLytEmpty = (ViewGroup) findViewById(android.R.id.empty);

        mListView.setOnItemClickListener(this);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getText(R.string.authenticating_));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
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
            mDoingAuth = savedInstanceState.getBoolean(KEY_DO_AUTH);
        }

        if(mDoingAuth) {
            startGoogleAuthentication();
        }
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
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // although this code is also at onResume() and is executed
        // only after adding it here the list is properly refreshed with the new account
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

        mTrackerManager.trackScreenView(SCREEN_ID);
    }

    @Override
    public void onClick(View v) {
        if(v.equals(mBtnAddAccount)) {
            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {"com.google"});
            startActivityForResult(intent, NEW_ACCOUNT_CODE);
            return;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSelectedAccount = mAccounts[position].name;
        mListView.setItemChecked(position, true);
        startGoogleAuthentication();
    }

    /**
     * Shows the progress UI for a lengthy operation.
     */
    private void showProgress() {
        if(mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    /**
     * Hides the progress UI for a lengthy operation.
     */
    private void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

}
