package com.peppermint.app.cloud.senders.sms.directsms;

/**
 * Created by Nuno Luz on 28-10-2015.
 *
 * Exception thrown when an SMS is not sent.
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
