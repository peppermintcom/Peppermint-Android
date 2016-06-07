package com.peppermint.app.ui.authentication;

import android.content.Context;

import com.peppermint.app.cloud.apis.google.GoogleApi;
import com.peppermint.app.cloud.apis.google.GoogleApiNoAuthorizationException;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.services.messenger.handlers.SenderSupportListener;
import com.peppermint.app.services.messenger.handlers.SenderSupportTask;
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

    public AuthenticationGoogleApiTask(final Context context, final String email, final SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        this.mEmail = email;

        // override to set the context
        getIdentity().setContext(context);
        getIdentity().setPreferences(new SenderPreferences(context));
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();
        // try to authorize and get the token from the Google API
        final GoogleApi googleApi = getGoogleApi(mEmail);

        try {
            mGoogleToken = googleApi.renewAuthenticationToken();
        } catch (GoogleApiNoAuthorizationException e) {
            // retry since it might take a while for the permissions to take effect
            try {
                Thread.sleep(1000l);
            } catch(Exception e1) {
                /* nothing to do here; eat up the exception */
            }
            mGoogleToken = googleApi.renewAuthenticationToken();
        }

        // refresh name just in case
        final SenderPreferences senderPreferences = getSenderPreferences();
        final GoogleApi.UserInfoResponse userInfoResponse = googleApi.getUserInfo(null);

        final String firstName = userInfoResponse.getFirstName();
        final String lastName = userInfoResponse.getLastName();
        final String fullName = userInfoResponse.getFullName();

        if (firstName != null && lastName != null && Utils.isValidName(firstName) && Utils.isValidName(lastName)) {
            senderPreferences.setFirstName(firstName);
            senderPreferences.setLastName(lastName);
        } else if (fullName != null && Utils.isValidName(fullName)) {
            String[] names = Utils.getFirstAndLastNames(fullName);
            senderPreferences.setFirstName(names[0]);
            senderPreferences.setLastName(names[1]);
        }
    }

    public String getGoogleToken() {
        return mGoogleToken;
    }

}
