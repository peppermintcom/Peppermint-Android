package com.peppermint.app.cloud.apis.exceptions;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiNoAccountException extends Exception {

    public PeppermintApiNoAccountException() {
    }

    public PeppermintApiNoAccountException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiNoAccountException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiNoAccountException(Throwable throwable) {
        super(throwable);
    }
}
