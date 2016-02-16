package com.peppermint.app.authenticator;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 02-02-2016.
 * <p>
 *      Wrapper class for data saved in a Peppermint Android account.
 * </p>
 */
public class AuthenticationData implements Serializable {
    private String mAccountServerId;
    private String mEmail, mPassword;
    private String mDeviceId, mDeviceKey;
    private int mAccountType;
    private String mDeviceServerId;
    private String mGcmRegistration;

    public AuthenticationData() {
    }

    public AuthenticationData(String mAccountServerId, String mEmail, String mPassword, String mDeviceServerId, String mDeviceId, String mDeviceKey, int mAccountType, String mGcmRegistration) {
        this.mAccountServerId = mAccountServerId;
        this.mEmail = mEmail;
        this.mPassword = mPassword;
        this.mDeviceId = mDeviceId;
        this.mDeviceKey = mDeviceKey;
        this.mAccountType = mAccountType;
        this.mDeviceServerId = mDeviceServerId;
        this.mGcmRegistration = mGcmRegistration;
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

    public int getAccountType() {
        return mAccountType;
    }

    public void setAccountType(int mAccountType) {
        this.mAccountType = mAccountType;
    }

    public String getDeviceServerId() {
        return mDeviceServerId;
    }

    public void setDeviceServerId(String mDeviceServerId) {
        this.mDeviceServerId = mDeviceServerId;
    }

    public String getGcmRegistration() {
        return mGcmRegistration;
    }

    public void setGcmRegistration(String mGcmRegistration) {
        this.mGcmRegistration = mGcmRegistration;
    }

    public String getAccountServerId() {
        return mAccountServerId;
    }

    public void setAccountServerId(String mAccountServerId) {
        this.mAccountServerId = mAccountServerId;
    }

    @Override
    public String toString() {
        return "AuthenticationData{" +
                "mAccountServerId='" + mAccountServerId + '\'' +
                ", mEmail='" + mEmail + '\'' +
                ", mPassword='" + mPassword + '\'' +
                ", mDeviceId='" + mDeviceId + '\'' +
                ", mDeviceKey='" + mDeviceKey + '\'' +
                ", mAccountType=" + mAccountType +
                ", mDeviceServerId=" + mDeviceServerId +
                ", mGcmRegistration=" + mGcmRegistration +
                '}';
    }
}
