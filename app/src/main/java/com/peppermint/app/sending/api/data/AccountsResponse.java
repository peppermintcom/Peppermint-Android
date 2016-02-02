package com.peppermint.app.sending.api.data;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class AccountsResponse extends AccessTokenResponse {

    private String mAccountId;
    private String mEmail, mFullName;
    private String mRegistrationTimestamp;
    private boolean mVerified;

    public AccountsResponse() {
    }

    public AccountsResponse(String mAccountId, String mEmail, String mFullName, String mRegistrationTimestamp, boolean mVerified) {
        this.mAccountId = mAccountId;
        this.mEmail = mEmail;
        this.mFullName = mFullName;
        this.mRegistrationTimestamp = mRegistrationTimestamp;
        this.mVerified = mVerified;
    }

    public String getAccountId() {
        return mAccountId;
    }

    public void setAccountId(String mAccountId) {
        this.mAccountId = mAccountId;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public String getFullName() {
        return mFullName;
    }

    public void setFullName(String mFullName) {
        this.mFullName = mFullName;
    }

    public String getRegistrationTimestamp() {
        return mRegistrationTimestamp;
    }

    public void setRegistrationTimestamp(String mRegistrationTimestamp) {
        this.mRegistrationTimestamp = mRegistrationTimestamp;
    }

    public boolean isVerified() {
        return mVerified;
    }

    public void setVerified(boolean mVerified) {
        this.mVerified = mVerified;
    }

    @Override
    public String toString() {
        return "AccountsResponse{" +
                "mAccountId='" + mAccountId + '\'' +
                ", mEmail='" + mEmail + '\'' +
                ", mFullName='" + mFullName + '\'' +
                ", mRegistrationTimestamp='" + mRegistrationTimestamp + '\'' +
                ", mVerified=" + mVerified +
                '}';
    }
}
