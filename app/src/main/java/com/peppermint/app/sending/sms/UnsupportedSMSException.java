package com.peppermint.app.sending.sms;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class UnsupportedSMSException extends RuntimeException {
    public UnsupportedSMSException() {
    }

    public UnsupportedSMSException(String detailMessage) {
        super(detailMessage);
    }

    public UnsupportedSMSException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UnsupportedSMSException(Throwable throwable) {
        super(throwable);
    }
}
