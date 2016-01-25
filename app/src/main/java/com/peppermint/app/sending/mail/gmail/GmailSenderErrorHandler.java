package com.peppermint.app.sending.mail.gmail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.sending.api.exceptions.GoogleApiInvalidAccessTokenException;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Error handler for the {@link GmailSender}.
 */
public class GmailSenderErrorHandler extends SenderErrorHandler {

    private static final String TAG = GmailSenderErrorHandler.class.getSimpleName();

    private static final int REQUEST_AUTHORIZATION = 998;
    private static final int REQUEST_ACCOUNT_PICKER = 999;

    public GmailSenderErrorHandler(Sender sender, SenderUploadListener senderUploadListener) {
        super(sender, senderUploadListener);
    }

    private void tryToGetNameFromApi(SendingRequest request, GmailSenderPreferences preferences) {
        if(!Utils.isValidName(preferences.getFullName())) {
            Log.d(TAG, "User name is not valid (" + preferences.getFullName() + ") trying Google API...");
            GoogleUserInfoSupportTask task = new GoogleUserInfoSupportTask(getSender(), request, null);
            launchSupportTask(task);
        }
    }

    @Override
    protected void onUploadTaskActivityResult(SenderUploadTask recoveringTask, int requestCode, int resultCode, Intent data) {
        GoogleApi googleApi = getGoogleApi();
        GmailSenderPreferences preferences = (GmailSenderPreferences) getPreferences();

        // the user has picked one of the multiple available google accounts to use...
        if(requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    googleApi.getCredential().setSelectedAccountName(accountName);
                    preferences.setPreferredAccountName(accountName);
                }
            }

            if(preferences.getPreferredAccountName() == null) {
                doNotRecover(recoveringTask);
            } else {
                doRecover(recoveringTask);
            }
            return;
        }

        if(requestCode == REQUEST_AUTHORIZATION) {
            if(handleAuthorizationActivityResult(recoveringTask, resultCode)) {
                doRecover(recoveringTask);
                return;
            }
        }

        doNotRecover(recoveringTask);
    }

    @Override
    protected void onSupportTaskActivityResult(SenderSupportTask supportTask, SenderUploadTask recoveringTask, int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_AUTHORIZATION) {
            if(handleAuthorizationActivityResult(supportTask, resultCode) && recoveringTask != null) {
                doRecover(recoveringTask);
                return;
            }
        }

        if(recoveringTask != null) {
            doNotRecover(recoveringTask);
        }
    }

    private boolean handleAuthorizationActivityResult(SenderTask recoveringTask, int resultCode) {
        // the user has given (or not) permission to use the Gmail API with his/her account

        if(resultCode == Activity.RESULT_OK) {
            tryToGetNameFromApi(recoveringTask.getSendingRequest(), (GmailSenderPreferences) getPreferences());
            return true;
        }

        if(resultCode == Activity.RESULT_CANCELED) {
            getPreferences().setEnabled(false);
            Toast.makeText(getContext(), R.string.sender_msg_cancelled_gmail_api, Toast.LENGTH_LONG).show();
        }

        return false;
    }

    @Override
    protected int tryToRecover(SenderUploadTask failedUploadTask, Throwable error) {

        GoogleApi googleApi = getGoogleApi();
        GmailSenderPreferences preferences = (GmailSenderPreferences) getPreferences();

        // in this case just ask for permissions
        if(error instanceof UserRecoverableAuthIOException || error instanceof UserRecoverableAuthException) {
            Intent intent = error instanceof UserRecoverableAuthIOException ? ((UserRecoverableAuthIOException) error).getIntent() : ((UserRecoverableAuthException) error).getIntent();
            startActivityForResult(failedUploadTask, REQUEST_AUTHORIZATION, intent);
            return RECOVERY_DONOTHING;
        }

        // in this case pick an account from those registered in the Android device
        if(error instanceof MailPreferredAccountNotSetException) {
            Account[] accounts = AccountManager.get(getContext()).getAccountsByType("com.google");
            if(accounts.length <= 0) {
                // no accounts in the device, so just fail
                doNotRecover(failedUploadTask);
                return RECOVERY_NOK;
            }

            if(accounts.length <= 1) {
                // one account in the device, so automatically pick it
                googleApi.getCredential().setSelectedAccountName(accounts[0].name);
                preferences.setPreferredAccountName(accounts[0].name);
                doRecover(failedUploadTask);
                return RECOVERY_OK;
            }

            // multiple accounts in the device - ask the user to pick one
            startActivityForResult(failedUploadTask, REQUEST_ACCOUNT_PICKER, googleApi.getCredential().newChooseAccountIntent());
            return RECOVERY_DONOTHING;
        }

        // in this case, try to ask for another access token
        if(error instanceof GoogleJsonResponseException || error instanceof GoogleApiInvalidAccessTokenException) {
            authorize(failedUploadTask.getSendingRequest());
            return RECOVERY_DONOTHING;
        }

        // NoInternetConnectionException might include small connection issues
        // even if there's internet connection, so allow the retries
        return RECOVERY_RETRY;
    }

    @Override
    protected int supportFinishedOk(SenderSupportTask supportTask) {
        if(supportTask instanceof GoogleAuthorizationSupportTask) {
            tryToGetNameFromApi(supportTask.getSendingRequest(), (GmailSenderPreferences) getPreferences());
        }
        return RECOVERY_OK;
    }

    @Override
    protected int tryToRecoverSupport(SenderSupportTask supportTask, Throwable error) {
        if(error instanceof UserRecoverableAuthIOException || error instanceof UserRecoverableAuthException) {
            Intent intent = error instanceof UserRecoverableAuthIOException ? ((UserRecoverableAuthIOException) error).getIntent() : ((UserRecoverableAuthException) error).getIntent();
            startActivityForResult(supportTask, REQUEST_AUTHORIZATION, intent);
            return RECOVERY_DONOTHING;
        }
        return RECOVERY_RETRY;
    }

    @Override
    public void authorize(SendingRequest sendingRequest) {
        GoogleAuthorizationSupportTask task = new GoogleAuthorizationSupportTask(getSender(), sendingRequest, null);
        launchSupportTask(task);
    }
}
