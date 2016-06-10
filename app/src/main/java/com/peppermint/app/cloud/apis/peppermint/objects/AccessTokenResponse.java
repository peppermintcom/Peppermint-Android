package com.peppermint.app.cloud.apis.peppermint.objects;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Data wrapper for HTTP access token responses.
 */
public class AccessTokenResponse implements Serializable {

    private String mAccessToken;

    public AccessTokenResponse() {
    }

    public AccessTokenResponse(String mAccessToken) {
        this.mAccessToken = mAccessToken;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String mAccessToken) {
        this.mAccessToken = mAccessToken;
    }

    @Override
    public String toString() {
        return "AccessTokenResponse{" +
                "mAccessToken='" + mAccessToken + '\'' +
                '}';
    }
}
