package com.peppermint.app.cloud.apis.google;

import android.content.Intent;

import com.google.android.gms.auth.UserRecoverableAuthException;

public class GoogleApiNoAuthorizationException extends GoogleApiResponseException {

    private Intent mHandleIntent;

    public GoogleApiNoAuthorizationException(UserRecoverableAuthException e) {
        super(e.getMessage(), e);
        this.mHandleIntent = e.getIntent();
    }

    public Intent getHandleIntent() {
        return mHandleIntent;
    }
}
