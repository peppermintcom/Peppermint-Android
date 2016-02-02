package com.peppermint.app.sending.api.data;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class RecordResponse implements Serializable {

    private String mCanonicalUrl, mShortUrl;

    public RecordResponse() {
    }

    public RecordResponse(String mCanonicalUrl, String mShortUrl) {
        this.mCanonicalUrl = mCanonicalUrl;
        this.mShortUrl = mShortUrl;
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
                "mCanonicalUrl='" + mCanonicalUrl + '\'' +
                ", mShortUrl='" + mShortUrl + '\'' +
                '}';
    }
}
