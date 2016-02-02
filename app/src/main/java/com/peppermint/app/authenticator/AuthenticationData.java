package com.peppermint.app.authenticator;

/**
 * Created by Nuno Luz on 02-02-2016.
 */
public class AuthenticationData {
    private String mEmail, mPassword;
    private String mDeviceId, mDeviceKey;
    private String mFirstName, mLastName;
    private int mAccountType;

    public AuthenticationData() {
    }

    public AuthenticationData(String mEmail, String mPassword, String mDeviceId, String mDeviceKey, String mFirstName, String mLastName, int mAccountType) {
        this.mEmail = mEmail;
        this.mPassword = mPassword;
        this.mDeviceId = mDeviceId;
        this.mDeviceKey = mDeviceKey;
        this.mFirstName = mFirstName;
        this.mLastName = mLastName;
        this.mAccountType = mAccountType;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String mPassword) {
        this.mPassword = mPassword;
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public void setDeviceId(String mDeviceId) {
        this.mDeviceId = mDeviceId;
    }

    public String getDeviceKey() {
        return mDeviceKey;
    }

    public void setDeviceKey(String mDeviceKey) {
        this.mDeviceKey = mDeviceKey;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public void setFirstName(String mFirstName) {
        this.mFirstName = mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public void setLastName(String mLastName) {
        this.mLastName = mLastName;
    }

    public int getAccountType() {
        return mAccountType;
    }

    public void setAccountType(int mAccountType) {
        this.mAccountType = mAccountType;
    }

    @Override
    public String toString() {
        return "AuthenticationData{" +
                "mEmail='" + mEmail + '\'' +
                ", mPassword='" + mPassword + '\'' +
                ", mDeviceId='" + mDeviceId + '\'' +
                ", mDeviceKey='" + mDeviceKey + '\'' +
                ", mFirstName='" + mFirstName + '\'' +
                ", mLastName='" + mLastName + '\'' +
                ", mAccountType=" + mAccountType +
                '}';
    }
}
