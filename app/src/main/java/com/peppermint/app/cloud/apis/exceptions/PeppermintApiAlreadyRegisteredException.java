package com.peppermint.app.cloud.apis.exceptions;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiAlreadyRegisteredException extends PeppermintApiResponseException {

    public PeppermintApiAlreadyRegisteredException() {
    }

    public PeppermintApiAlreadyRegisteredException(int code) {
        super(code);
    }

    public PeppermintApiAlreadyRegisteredException(int code, String detailMessage) {
        super(code, detailMessage);
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
