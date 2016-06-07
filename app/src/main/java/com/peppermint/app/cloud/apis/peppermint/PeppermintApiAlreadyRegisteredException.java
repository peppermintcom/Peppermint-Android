package com.peppermint.app.cloud.apis.peppermint;

public class PeppermintApiAlreadyRegisteredException extends PeppermintApiResponseException {

    public PeppermintApiAlreadyRegisteredException() {
        super();
    }

    public PeppermintApiAlreadyRegisteredException(int code, String detailMessage) {
        super(code, detailMessage);
    }
}
