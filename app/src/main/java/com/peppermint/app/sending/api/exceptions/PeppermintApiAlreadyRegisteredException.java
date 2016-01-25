package com.peppermint.app.sending.api.exceptions;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiAlreadyRegisteredException extends PeppermintApiException {

    public PeppermintApiAlreadyRegisteredException() {
    }

    public PeppermintApiAlreadyRegisteredException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiAlreadyRegisteredException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiAlreadyRegisteredException(Throwable throwable) {
        super(throwable);
    }
}
