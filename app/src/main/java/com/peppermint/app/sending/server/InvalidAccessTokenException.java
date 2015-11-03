package com.peppermint.app.sending.server;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
    }

    public InvalidAccessTokenException(String detailMessage) {
        super(detailMessage);
    }

    public InvalidAccessTokenException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public InvalidAccessTokenException(Throwable throwable) {
        super(throwable);
    }
}
