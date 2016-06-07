package com.peppermint.app.cloud.apis.google;

import com.peppermint.app.cloud.rest.HttpResponseException;

/**
 * Created by Nuno Luz on 19-04-2016.
 */
public class GoogleApiResponseException extends HttpResponseException {
    public GoogleApiResponseException() {
    }

    public GoogleApiResponseException(int code) {
        super(code);
    }

    public GoogleApiResponseException(int code, String detailMessage) {
        super(code, detailMessage);
    }

    public GoogleApiResponseException(String detailMessage) {
        super(detailMessage);
    }

    public GoogleApiResponseException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public GoogleApiResponseException(Throwable throwable) {
        super(throwable);
    }
}
