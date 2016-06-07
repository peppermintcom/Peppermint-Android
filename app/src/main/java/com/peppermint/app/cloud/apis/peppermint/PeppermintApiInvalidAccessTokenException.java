package com.peppermint.app.cloud.apis.peppermint;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiInvalidAccessTokenException extends PeppermintApiResponseException {

    public PeppermintApiInvalidAccessTokenException() {
    }

    public PeppermintApiInvalidAccessTokenException(int code) {
        super(code);
    }

    public PeppermintApiInvalidAccessTokenException(int code, String detailMessage) {
        super(code, detailMessage);
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
