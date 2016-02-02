package com.peppermint.app.sending.api.data;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class UploadsResponse implements Serializable {

    private String mSignedUrl;
    private String mCanonicalUrl, mShortUrl;

    public UploadsResponse() {
    }

    public UploadsResponse(String mSignedUrl, String mCanonicalUrl, String mShortUrl) {
        this.mSignedUrl = mSignedUrl;
        this.mCanonicalUrl = mCanonicalUrl;
        this.mShortUrl = mShortUrl;
    }

    public String getSignedUrl() {
        return mSignedUrl;
    }

    public void setSignedUrl(String mSignedUrl) {
        this.mSignedUrl = mSignedUrl;
    }

    public String getCanonicalUrl() {
        return mCanonicalUrl;
    }

    public void setCanonicalUrl(String mCanonicalUrl) {
        this.mCanonicalUrl = mCanonicalUrl;
    }

    public String getShortUrl() {
        return mShortUrl;
    }

    public void setShortUrl(String mShortUrl) {
        this.mShortUrl = mShortUrl;
    }

    @Override
    public String toString() {
        return "UploadsResponse{" +
                "mSignedUrl='" + mSignedUrl + '\'' +
                ", mCanonicalUrl='" + mCanonicalUrl + '\'' +
                ", mShortUrl='" + mShortUrl + '\'' +
                '}';
    }
}
