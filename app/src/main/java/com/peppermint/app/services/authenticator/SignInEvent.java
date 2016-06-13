package com.peppermint.app.services.authenticator;

/**
 * Created by Nuno Luz on 16-03-2016.
 *
 * Represents a sign in event triggered by the {@link AuthenticationService}.
 *
 */
public class SignInEvent {

    private AuthenticationData mAuthenticationData;

    public SignInEvent(AuthenticationData mAuthenticationData) {
        this.mAuthenticationData = mAuthenticationData;
    }

    public AuthenticationData getAuthenticationData() {
        return mAuthenticationData;
    }

    public void setAuthenticationData(AuthenticationData mAuthenticationData) {
        this.mAuthenticationData = mAuthenticationData;
    }

    @Override
    public String toString() {
        return "SignInEvent{" +
                "mAuthenticationData=" + mAuthenticationData +
                '}';
    }
}
