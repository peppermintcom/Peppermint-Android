package com.peppermint.app.data;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 11-05-2016.
 *
 * Represents a pending logout.
 */
public class PendingLogout implements Serializable {

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
        this.mDeviceServerId = mDeviceServerId;
    }

    public String getAccountServerId() {
        return mAccountServerId;
    }

    public void setAccountServerId(String mAccountServerId) {
        this.mAccountServerId = mAccountServerId;
    }

    public String getAuthenticationToken() {
        return mAuthenticationToken;
    }

    public void setAuthenticationToken(String mAuthenticationToken) {
        this.mAuthenticationToken = mAuthenticationToken;
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
                '}';
    }
}
