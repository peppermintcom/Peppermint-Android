package com.peppermint.app.authenticator;

import android.content.Context;

import com.peppermint.app.cloud.apis.GoogleApi;
import com.peppermint.app.cloud.apis.exceptions.GoogleApiNoAuthorizationException;
import com.peppermint.app.cloud.senders.SenderSupportListener;
import com.peppermint.app.cloud.senders.SenderSupportTask;

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
    }

    public String getGoogleToken() {
        return mGoogleToken;
    }

}
