package com.peppermint.app.sending.exceptions;

/**
 * Created by Nuno Luz on 05-10-2015.
 *
 * Exception that should be thrown when there's no internet connectivity.
 * Since it is an {@link ElectableForQueueingException}, it allows a {@link com.peppermint.app.data.SendingRequest}
 * to be queued for future retries if there's no connectivity.
 */
public class NoInternetConnectionException extends ElectableForQueueingException {
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
