package com.peppermint.app.sending.exceptions;

/**
 * Created by Nuno Luz on 05-10-2015.
 *
 * Type of exception that allows a sending request to be queued for future retries.
 */
public class ElectableForQueueingException extends Exception {
    public ElectableForQueueingException() {
    }

    public ElectableForQueueingException(String detailMessage) {
        super(detailMessage);
    }

    public ElectableForQueueingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ElectableForQueueingException(Throwable throwable) {
        super(throwable);
    }
}
