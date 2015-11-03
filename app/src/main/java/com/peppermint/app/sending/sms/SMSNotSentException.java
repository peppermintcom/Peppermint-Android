package com.peppermint.app.sending.sms;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class SMSNotSentException extends RuntimeException {
    public SMSNotSentException() {
    }

    public SMSNotSentException(String detailMessage) {
        super(detailMessage);
    }

    public SMSNotSentException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public SMSNotSentException(Throwable throwable) {
        super(throwable);
    }
}
