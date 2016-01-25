package com.peppermint.app.sending.api.exceptions;

/**
 * Created by Nuno Luz on 25-01-2016.
 */
public class PeppermintApiResponseCodeException extends PeppermintApiException {
    public PeppermintApiResponseCodeException() {
    }

    public PeppermintApiResponseCodeException(int code, String detailMessage) {
        super("Code " + code + ": " + detailMessage);
    }

    public PeppermintApiResponseCodeException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiResponseCodeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiResponseCodeException(Throwable throwable) {
        super(throwable);
    }
}
