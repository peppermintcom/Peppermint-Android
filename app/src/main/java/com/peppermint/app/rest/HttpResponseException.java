package com.peppermint.app.rest;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class HttpResponseException extends RuntimeException {
    public HttpResponseException() {
    }

    public HttpResponseException(String detailMessage) {
        super(detailMessage);
    }

    public HttpResponseException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public HttpResponseException(Throwable throwable) {
        super(throwable);
    }
}
