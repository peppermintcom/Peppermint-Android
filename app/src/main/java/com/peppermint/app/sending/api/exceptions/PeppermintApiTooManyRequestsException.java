package com.peppermint.app.sending.api.exceptions;

import com.peppermint.app.sending.exceptions.ElectableForQueueingException;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class PeppermintApiTooManyRequestsException extends PeppermintApiException implements ElectableForQueueingException {

    public PeppermintApiTooManyRequestsException() {
    }

    public PeppermintApiTooManyRequestsException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiTooManyRequestsException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiTooManyRequestsException(Throwable throwable) {
        super(throwable);
    }
}
