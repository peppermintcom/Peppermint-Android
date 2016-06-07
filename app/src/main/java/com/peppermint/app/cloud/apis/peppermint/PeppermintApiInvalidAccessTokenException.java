package com.peppermint.app.cloud.apis.peppermint;

public class PeppermintApiInvalidAccessTokenException extends PeppermintApiResponseException {

    public PeppermintApiInvalidAccessTokenException() {
        super();
    }

    public PeppermintApiInvalidAccessTokenException(int code, String detailMessage) {
        super(code, detailMessage);
    }

    public PeppermintApiInvalidAccessTokenException(String detailMessage) {
        super(detailMessage);
    }
}
