package com.peppermint.app.sending;

import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticatorUtils;
import com.peppermint.app.data.Message;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.mail.gmail.GmailSender;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     The abstract AsyncTask implementation executed by a {@link Sender}.<br />
 *     Each {@link Sender} must have their own {@link GoogleAuthorizationSupportTask} concrete implementations.
 * </p>
 * <p>
 *     As with {@link Sender}s, {@link GoogleAuthorizationSupportTask}s are configured through Parameters and Preferences.
 *     Preferences use the native Android Shared Preferences mechanism, so that data is saved
 *     across different executions of the app. Parameters are part of a key-value map passed to
 *     the sending task instance (each implementation may have its own parameters).
 * </p>
 */
public class GoogleAuthorizationSupportTask extends SenderSupportTask implements Cloneable {

    public GoogleAuthorizationSupportTask(GoogleAuthorizationSupportTask supportTask) {
        super(supportTask);
    }

    public GoogleAuthorizationSupportTask(Sender sender, Message message, SenderSupportListener senderSupportListener) {
        super(sender, message, senderSupportListener);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();

        AuthenticatorUtils utils = new AuthenticatorUtils(getContext());
        AuthenticationData data = utils.getAccountData();

        GoogleApi api = getGoogleApi(data.getEmail());
        String token = api.refreshAccessToken();

        // refresh name just in case
        GoogleApi.UserInfoResponse response = api.getUserInfo();

        String firstName = response.getFirstName();
        String lastName = response.getLastName();
        String fullName = response.getFullName();

        if (firstName != null && lastName != null && Utils.isValidName(firstName) && Utils.isValidName(lastName)) {
            getSenderPreferences().setFirstName(firstName);
            getSenderPreferences().setLastName(lastName);
        } else if (fullName != null && Utils.isValidName(fullName)) {
            String[] names = Utils.getFirstAndLastNames(fullName);
            getSenderPreferences().setFirstName(names[0]);
            getSenderPreferences().setLastName(names[1]);
        }

        // update token in account password
        if(data.getAccountType() == PeppermintApi.ACCOUNT_TYPE_GOOGLE) {
            utils.updateAccountPassword(token);
        }
    }

    protected GoogleApi getGoogleApi(String email) {
        GoogleApi api = (GoogleApi) getParameter(GmailSender.PARAM_GOOGLE_API);
        if(api == null) {
            api = new GoogleApi(getContext());
            setParameter(GmailSender.PARAM_GOOGLE_API, api);
        }
        if(api.getCredential() == null || api.getService() == null || api.getAccountName().compareTo(email) != 0) {
            api.setAccountName(email);
        }
        return api;
    }
}
