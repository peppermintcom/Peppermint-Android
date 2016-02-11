package com.peppermint.app.cloud.apis.exceptions;

/**
 * Created by Nuno Luz on 25-01-2016.
 */
public class PeppermintApiException extends Exception {
    public PeppermintApiException() {
    }

    public PeppermintApiException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiException(Throwable throwable) {
        super(throwable);
    }
}
