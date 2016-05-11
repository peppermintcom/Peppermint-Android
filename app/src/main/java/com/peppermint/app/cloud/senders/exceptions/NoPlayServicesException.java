package com.peppermint.app.cloud.senders.exceptions;

/**
 * Created by Nuno Luz on 05-10-2015.
 *
 */
public class NoPlayServicesException extends Exception {
    public NoPlayServicesException() {
    }

    public NoPlayServicesException(String detailMessage) {
        super(detailMessage);
    }

    public NoPlayServicesException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NoPlayServicesException(Throwable throwable) {
        super(throwable);
    }
}
