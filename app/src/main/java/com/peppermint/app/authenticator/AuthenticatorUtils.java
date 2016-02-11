package com.peppermint.app.authenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.Build;

import com.peppermint.app.sending.api.exceptions.PeppermintApiNoAccountException;
import com.peppermint.app.tracking.TrackerManager;

/**
 * Created by Nuno Luz on 02-02-2016.
 */
public class AuthenticatorUtils {

    private TrackerManager mTrackerManager;
    private AccountManager mAccountManager;
    private Context mContext;
    private Account mAccount;

    public AuthenticatorUtils(Context context) {
        mContext = context;
        mTrackerManager = TrackerManager.getInstance(context.getApplicationContext());
        refreshAccount();
    }

    public void refreshAccount() {
        mAccountManager = AccountManager.get(mContext);
        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorConstants.ACCOUNT_TYPE);
        mAccount = accounts != null && accounts.length > 0 ? accounts[0] : null;
    }

    public String peekAccessToken() throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }
        return mAccountManager.peekAuthToken(mAccount, AuthenticatorConstants.FULL_TOKEN_TYPE);
    }

    public void invalidateAccessToken() throws PeppermintApiNoAccountException {
        mAccountManager.invalidateAuthToken(AuthenticatorConstants.FULL_TOKEN_TYPE, peekAccessToken());
    }

    public String getAccessToken() throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        try {
            return mAccountManager.blockingGetAuthToken(mAccount, AuthenticatorConstants.FULL_TOKEN_TYPE, true);
        } catch (OperationCanceledException e) {
            mTrackerManager.log("Authentication cancelled.", e);
            throw new PeppermintApiNoAccountException(e);
        } catch (Exception e) {
            mTrackerManager.log("Authentication error.", e);
            throw new PeppermintApiNoAccountException(e);
        }
    }

    public void updateAccountGcmRegistration(String gcmRegistration) throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_GCM_REG, gcmRegistration);
    }

    public void updateAccountPassword(String password) throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        mAccountManager.setPassword(mAccount, password);
        invalidateAccessToken();
    }

    public void signOut() throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mAccountManager.removeAccountExplicitly(getAccount());
        } else {
            mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_EMAIL, null);
            mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_TYPE, null);
            mAccountManager.setPassword(mAccount, null);
            mAccountManager.setAuthToken(mAccount, AuthenticatorConstants.FULL_TOKEN_TYPE, null);
        }
    }

    public void createAccount(String accessToken, AuthenticationData data) {
        createAccount(accessToken, data.getEmail(), data.getPassword(), data.getDeviceServerId(), data.getDeviceId(), data.getDeviceKey(), data.getAccountType());
    }

    public void createAccount(String accessToken, String email, String password, String deviceServerId, String deviceId, String deviceKey, int accountType) {
        if(mAccount == null) {
            mAccount = new Account(AuthenticatorConstants.ACCOUNT_NAME, AuthenticatorConstants.ACCOUNT_TYPE);
            mAccountManager.addAccountExplicitly(mAccount, password, null);
        }

        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_EMAIL, email);
        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_SERVER_ID, deviceServerId);
        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_ID, deviceId);
        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_KEY, deviceKey);
        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_TYPE, String.valueOf(accountType));
        mAccountManager.setPassword(mAccount, password);

        if(accessToken != null) {
            mAccountManager.setAuthToken(mAccount, AuthenticatorConstants.FULL_TOKEN_TYPE, accessToken);
        }
    }

    public AuthenticationData getAccountData() throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        AuthenticationData data = new AuthenticationData();
        data.setGcmRegistration(mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_GCM_REG));
        data.setEmail(mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_EMAIL));
        data.setDeviceServerId(mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_SERVER_ID));
        data.setDeviceId(mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_ID));
        data.setDeviceKey(mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_KEY));
        data.setAccountType(Integer.valueOf(mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_TYPE)));
        data.setPassword(mAccountManager.getPassword(mAccount));

        return data;
    }

    public Account getAccount() {
        return mAccount;
    }

    public AccountManager getAccountManager() {
        return mAccountManager;
    }

}
