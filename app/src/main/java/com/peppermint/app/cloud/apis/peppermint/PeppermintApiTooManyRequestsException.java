package com.peppermint.app.cloud.apis.peppermint;

public class PeppermintApiTooManyRequestsException extends PeppermintApiResponseException {

    public PeppermintApiTooManyRequestsException() {
        super();
    }

    public PeppermintApiTooManyRequestsException(int code, String detailMessage) {
        super(code, detailMessage);
    }
}
