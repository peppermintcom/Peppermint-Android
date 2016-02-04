package com.peppermint.app.authenticator;

import android.content.Context;

import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.sending.mail.gmail.GmailSender;
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
    private String mEmail;

    public AuthenticationGoogleApiTask(Context context, String email, SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        this.mEmail = email;
        getIdentity().setContext(context);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();
        // try to authorize Google API
        GoogleApi googleApi = getGoogleApi(mEmail);
        googleApi.refreshAccessToken();

        GoogleApi.UserInfoResponse response = googleApi.getUserInfo();

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

        mGoogleToken = googleApi.getCredential().getToken();
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
