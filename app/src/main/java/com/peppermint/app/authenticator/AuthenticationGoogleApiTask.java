package com.peppermint.app.authenticator;

import android.content.Context;

import com.peppermint.app.cloud.apis.GoogleApi;
import com.peppermint.app.cloud.senders.SenderSupportListener;
import com.peppermint.app.cloud.senders.SenderSupportTask;
import com.peppermint.app.cloud.senders.mail.gmail.GmailSender;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Authentication task for the Google API.
 * </p>
 */
public class AuthenticationGoogleApiTask extends SenderSupportTask {

    private String mGoogleToken;
    private String mEmail;

    public AuthenticationGoogleApiTask(Context context, String email, SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        this.mEmail = email;
        // override to set the context
        getIdentity().setContext(context);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();

        // try to authorize and get the token from the Google API
        GoogleApi googleApi = getGoogleApi(mEmail);
        googleApi.refreshAccessToken();

        // get the user name from the Google API
        GoogleApi.UserInfoResponse response = googleApi.getUserInfo(getId().toString());

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

        mGoogleToken = googleApi.getAccessToken();
    }

    public String getGoogleToken() {
        return mGoogleToken;
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
