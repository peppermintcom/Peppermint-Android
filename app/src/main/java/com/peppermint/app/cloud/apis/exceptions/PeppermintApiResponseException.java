package com.peppermint.app.cloud.apis.exceptions;

import com.peppermint.app.cloud.rest.HttpResponseException;

/**
 * Created by Nuno Luz on 19-04-2016.
 */
public class PeppermintApiResponseException extends HttpResponseException {
    public PeppermintApiResponseException() {
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
