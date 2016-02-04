package com.peppermint.app.sending.exceptions;

import com.peppermint.app.data.Message;

/**
 * Created by Nuno Luz on 05-10-2015.
 *
 * Exception that should be thrown when there's no internet connectivity.
 * Since it is an {@link ElectableForQueueingException}, it allows a {@link Message}
 * to be queued for future retries if there's no connectivity.
 */
public class TryAgainException extends Exception implements ElectableForQueueingException {
    public TryAgainException() {
    }

    public TryAgainException(String detailMessage) {
        super(detailMessage);
    }

    public TryAgainException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public TryAgainException(Throwable throwable) {
        super(throwable);
    }
}
