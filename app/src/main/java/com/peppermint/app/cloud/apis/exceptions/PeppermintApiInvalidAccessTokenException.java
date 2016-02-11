package com.peppermint.app.cloud.apis.exceptions;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiInvalidAccessTokenException extends PeppermintApiException {

    public PeppermintApiInvalidAccessTokenException() {
    }

    public PeppermintApiInvalidAccessTokenException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiInvalidAccessTokenException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiInvalidAccessTokenException(Throwable throwable) {
        super(throwable);
    }
}
