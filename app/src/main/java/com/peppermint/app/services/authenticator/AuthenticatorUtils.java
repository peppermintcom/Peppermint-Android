package com.peppermint.app.services.authenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import com.peppermint.app.cloud.apis.peppermint.PeppermintApiNoAccountException;
import com.peppermint.app.dal.DatabaseHelper;
import com.peppermint.app.dal.contact.ContactData;
import com.peppermint.app.dal.contact.ContactManager;
import com.peppermint.app.dal.pendinglogout.PendingLogout;
import com.peppermint.app.dal.pendinglogout.PendingLogoutManager;
import com.peppermint.app.services.messenger.MessengerServiceManager;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by Nuno Luz on 02-02-2016.
 * <p>
 *     Helper/utility class for Peppermint Authentication.
 * </p>
 */
public class AuthenticatorUtils {

    private static final long SYNC_INTERVAL = 3600L;

    private TrackerManager mTrackerManager;
    private AccountManager mAccountManager;
    private Context mContext;
    private Account mAccount;

    public AuthenticatorUtils(Context context) {
        mContext = context;
        mTrackerManager = TrackerManager.getInstance(context.getApplicationContext());
        refreshAccount();
    }

    public void requestSync() throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        if(!ContentResolver.isSyncActive(mAccount, AuthenticatorConstants.AUTHORITY)) {
            final Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            ContentResolver.requestSync(mAccount, AuthenticatorConstants.AUTHORITY, bundle);
        }
    }

    public void setupPeriodicSync() throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }
        ContentResolver.addPeriodicSync(
                mAccount,
                AuthenticatorConstants.AUTHORITY,
                Bundle.EMPTY,
                SYNC_INTERVAL);
    }

    /**
     * Check the {@link AccountManager} and get an updated Peppermint account instance.
     */
    public void refreshAccount() {
        mAccountManager = AccountManager.get(mContext);
        Account[] accounts = mAccountManager.getAccountsByType(AuthenticatorConstants.ACCOUNT_TYPE);
        mAccount = accounts != null && accounts.length > 0 ? accounts[0] : null;
    }

    /**
     * Get the current access token associated with the Peppermint account.
     * @return the access token or null if there's none
     * @throws PeppermintApiNoAccountException if no Peppermint account is found
     */
    public String peekAccessToken() throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }
        return mAccountManager.peekAuthToken(mAccount, AuthenticatorConstants.FULL_TOKEN_TYPE);
    }

    /**
     * Invalidate the current Peppermint account access token.
     * @throws PeppermintApiNoAccountException if no Peppermint account is found
     */
    public void invalidateAccessToken() throws PeppermintApiNoAccountException {
        mAccountManager.invalidateAuthToken(AuthenticatorConstants.ACCOUNT_TYPE, peekAccessToken());
    }

    /**
     * Get an access token for the Peppermint account. <strong>This is a blocking operation</strong>
     * and will retrieve an access token from the server if necessary.
     * @return the access token or null if credentials are invalid
     * @throws PeppermintApiNoAccountException if no Peppermint account is found
     */
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

    /**
     * Save the device's GCM registration token in the local Peppermint account data.
     * @param gcmRegistration the GCM registration token
     * @throws PeppermintApiNoAccountException if no Peppermint account is found
     */
    public void updateAccountGcmRegistration(String gcmRegistration) throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_GCM_REG, gcmRegistration);
    }

    public void updateAccountServerId(String accountServerId) throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_ACCOUNT_SERVER_ID, accountServerId);
    }

    /**
     * Set a new password for the local Peppermint account. <b>This will invalidate the current access token.</b>
     * @param password the new password
     * @throws PeppermintApiNoAccountException if no Peppermint account is found
     */
    public void updateAccountPassword(String password) throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        mAccountManager.setPassword(mAccount, password);
        /*invalidateAccessToken();*/
    }

    /**
     * Sign out from Peppermint. This will remove the local Peppermint account from the device.
     * @throws PeppermintApiNoAccountException if no Peppermint account is found
     */
    public void signOut() throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        // cancel all pending messages
        final MessengerServiceManager messengerServiceManager = new MessengerServiceManager(mContext);
        messengerServiceManager.cancel();

        // remove all peppermint contacts
        int permissionCheck = ContextCompat.checkSelfPermission(mContext,
                android.Manifest.permission.WRITE_CONTACTS);

        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Map<Long, ContactData> peppermintContacts = ContactManager.getInstance().getPeppermintContacts(mContext);
            for (ContactData contactData : peppermintContacts.values()) {
                ContactManager.getInstance().deletePeppermint(mContext, contactData.getRawId(), null);
            }
            TrackerManager.getInstance(mContext).log("Unable to reset Peppermint contacts");
        }

        // clear app data and preferences
        Utils.clearApplicationData(mContext);
        clearSharedPreferences();
        DatabaseHelper.clearInstance();

        // register logout request to the backend
        final PendingLogout pendingLogout = new PendingLogout(0,
                mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_SERVER_ID),
                mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_ACCOUNT_SERVER_ID),
                peekAccessToken());

        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(mContext);
        databaseHelper.lock();
        try {
            PendingLogoutManager.getInstance().insert(DatabaseHelper.getInstance(mContext).getWritableDatabase(), pendingLogout);
        } catch (SQLException e) {
            mTrackerManager.logException(e);
        }
        databaseHelper.unlock();

        messengerServiceManager.doPendingLogouts();

        // remove the account
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mAccountManager.removeAccountExplicitly(getAccount());
        } else {
            mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_EMAIL, null);
            mAccountManager.setPassword(mAccount, null);
            mAccountManager.setAuthToken(mAccount, AuthenticatorConstants.FULL_TOKEN_TYPE, null);
        }

        // clear all notifications
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        // send global sign out event
        AuthenticationService.postSignOutEvent();
    }

    private void clearSharedPreferences() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    /**
     * Create a local Peppermint account with the specified data (see also {@link #createAccount(String, String, String, String, String, String, String, int)}).
     * @param accessToken the registered account access token
     * @param data the account data
     */
    public void createAccount(String accessToken, AuthenticationData data) {
        createAccount(accessToken, data.getEmail(), data.getAccountServerId(), data.getPassword(), data.getDeviceServerId(), data.getDeviceId(), data.getDeviceKey(), data.getAccountType());
    }

    /**
     * Create a local Peppermint account with the specified data (see also {@link #createAccount(String, AuthenticationData)}).
     * @param accessToken the registered account access token
     * @param email the account email
     * @param password the account password
     * @param deviceServerId the device id from the server
     * @param deviceId the local device id
     * @param deviceKey the device key/password
     * @param accountType the account type
     */
    public void createAccount(String accessToken, String email, String accountServerId, String password, String deviceServerId, String deviceId, String deviceKey, int accountType) {
        if(mAccount == null) {
            mAccount = new Account(AuthenticatorConstants.ACCOUNT_NAME, AuthenticatorConstants.ACCOUNT_TYPE);
            mAccountManager.addAccountExplicitly(mAccount, password, null);
        }

        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_ACCOUNT_SERVER_ID, accountServerId);
        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_EMAIL, email);
        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_SERVER_ID, deviceServerId);
        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_ID, deviceId);
        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_DEVICE_KEY, deviceKey);
        mAccountManager.setUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_TYPE, String.valueOf(accountType));
        mAccountManager.setPassword(mAccount, password);

        if(accessToken != null) {
            mAccountManager.setAuthToken(mAccount, AuthenticatorConstants.FULL_TOKEN_TYPE, accessToken);
        }

        try {
            AuthenticationService.postSignInEvent(getAccountData());
        } catch (PeppermintApiNoAccountException e) {
            /* nothing to do here */
        }
    }

    /**
     * Get the Peppermint account data in the {@link AuthenticationData} wrapper class.
     * @return the {@link AuthenticationData} instance with the account data
     * @throws PeppermintApiNoAccountException if no Peppermint account is found
     */
    public AuthenticationData getAccountData() throws PeppermintApiNoAccountException {
        if(mAccount == null) {
            throw new PeppermintApiNoAccountException();
        }

        String email = mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_EMAIL);
        if(email == null) {
            throw new PeppermintApiNoAccountException();
        }

        AuthenticationData data = new AuthenticationData();
        data.setGcmRegistration(mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_GCM_REG));
        data.setAccountServerId(mAccountManager.getUserData(mAccount, AuthenticatorConstants.ACCOUNT_PARAM_ACCOUNT_SERVER_ID));
        data.setEmail(email);
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
