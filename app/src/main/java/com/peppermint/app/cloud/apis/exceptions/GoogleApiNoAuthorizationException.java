package com.peppermint.app.cloud.apis.exceptions;

import android.content.Intent;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

/**
 * Created by Nuno Luz on 01-02-2016.
 */
public class GoogleApiNoAuthorizationException extends Exception {

    private Intent mHandleIntent;

    public GoogleApiNoAuthorizationException(Intent mHandleIntent) {
        this.mHandleIntent = mHandleIntent;
    }

    public GoogleApiNoAuthorizationException(UserRecoverableAuthIOException e) {
        super(e.getMessage(), e);
        this.mHandleIntent = e.getIntent();
    }

    public GoogleApiNoAuthorizationException(UserRecoverableAuthException e) {
        super(e.getMessage(), e);
        this.mHandleIntent = e.getIntent();
    }

    public GoogleApiNoAuthorizationException(String detailMessage) {
        super(detailMessage);
    }

    public GoogleApiNoAuthorizationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public GoogleApiNoAuthorizationException(Throwable throwable) {
        super(throwable);
    }

    public Intent getHandleIntent() {
        return mHandleIntent;
    }

    public void setHandleIntent(Intent mHandleIntent) {
        this.mHandleIntent = mHandleIntent;
    }
}
