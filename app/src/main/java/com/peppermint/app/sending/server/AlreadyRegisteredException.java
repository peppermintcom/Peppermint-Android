package com.peppermint.app.sending.server;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class AlreadyRegisteredException extends RuntimeException {
    public AlreadyRegisteredException() {
    }

    public AlreadyRegisteredException(String detailMessage) {
        super(detailMessage);
    }

    public AlreadyRegisteredException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public AlreadyRegisteredException(Throwable throwable) {
        super(throwable);
    }
}
