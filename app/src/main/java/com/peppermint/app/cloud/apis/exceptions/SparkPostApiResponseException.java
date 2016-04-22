package com.peppermint.app.cloud.apis.exceptions;

import com.peppermint.app.cloud.rest.HttpResponseException;

/**
 * Created by Nuno Luz on 19-04-2016.
 */
public class SparkPostApiResponseException extends HttpResponseException {
    public SparkPostApiResponseException() {
    }

    public SparkPostApiResponseException(int code, String detailMessage) {
        super(code, detailMessage);
    }

    public SparkPostApiResponseException(String detailMessage) {
        super(detailMessage);
    }

    public SparkPostApiResponseException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public SparkPostApiResponseException(Throwable throwable) {
        super(throwable);
    }
}
