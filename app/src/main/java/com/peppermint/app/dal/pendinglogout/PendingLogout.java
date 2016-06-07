package com.peppermint.app.dal.pendinglogout;

import com.peppermint.app.dal.DataObject;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 11-05-2016.
 *
 * Represents a pending logout.
 */
public class PendingLogout extends DataObject implements Serializable {

    public static final int FIELD_DEVICE_SERVER_ID = 1;
    public static final int FIELD_ACCOUNT_SERVER_ID = 2;
    public static final int FIELD_AUTHENTICATION_TOKEN = 3;

    private long mId;
    private String mDeviceServerId, mAccountServerId;
    private String mAuthenticationToken;

    public PendingLogout() {}

    public PendingLogout(long mId, String mDeviceServerId, String mAccountServerId, String mAuthenticationToken) {
        this();
        this.mId = mId;
        this.mDeviceServerId = mDeviceServerId;
        this.mAccountServerId = mAccountServerId;
        this.mAuthenticationToken = mAuthenticationToken;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public String getDeviceServerId() {
        return mDeviceServerId;
    }

    public void setDeviceServerId(String mDeviceServerId) {
        String oldValue = this.mDeviceServerId;
        this.mDeviceServerId = mDeviceServerId;
        registerUpdate(FIELD_DEVICE_SERVER_ID, oldValue, mDeviceServerId);
    }

    public String getAccountServerId() {
        return mAccountServerId;
    }

    public void setAccountServerId(String mAccountServerId) {
        String oldValue = this.mAccountServerId;
        this.mAccountServerId = mAccountServerId;
        registerUpdate(FIELD_ACCOUNT_SERVER_ID, oldValue, mAccountServerId);
    }

    public String getAuthenticationToken() {
        return mAuthenticationToken;
    }

    public void setAuthenticationToken(String mAuthenticationToken) {
        String oldValue = this.mAuthenticationToken;
        this.mAuthenticationToken = mAuthenticationToken;
        registerUpdate(FIELD_AUTHENTICATION_TOKEN, oldValue, mAuthenticationToken);
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }

        // to allow comparison operations performed by native Java lists
        if(o instanceof PendingLogout) {
            if(((PendingLogout) o).mId > 0 && mId > 0) {
                return ((PendingLogout) o).mId == mId;
            }
        }
        return super.equals(o);
    }

    @Override
    public String toString() {
        return "PendingLogout{" +
                "mId=" + mId +
                ", mDeviceServerId='" + mDeviceServerId + '\'' +
                ", mAccountServerId='" + mAccountServerId + '\'' +
                ", mAuthenticationToken='" + mAuthenticationToken + '\'' +
                ", " + super.toString() +
                '}';
    }
}
