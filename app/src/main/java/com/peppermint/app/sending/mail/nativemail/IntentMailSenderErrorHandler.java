package com.peppermint.app.sending.mail.nativemail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;
import com.peppermint.app.sending.mail.gmail.GmailSender;

import java.util.Map;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Error handler for the {@link GmailSender}.
 */
public class IntentMailSenderErrorHandler extends SenderErrorHandler {

    private static final String TAG = IntentMailSenderErrorHandler.class.getSimpleName();

    private static final int REQUEST_ACCOUNT_PICKER = 999;

    private static final String ACCOUNT_TYPE = "com.google";

    public IntentMailSenderErrorHandler(Context context, SenderListener senderListener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(context, senderListener, parameters, preferences);
    }

    @Override
    protected void onActivityResult(SenderTask recoveredTask, int requestCode, int resultCode, Intent data) {
        IntentMailSenderPreferences preferences = (IntentMailSenderPreferences) getSenderPreferences();

        // the user has picked one of the multiple available google accounts to use...
        if(requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
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

        doNotRecover(recoveredTask);
    }

    @Override
    public void tryToRecover(SenderTask failedSendingTask) {
        super.tryToRecover(failedSendingTask);

        Throwable e = failedSendingTask.getError();
        IntentMailSenderPreferences preferences = (IntentMailSenderPreferences) getSenderPreferences();
        SendingRequest request = failedSendingTask.getSendingRequest();

        // in this case pick an account from those registered in the Android device
        if(e instanceof MailPreferredAccountNotSetException) {
            Account[] accounts = AccountManager.get(getContext()).getAccountsByType(ACCOUNT_TYPE);
            if(accounts.length <= 0) {
                // no accounts in the device, so just fail
                doNotRecover(failedSendingTask);
                return;
            }

            if(accounts.length <= 1) {
                // one account in the device, so automatically pick it
                preferences.setPreferredAccountName(accounts[0].name);
                doRecover(failedSendingTask);
                return;
            }

            // multiple accounts in the device - ask the user to pick one
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivityForResult(request.getId(), REQUEST_ACCOUNT_PICKER, AccountManager.newChooseAccountIntent(null, null,
                        new String[]{ACCOUNT_TYPE}, null, null, null, null));
            } else {
                startActivityForResult(request.getId(), REQUEST_ACCOUNT_PICKER, AccountManager.newChooseAccountIntent(null, null,
                        new String[]{ACCOUNT_TYPE}, true, null, null, null, null));
            }
            return;
        }

        doNotRecover(failedSendingTask);
    }
}
