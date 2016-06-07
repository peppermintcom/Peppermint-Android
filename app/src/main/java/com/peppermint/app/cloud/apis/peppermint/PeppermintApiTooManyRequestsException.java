package com.peppermint.app.cloud.apis.peppermint;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class PeppermintApiTooManyRequestsException extends PeppermintApiResponseException {

    public PeppermintApiTooManyRequestsException() {
    }

    public PeppermintApiTooManyRequestsException(int code) {
        super(code);
    }

    public PeppermintApiTooManyRequestsException(int code, String detailMessage) {
        super(code, detailMessage);
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
