package com.peppermint.app.sending.api.exceptions;

/**
 * Created by Nuno Luz on 25-01-2016.
 */
public class GoogleApiInvalidAccessTokenException extends Exception {
    public GoogleApiInvalidAccessTokenException() {
    }

    public GoogleApiInvalidAccessTokenException(String detailMessage) {
        super(detailMessage);
    }

    public GoogleApiInvalidAccessTokenException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public GoogleApiInvalidAccessTokenException(Throwable throwable) {
        super(throwable);
    }
}
