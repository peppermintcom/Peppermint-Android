package com.peppermint.app.events;

import com.peppermint.app.authenticator.AuthenticationData;

/**
 * Created by Nuno Luz on 16-03-2016.
 */
public class SignInEvent {

    private AuthenticationData mAuthenticationData;

    public SignInEvent() {
    }

    public SignInEvent(AuthenticationData mAuthenticationData) {
        this.mAuthenticationData = mAuthenticationData;
    }

    public AuthenticationData getAuthenticationData() {
        return mAuthenticationData;
    }

    public void setAuthenticationData(AuthenticationData mAuthenticationData) {
        this.mAuthenticationData = mAuthenticationData;
    }
}
