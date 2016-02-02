package com.peppermint.app.authenticator;

import android.content.Context;

import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Peppermint API authorization support task.
 * </p>
 */
public class AuthenticationGoogleApiTask extends SenderSupportTask {

    private static final String TAG = AuthenticationGoogleApiTask.class.getSimpleName();

    private String mGoogleToken;
    private String mFirstName, mLastName;

    public AuthenticationGoogleApiTask(Context context, GoogleApi googleApi, SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        setParameter(Sender.PARAM_GOOGLE_API, googleApi);
        getIdentity().setContext(context);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();
        // try to authorize Google API
        getGoogleApi().refreshAccessToken();

        GoogleApi.UserInfoResponse response = getGoogleApi().getUserInfo();

        String firstName = response.getFirstName();
        String lastName = response.getLastName();
        String fullName = response.getFullName();

        if (firstName != null && lastName != null && Utils.isValidName(firstName) && Utils.isValidName(lastName)) {
            mFirstName = firstName;
            mLastName = lastName;
        } else if (fullName != null && Utils.isValidName(fullName)) {
            String[] names = Utils.getFirstAndLastNames(fullName);
            mFirstName = names[0];
            mLastName = names[1];
        }

        mGoogleToken = getGoogleApi().getCredential().getToken();
    }

    public String getGoogleToken() {
        return mGoogleToken;
    }
}
