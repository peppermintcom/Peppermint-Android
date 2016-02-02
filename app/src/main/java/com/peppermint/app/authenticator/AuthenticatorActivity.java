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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.peppermint.app.R;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.api.exceptions.GoogleApiDeniedAuthorizationException;
import com.peppermint.app.sending.api.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.CustomAuthenticatorActivity;
import com.peppermint.app.ui.authentication.AuthActivity;
import com.peppermint.app.ui.views.simple.CustomNoScrollListView;
import com.peppermint.app.ui.views.simple.CustomValidatedEditText;
import com.peppermint.app.utils.Utils;

import javax.net.ssl.SSLException;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends CustomAuthenticatorActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    /**
     * Checks if authentication is required and takes the necessary steps to launch the {@link AuthActivity}.<br />
     * It can also request authorization from all sender (check {@link com.peppermint.app.sending.Sender} and
     * {@link com.peppermint.app.sending.SenderManager}) API implementations.
     *
     * @param callerActivity the caller activity
     * @param requestCode the request code
     * @param authorize if it should request authorization from all senders
     * @return true if authentication screen was launched; false otherwise
     */
    public static boolean startAuthentication(Activity callerActivity, int requestCode, boolean authorize) {
        SenderPreferences prefs = new SenderPreferences(callerActivity);

        // 2a. check if there's already a preferred account
        if (prefs.getGmailSenderPreferences().getPreferredAccountName() != null) {
            TrackerManager.getInstance(callerActivity.getApplicationContext()).setUserEmail(prefs.getGmailSenderPreferences().getPreferredAccountName());

            if(authorize) {
                // 3a. (optional) authorize the Gmail API and all other necessary apis
                SenderServiceManager senderManager = new SenderServiceManager(callerActivity.getApplicationContext());
                senderManager.startAndAuthorize();
            }

            return false;
        }

        // 2b. otherwise check if there's only one account and set that one as the preferred
        Account[] accounts = AccountManager.get(callerActivity).getAccountsByType("com.google");
        if (accounts.length == 1) {
            prefs.getGmailSenderPreferences().setPreferredAccountName(accounts[0].name);

            TrackerManager.getInstance(callerActivity.getApplicationContext()).setUserEmail(prefs.getGmailSenderPreferences().getPreferredAccountName());

            if(authorize) {
                // 3b. (optional) authorize the Gmail API and all other necessary apis
                SenderServiceManager senderManager = new SenderServiceManager(callerActivity.getApplicationContext());
                senderManager.startAndAuthorize();
            }

            return false;
        }

        // just show the auth screen
        Intent intent = new Intent(callerActivity, AuthActivity.class);
        callerActivity.startActivityForResult(intent, requestCode);
        return true;
    }

    private static final int NEW_ACCOUNT_CODE = 1234;
    private static final int AUTHORIZATION_CODE = 1235;

    private static final String KEY_FIRST_NAME = TAG + "_FirstName";
    private static final String KEY_LAST_NAME = TAG + "_LastName";
    private static final String KEY_SEL_ACCOUNT = TAG + "_SelectedAccount";

    private static final String KEY_DEVICE_ID = TAG + "_DeviceId";
    private static final String KEY_DEVICE_KEY = TAG + "_DeviceKey";

    private static final String KEY_PASSWORD = TAG + "_Password";
    private static final String KEY_ACCOUNT_TYPE = TAG + "_AccountType";
    private static final String KEY_GOOGLE_TOKEN = TAG + "_GoogleToken";

    // the ID of the screen for the Tracker API
    private static final String SCREEN_ID = "Authentication";

    // general use
    private TrackerManager mTrackerManager;
    private SenderPreferences mPreferences;
    private GoogleApi mGoogleApi;
    private PeppermintApi mPeppermintApi;
    private AuthenticatorUtils mAuthenticatorUtils;

    // names
    private CustomValidatedEditText mTxtFirstName, mTxtLastName;
    private boolean mDontSetNameFromPrefs = false;

    private CustomValidatedEditText.Validator mFirstNameValidator = new CustomValidatedEditText.Validator() {
        @Override
        public String getValidatorMessage(CharSequence text) {
            String name = text.toString().trim();
            if(!Utils.isValidName(name)) {
                return getString(R.string.msg_insert_first_name);
            }
            mValidityChecker.areValid();
            return null;
        }
    };

    private CustomValidatedEditText.Validator mLastNameValidator = new CustomValidatedEditText.Validator() {
        @Override
        public String getValidatorMessage(CharSequence text) {
            String name = text.toString().trim();
            if(!Utils.isValidNameMaybeEmpty(name)) {
                return getString(R.string.msg_insert_last_name);
            }
            return null;
        }
    };

    private CustomValidatedEditText.OnValidityChangeListener mValidityChangeListener = new CustomValidatedEditText.OnValidityChangeListener() {
        @Override
        public void onValidityChange(boolean isValid) {
            mValidityChecker.areValid();
        }
    };

    private CustomValidatedEditText.ValidityChecker mValidityChecker;

    // account list
    private CustomNoScrollListView mListView;
    private AuthenticatorArrayAdapter mAdapter;
    private ViewGroup mLytEmpty;

    private Account[] mAccounts;
    private String mSelectedAccount;

    // buttons
    private Button mBtnNext, mBtnAddAccount;

    // authentication
    private ProgressDialog mProgressDialog = null;

    private AuthenticationPeppermintTask mAuthenticationPeppermintTask;
    private AuthenticationGoogleApiTask mAuthenticationGoogleApiTask;

    private String mDeviceId, mDeviceKey;
    private String mFirstName, mLastName, mPassword;
    private int mAccountType = PeppermintApi.ACCOUNT_TYPE_GOOGLE;
    private String mGoogleToken;

    private final SenderSupportListener mGoogleApiTaskListener = new SenderSupportListener() {
        @Override
        public void onSendingSupportStarted(SenderSupportTask supportTask) {
        }

        @Override
        public void onSendingSupportCancelled(SenderSupportTask supportTask) {
            hideProgress();
        }

        @Override
        public void onSendingSupportFinished(SenderSupportTask supportTask) {
            mGoogleToken = ((AuthenticationGoogleApiTask) supportTask).getGoogleToken();
            startPeppermintAuthentication();
        }

        @Override
        public void onSendingSupportError(SenderSupportTask supportTask, Throwable error) {
            handleError(error);
        }

        @Override
        public void onSendingSupportProgress(SenderSupportTask supportTask, float progressValue) { }
    };

    private final SenderSupportListener mPeppermintTaskListener = new SenderSupportListener() {
        @Override
        public void onSendingSupportStarted(SenderSupportTask supportTask) {
        }

        @Override
        public void onSendingSupportCancelled(SenderSupportTask supportTask) {
            hideProgress();
        }

        @Override
        public void onSendingSupportFinished(SenderSupportTask supportTask) {
            AuthenticationPeppermintTask task = (AuthenticationPeppermintTask) supportTask;
            hideProgress();
            finishAuthentication(task.getAccessToken());
        }

        @Override
        public void onSendingSupportError(SenderSupportTask supportTask, Throwable error) {
            handleError(error);
        }

        @Override
        public void onSendingSupportProgress(SenderSupportTask supportTask, float progressValue) { }
    };

    private void startGoogleAuthentication() {
        showProgress();
        mGoogleApi.setAccountName(mSelectedAccount);
        mAuthenticationGoogleApiTask = new AuthenticationGoogleApiTask(this, mGoogleApi, mGoogleApiTaskListener);
        mAuthenticationGoogleApiTask.getIdentity().setTrackerManager(mTrackerManager);
        mAuthenticationGoogleApiTask.execute((Void) null);
    }

    private void startPeppermintAuthentication() {
        showProgress();
        mFirstName = Utils.capitalizeFully(mTxtFirstName.getText().toString());
        mLastName = Utils.capitalizeFully(mTxtLastName.getText().toString());
        String[] keys = PeppermintApi.getKeys(this);
        mDeviceId = keys[0];
        mDeviceKey = keys[1];
        mPassword = mDeviceId + "-" + mDeviceKey;
        mAccountType = PeppermintApi.ACCOUNT_TYPE_GOOGLE;

        mAuthenticationPeppermintTask = new AuthenticationPeppermintTask(this, mPeppermintApi, mAccountType, mDeviceId, mDeviceKey, mSelectedAccount, mPassword, mFirstName, mLastName, mPeppermintTaskListener);
        mAuthenticationPeppermintTask.getIdentity().setTrackerManager(mTrackerManager);
        mAuthenticationPeppermintTask.execute((Void) null);
    }

    private void handleError(Throwable error) {

        if(error instanceof UserRecoverableAuthIOException || error instanceof UserRecoverableAuthException) {
            Intent intent = error instanceof UserRecoverableAuthIOException ? ((UserRecoverableAuthIOException) error).getIntent() : ((UserRecoverableAuthException) error).getIntent();
            startActivityForResult(intent, AUTHORIZATION_CODE);
            return;
        }

        hideProgress();

        if(error instanceof GoogleApiDeniedAuthorizationException) {
            Toast.makeText(AuthenticatorActivity.this, R.string.msg_google_api_denied, Toast.LENGTH_LONG).show();
        }

        if(error instanceof NoInternetConnectionException) {
            Toast.makeText(AuthenticatorActivity.this, R.string.sender_msg_no_internet, Toast.LENGTH_LONG).show();
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
        mAuthenticatorUtils.createAccount(accessToken, mSelectedAccount, mPassword, mDeviceId, mDeviceKey, mFirstName, mLastName, mAccountType);

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

        FrameLayout lytContainer = (FrameLayout) findViewById(R.id.container);
        View v = getLayoutInflater().inflate(R.layout.f_authentication, null, false);
        lytContainer.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mPreferences = new SenderPreferences(this);
        mGoogleApi = new GoogleApi(this);
        mPeppermintApi = new PeppermintApi();
        mAuthenticatorUtils = new AuthenticatorUtils(this);
        mTrackerManager = TrackerManager.getInstance(getApplicationContext());

        // inflate custom action bar
        v = getLayoutInflater().inflate(R.layout.v_authentication_actionbar, null, false);
        getCustomActionBar().setContents(v, true);

        // disable back/menu button
        getCustomActionBar().getMenuButton().setVisibility(View.GONE);

        // init layout components
        // global touch interceptor to hide keyboard
        getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Rect outRectFirst = new Rect();
                    Rect outRectLast = new Rect();
                    mTxtFirstName.getGlobalVisibleRect(outRectFirst);
                    mTxtLastName.getGlobalVisibleRect(outRectLast);
                    if (!outRectFirst.contains((int) event.getRawX(), (int) event.getRawY()) && !outRectLast.contains((int) event.getRawX(), (int) event.getRawY())) {
                        Utils.hideKeyboard(AuthenticatorActivity.this, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                    }
                }
                return false;
            }
        });

        mBtnNext = (Button) findViewById(R.id.btnNext);
        mBtnNext.setOnClickListener(this);

        mBtnAddAccount = (Button) findViewById(R.id.btnAddAccount);
        mBtnAddAccount.setOnClickListener(this);

        mTxtFirstName = (CustomValidatedEditText) findViewById(R.id.txtFirstName);
        mTxtFirstName.setValidator(mFirstNameValidator);
        mTxtFirstName.setOnValidityChangeListener(mValidityChangeListener);
        mTxtFirstName.setValidBackgroundResource(R.drawable.background_edittext_simple);
        mTxtFirstName.setInvalidBackgroundResource(R.drawable.ic_edittext_invalid);

        mTxtLastName = (CustomValidatedEditText) findViewById(R.id.txtLastName);
        mTxtLastName.setOnValidityChangeListener(mValidityChangeListener);
        mTxtLastName.setValidator(mLastNameValidator);
        mTxtLastName.setValidBackgroundResource(R.drawable.background_edittext_simple);
        mTxtLastName.setInvalidBackgroundResource(R.drawable.ic_edittext_invalid);

        mValidityChecker = new CustomValidatedEditText.ValidityChecker(mTxtFirstName, mTxtLastName) {
            @Override
            public synchronized boolean areValid() {
                if(mAccounts == null || mAccounts.length <= 0 || mListView.getCheckedItemPosition() < 0) {
                    mBtnAddAccount.setTextColor(Utils.getColorStateList(AuthenticatorActivity.this, R.color.color_orange_to_white_pressed));
                    mBtnNext.setEnabled(false);
                    return false;
                }

                mBtnAddAccount.setTextColor(Utils.getColorStateList(AuthenticatorActivity.this, R.color.color_green_to_white_pressed));
                boolean superValid = super.areValid();
                mBtnNext.setEnabled(superValid);

                return superValid;
            }
        };

        mListView = (CustomNoScrollListView) findViewById(android.R.id.list);
        mLytEmpty = (ViewGroup) findViewById(android.R.id.empty);

        mListView.setOnItemClickListener(this);

        if(savedInstanceState != null) {
            mFirstName = savedInstanceState.getString(KEY_FIRST_NAME);
            mLastName = savedInstanceState.getString(KEY_LAST_NAME);
            mSelectedAccount = savedInstanceState.getString(KEY_SEL_ACCOUNT);
            mDeviceKey = savedInstanceState.getString(KEY_DEVICE_KEY);
            mDeviceId = savedInstanceState.getString(KEY_DEVICE_ID);
            mPassword = savedInstanceState.getString(KEY_PASSWORD);
            mAccountType = savedInstanceState.getInt(KEY_ACCOUNT_TYPE, PeppermintApi.ACCOUNT_TYPE_GOOGLE);
            mGoogleToken = savedInstanceState.getString(KEY_GOOGLE_TOKEN);
        }

        if(mFirstName != null || mLastName != null) {
            if(mFirstName != null) {
                mTxtFirstName.setText(mFirstName);
            }
            if(mLastName != null) {
                mTxtLastName.setText(mLastName);
            }
            // only try to get the name from prefs if there's no saved instance state
            mDontSetNameFromPrefs = true;
        } else {
            mDontSetNameFromPrefs = false;
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
        outState.putString(KEY_FIRST_NAME, mTxtFirstName.getText().toString());
        outState.putString(KEY_LAST_NAME, mTxtLastName.getText().toString());
        outState.putString(KEY_SEL_ACCOUNT, mSelectedAccount);
        outState.putString(KEY_DEVICE_ID, mDeviceId);
        outState.putString(KEY_DEVICE_KEY, mDeviceKey);
        outState.putString(KEY_PASSWORD, mPassword);
        outState.putInt(KEY_ACCOUNT_TYPE, mAccountType);
        outState.putString(KEY_GOOGLE_TOKEN, mGoogleToken);
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
                startPeppermintAuthentication();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAccountList();

        // only try to get the name from prefs if there's no saved instance state
        if(!mDontSetNameFromPrefs) {
            String firstName = mPreferences.getFirstName();
            String lastName = mPreferences.getLastName();

            if(firstName != null && Utils.isValidName(firstName)) {
                mTxtFirstName.setText(firstName);
            }
            if(lastName != null && Utils.isValidName(lastName)) {
                mTxtLastName.setText(lastName);
            }

            mDontSetNameFromPrefs = true;
        }
        mTxtFirstName.setSelection(mTxtFirstName.getText().length());
        mTxtLastName.setSelection(mTxtLastName.getText().length());

        mTrackerManager.trackScreenView(SCREEN_ID);

        mTxtFirstName.validate();
        mTxtLastName.validate();
    }

    @Override
    public void onClick(View v) {
        if(v.equals(mBtnAddAccount)) {
            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {"com.google"});
            startActivityForResult(intent, NEW_ACCOUNT_CODE);
            return;
        }

        if(v.equals(mBtnNext)) {
            if(!mValidityChecker.areValid()) {
                return;
            }

            startGoogleAuthentication();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSelectedAccount = mAccounts[position].name;
        mListView.setItemChecked(position, true);
        mValidityChecker.areValid();
    }

    /*
     * {@inheritDoc}
     */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getText(R.string.authenticating_));
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.i(TAG, "user cancelling authentication");
                if(mAuthenticationPeppermintTask != null) {
                    mAuthenticationPeppermintTask.cancel(true);
                }
                if(mAuthenticationGoogleApiTask != null) {
                    mAuthenticationGoogleApiTask.cancel(true);
                }
            }
        });
        // We save off the progress dialog in a field so that we can dismiss
        // it later. We can't just call dismissDialog(0) because the system
        // can lose track of our dialog if there's an orientation change.
        mProgressDialog = dialog;
        return dialog;
    }

    /**
     * Shows the progress UI for a lengthy operation.
     */
    private void showProgress() {
        showDialog(0);
    }

    /**
     * Hides the progress UI for a lengthy operation.
     */
    private void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
}
