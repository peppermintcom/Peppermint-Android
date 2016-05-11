package com.peppermint.app.cloud.senders.exceptions;

/**
 * Created by Nuno Luz on 05-10-2015.
 *
 */
public class NoInternetConnectionException extends Exception {
    public NoInternetConnectionException() {
    }

    public NoInternetConnectionException(String detailMessage) {
        super(detailMessage);
    }

    public NoInternetConnectionException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NoInternetConnectionException(Throwable throwable) {
        super(throwable);
    }
}
