package com.peppermint.app.sending.api.data;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class AccessTokenResponse implements Serializable {
    private String mAccessToken;
    public AccessTokenResponse() {
    }
    public String getAccessToken() {
        return mAccessToken;
    }
    public void setAccessToken(String mAccessToken) {
        this.mAccessToken = mAccessToken;
    }
}
