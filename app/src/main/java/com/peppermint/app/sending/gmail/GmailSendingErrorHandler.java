package com.peppermint.app.sending.gmail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingErrorHandler;
import com.peppermint.app.sending.SendingTask;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 */
public class GmailSendingErrorHandler extends SendingErrorHandler {

    private static final String TAG = GmailSendingErrorHandler.class.getSimpleName();

    private static final int REQUEST_AUTHORIZATION = 998;
    private static final int REQUEST_ACCOUNT_PICKER = 999;

    private static final int MAX_RETRIES = 3;

    // Retry Map, which allows trying to send the Email up to MAX_RETRIES times if it fails.
    protected Map<UUID, Integer> mRetryMap;

    public GmailSendingErrorHandler(Context context, SenderListener senderListener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(context, senderListener, parameters, preferences);
        mRetryMap = new HashMap<>();
    }

    @Override
    protected void onActivityResult(SendingTask recoveredTask, int requestCode, int resultCode, Intent data) {
        GoogleAccountCredential credential = (GoogleAccountCredential) getParameter(GmailSender.PARAM_GMAIL_CREDENTIAL);
        GmailSenderPreferences preferences = (GmailSenderPreferences) getSenderPreferences();

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

        if(requestCode == REQUEST_AUTHORIZATION){
            if(resultCode == Activity.RESULT_OK) {
                doRecover(recoveredTask);
                return;
            } else if(resultCode == Activity.RESULT_CANCELED) {
                preferences.setSkipIfPermissionRequired(true);
                Toast.makeText(getContext(), R.string.msg_cancelled_gmail_api, Toast.LENGTH_LONG).show();
            }
        }

        doNotRecover(recoveredTask);
    }

    @Override
    public void tryToRecover(SendingTask failedSendingTask) {
        super.tryToRecover(failedSendingTask);

        Throwable e = failedSendingTask.getError();
        GoogleAccountCredential credential = (GoogleAccountCredential) getParameter(GmailSender.PARAM_GMAIL_CREDENTIAL);
        GmailSenderPreferences preferences = (GmailSenderPreferences) getSenderPreferences();
        SendingRequest request = failedSendingTask.getSendingRequest();

        if(e instanceof UserRecoverableAuthIOException) {
            if(preferences.getSkipIfPermissionRequired()) {
                doNotRecover(failedSendingTask);
                return;
            }

            startActivityForResult(request.getId(), REQUEST_AUTHORIZATION, ((UserRecoverableAuthIOException) e).getIntent());
            return;
        }

        if(e instanceof GmailPreferredAccountNotSetException) {
            Account[] accounts = AccountManager.get(getContext()).getAccountsByType("com.google");
            if(accounts.length <= 0) {
                doNotRecover(failedSendingTask);
                return;
            }

            if(accounts.length <= 1) {
                credential.setSelectedAccountName(accounts[0].name);
                preferences.setPreferredAccountName(accounts[0].name);
                doRecover(failedSendingTask);
                return;
            }

            startActivityForResult(request.getId(), REQUEST_ACCOUNT_PICKER, credential.newChooseAccountIntent());
            return;
        }

        if(e instanceof GoogleJsonResponseException) {
            try {
                GoogleAuthUtil.invalidateToken(getContext(), credential.getToken());
                doRecover(failedSendingTask);
            } catch (Exception ex) {
                Log.e(TAG, "Error invalidating Gmail API token!", ex);
                Crashlytics.logException(ex);
                doNotRecover(failedSendingTask);
            }
            return;
        }

        if(e instanceof NoInternetConnectionException) {
            doNotRecover(failedSendingTask);
            return;
        }

        if(!mRetryMap.containsKey(request.getId())) {
            mRetryMap.put(request.getId(), 1);
        } else {
            mRetryMap.put(request.getId(), mRetryMap.get(request.getId()) + 1);
        }

        if(mRetryMap.get(request.getId()) > MAX_RETRIES) {
            mRetryMap.remove(request.getId());
            doNotRecover(failedSendingTask);
            return;
        }

        // just try again for MAX_RETRIES times tops
        doRecover(failedSendingTask);
    }
}