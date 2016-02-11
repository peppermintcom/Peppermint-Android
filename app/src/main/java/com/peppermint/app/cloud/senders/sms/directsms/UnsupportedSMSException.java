package com.peppermint.app.cloud.senders.sms.directsms;

/**
 * Created by Nuno Luz on 28-10-2015.
 *
 * Exception thrown the sending SMSs is not supported by the device.
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
