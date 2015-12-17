package com.peppermint.app.sending.mail.gmail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.peppermint.app.R;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;
import com.peppermint.app.sending.server.InvalidAccessTokenException;
import com.peppermint.app.tracking.TrackerManager;

import java.util.Map;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Error handler for the {@link GmailSender}.
 */
public class GmailSenderErrorHandler extends SenderErrorHandler {

    private static final String TAG = GmailSenderErrorHandler.class.getSimpleName();

    private static final int REQUEST_AUTHORIZATION = 998;
    private static final int REQUEST_ACCOUNT_PICKER = 999;

    private class GetTokenAsyncTask extends AsyncTask<GoogleAccountCredential, Void, Throwable> {
        @Override
        protected Throwable doInBackground(GoogleAccountCredential... params) {
            try {
                GoogleAuthUtil.clearToken(getContext(), params[0].getToken());
                return null;
            } catch(Throwable e) {
                return e;
            }
        }
    }

    public GmailSenderErrorHandler(Context context, SenderListener senderListener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(context, senderListener, parameters, preferences);
    }

    @Override
    protected void onActivityResult(SenderTask recoveredTask, int requestCode, int resultCode, Intent data) {
        GoogleAccountCredential credential = (GoogleAccountCredential) getParameter(GmailSender.PARAM_GMAIL_CREDENTIAL);
        GmailSenderPreferences preferences = (GmailSenderPreferences) getSenderPreferences();

        // the user has picked one of the multiple available google accounts to use...
        if(requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    credential.setSelectedAccountName(accountName);
                    preferences.setPreferredAccountName(accountName);
                }
            }

            if(preferences.getPreferredAccountName() == null) {
                doNotRecover(recoveredTask);
            } else {
                doRecover(recoveredTask);
            }
            return;
        }

        // the user has given (or not) permission to use the Gmail API with his/her account
        if(requestCode == REQUEST_AUTHORIZATION){
            if(resultCode == Activity.RESULT_OK) {
                doRecover(recoveredTask);
                return;
            } else if(resultCode == Activity.RESULT_CANCELED) {
                preferences.setEnabled(false);
                Toast.makeText(getContext(), R.string.msg_cancelled_gmail_api, Toast.LENGTH_LONG).show();
            }
        }

        doNotRecover(recoveredTask);
    }

    @Override
    public void tryToRecover(SenderTask failedSendingTask) {
        super.tryToRecover(failedSendingTask);

        Throwable e = failedSendingTask.getError();
        GoogleAccountCredential credential = (GoogleAccountCredential) getParameter(GmailSender.PARAM_GMAIL_CREDENTIAL);
        GmailSenderPreferences preferences = (GmailSenderPreferences) getSenderPreferences();

        // in this case just ask for permissions
        if(e instanceof UserRecoverableAuthIOException || e instanceof UserRecoverableAuthException) {
            Intent intent = e instanceof UserRecoverableAuthIOException ? ((UserRecoverableAuthIOException) e).getIntent() : ((UserRecoverableAuthException) e).getIntent();
            startActivityForResult(failedSendingTask.getId(), REQUEST_AUTHORIZATION, intent);
            return;
        }

        // in this case pick an account from those registered in the Android device
        if(e instanceof MailPreferredAccountNotSetException) {
            Account[] accounts = AccountManager.get(getContext()).getAccountsByType("com.google");
            if(accounts.length <= 0) {
                // no accounts in the device, so just fail
                doNotRecover(failedSendingTask);
                return;
            }

            if(accounts.length <= 1) {
                // one account in the device, so automatically pick it
                credential.setSelectedAccountName(accounts[0].name);
                preferences.setPreferredAccountName(accounts[0].name);
                doRecover(failedSendingTask);
                return;
            }

            // multiple accounts in the device - ask the user to pick one
            startActivityForResult(failedSendingTask.getId(), REQUEST_ACCOUNT_PICKER, credential.newChooseAccountIntent());
            return;
        }

        // in this case, try to ask for another access token
        if(e instanceof GoogleJsonResponseException || e instanceof GmailResponseException || e instanceof InvalidAccessTokenException) {
            try {
                Throwable obj = new GetTokenAsyncTask().execute(credential).get();
                if(obj == null) {
                    doRecover(failedSendingTask);
                } else if(obj instanceof UserRecoverableAuthIOException || obj instanceof UserRecoverableAuthException) {
                    Intent intent = obj instanceof UserRecoverableAuthIOException ? ((UserRecoverableAuthIOException) obj).getIntent() : ((UserRecoverableAuthException) obj).getIntent();
                    startActivityForResult(failedSendingTask.getId(), REQUEST_AUTHORIZATION, intent);
                } else {
                    throw obj;
                }
            } catch (Throwable ex) {
                Log.e(TAG, "Error invalidating Gmail API token!", ex);
                TrackerManager.getInstance(getContext().getApplicationContext()).logException(ex);
                doNotRecover(failedSendingTask);
            }
            return;
        }

        // NoInternetConnectionException might include small connection issues
        // even if there's internet connection, so allow the retries
        checkRetries(failedSendingTask);
    }
}
