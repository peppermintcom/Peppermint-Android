package com.peppermint.app.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.cloud.apis.GoogleApi;
import com.peppermint.app.cloud.apis.PeppermintApi;
import com.peppermint.app.cloud.apis.data.JWTsResponse;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiNoAccountException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiResponseException;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 26-01-2016.
 *
 * Authenticator class for Peppermint accounts.
 *
 */
class Authenticator extends AbstractAccountAuthenticator {

    private static final String TAG = Authenticator.class.getSimpleName();

    private final Handler mHandler;
    private final Context mContext;

    private final GoogleApi mGoogleApi;
    private final PeppermintApi mPeppermintApi;
    private final AuthenticatorUtils mAuthenticatorUtils;
    private final SenderPreferences mSenderPreferences;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
        mHandler = new Handler(context.getMainLooper());
        mGoogleApi = new GoogleApi(context);
        mPeppermintApi = new PeppermintApi(context);
        mAuthenticatorUtils = new AuthenticatorUtils(context);
        mSenderPreferences = new SenderPreferences(context);
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        mAuthenticatorUtils.refreshAccount();
        if(mAuthenticatorUtils.getAccount() != null) {
            // allow only one Peppermint account on the device
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, R.string.only_one_peppermint_account_supported, Toast.LENGTH_LONG).show();
                }
            });

            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "Only one Peppermint account is supported");
            return result;
        }

        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {

        try {
            // if the caller requested an authToken type we don't support, then return an error
            if (authTokenType.compareTo(AuthenticatorConstants.FULL_TOKEN_TYPE) != 0) {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ERROR_MESSAGE, "Invalid authTokenType");
                return result;
            }

            // extract the data from the Account Manager, and ask the server for an appropriate AuthToken.
            mAuthenticatorUtils.refreshAccount();

            AuthenticationData data = mAuthenticatorUtils.getAccountData();
            try {
                // google account setup
                TrackerManager.getInstance(mContext).setUserEmail(data.getEmail());
                mGoogleApi.setAccountName(data.getEmail());
                final String accountPassword = mGoogleApi.renewAuthenticationToken();

                // refresh name just in case
                final GoogleApi.UserInfoResponse userInfoResponse = mGoogleApi.getUserInfo(null);

                final String firstName = userInfoResponse.getFirstName();
                final String lastName = userInfoResponse.getLastName();
                final String fullName = userInfoResponse.getFullName();

                if (firstName != null && lastName != null && Utils.isValidName(firstName) && Utils.isValidName(lastName)) {
                    mSenderPreferences.setFirstName(firstName);
                    mSenderPreferences.setLastName(lastName);
                } else if (fullName != null && Utils.isValidName(fullName)) {
                    String[] names = Utils.getFirstAndLastNames(fullName);
                    mSenderPreferences.setFirstName(names[0]);
                    mSenderPreferences.setLastName(names[1]);
                }

                // peppermint account setup
                JWTsResponse authResponse = mPeppermintApi.authBoth(null, data.getEmail(), accountPassword, data.getDeviceId(), data.getDeviceKey(), data.getAccountType());

                mAuthenticatorUtils.updateAccountServerId(authResponse.getAccount().getAccountId());
                mAuthenticatorUtils.updateAccountPassword(accountPassword);

                if (!TextUtils.isEmpty(authResponse.getAccessToken())) {
                    TrackerManager.getInstance(mContext.getApplicationContext()).setUserEmail(account.name);

                    final Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, AuthenticatorConstants.ACCOUNT_TYPE);
                    result.putString(AccountManager.KEY_AUTHTOKEN, authResponse.getAccessToken());
                    return result;
                }
            } catch (PeppermintApiInvalidAccessTokenException e) {
                Log.w(TAG, "Invalid credentials!", e);
            } catch (PeppermintApiResponseException e) {
                throw new NetworkErrorException(e);
            }

        } catch (PeppermintApiNoAccountException e) {
            TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
            throw new RuntimeException(e);
        } catch (Throwable e) {
            TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
            return result;
        }

        // if we get here, then we need to re-prompt the user for credentials.
        // we do that by creating an intent to display the AuthenticatorActivity.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
        // this call is used to query whether the Authenticator supports
        // specific features. We don't expect to get called, so we always
        // return false (no) for any queries.
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return authTokenType + " (Label)";
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

}
