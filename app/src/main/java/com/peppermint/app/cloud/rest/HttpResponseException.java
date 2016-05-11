package com.peppermint.app.cloud.rest;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class HttpResponseException extends RuntimeException {

    protected int mResponseCode = 0;

    public HttpResponseException() {
    }

    public HttpResponseException(int code) {
        super("Code " + code);
        this.mResponseCode = code;
    }

    public HttpResponseException(int code, String detailMessage) {
        super("Code " + code + ": " + detailMessage);
        this.mResponseCode = code;
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

    public int getResponseCode() {
        return mResponseCode;
    }
}
