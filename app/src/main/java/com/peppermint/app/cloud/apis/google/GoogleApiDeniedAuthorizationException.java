package com.peppermint.app.cloud.apis.google;

/**
 * Created by Nuno Luz on 01-02-2016.
 */
public class GoogleApiDeniedAuthorizationException extends Exception {
    public GoogleApiDeniedAuthorizationException() {
    }

    public GoogleApiDeniedAuthorizationException(String detailMessage) {
        super(detailMessage);
    }

    public GoogleApiDeniedAuthorizationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public GoogleApiDeniedAuthorizationException(Throwable throwable) {
        super(throwable);
    }
}
