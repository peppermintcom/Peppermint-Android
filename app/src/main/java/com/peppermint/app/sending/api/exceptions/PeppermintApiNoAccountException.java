package com.peppermint.app.sending.api.exceptions;

import com.peppermint.app.sending.exceptions.ElectableForQueueingException;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiNoAccountException extends PeppermintApiException implements ElectableForQueueingException {

    public PeppermintApiNoAccountException() {
    }

    public PeppermintApiNoAccountException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiNoAccountException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiNoAccountException(Throwable throwable) {
        super(throwable);
    }
}
