package com.peppermint.app.cloud.apis.peppermint;

import com.peppermint.app.cloud.rest.HttpResponseException;

public class PeppermintApiResponseException extends HttpResponseException {
    public PeppermintApiResponseException() {
    }

    public PeppermintApiResponseException(int code) {
        super(code);
    }

    public PeppermintApiResponseException(int code, String detailMessage) {
        super(code, detailMessage);
    }

    public PeppermintApiResponseException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiResponseException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiResponseException(Throwable throwable) {
        super(throwable);
    }
}
