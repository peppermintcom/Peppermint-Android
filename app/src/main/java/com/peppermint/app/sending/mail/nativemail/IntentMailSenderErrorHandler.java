package com.peppermint.app.sending.mail.nativemail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Error handler for the {@link IntentMailSender}.
 */
public class IntentMailSenderErrorHandler extends SenderErrorHandler {

    private static final int REQUEST_ACCOUNT_PICKER = 999;

    private static final String ACCOUNT_TYPE = "com.google";

    public IntentMailSenderErrorHandler(Sender sender, SenderUploadListener senderUploadListener) {
        super(sender, senderUploadListener);
    }

    @Override
    protected void onUploadTaskActivityResult(SenderUploadTask recoveredTask, int requestCode, int resultCode, Intent data) {
        IntentMailSenderPreferences preferences = (IntentMailSenderPreferences) getPreferences();

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
    protected int tryToRecover(SenderUploadTask failedUploadTask, Throwable error) {

        IntentMailSenderPreferences preferences = (IntentMailSenderPreferences) getPreferences();

        // in this case pick an account from those registered in the Android device
        if(error instanceof MailPreferredAccountNotSetException) {
            Account[] accounts = AccountManager.get(getContext()).getAccountsByType(ACCOUNT_TYPE);
            if(accounts.length <= 0) {
                // no accounts in the device, so just fail
                return RECOVERY_NOK;
            }

            if(accounts.length <= 1) {
                // one account in the device, so automatically pick it
                preferences.setPreferredAccountName(accounts[0].name);
                return RECOVERY_RETRY;
            }

            // multiple accounts in the device - ask the user to pick one
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivityForResult(failedUploadTask, REQUEST_ACCOUNT_PICKER, AccountManager.newChooseAccountIntent(null, null,
                        new String[]{ACCOUNT_TYPE}, null, null, null, null));
            } else {
                startActivityForResult(failedUploadTask, REQUEST_ACCOUNT_PICKER, AccountManager.newChooseAccountIntent(null, null,
                        new String[]{ACCOUNT_TYPE}, true, null, null, null, null));
            }

            return RECOVERY_DONOTHING;
        }

        return RECOVERY_NOK;
    }

}
