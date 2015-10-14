package com.peppermint.app.sending.gmail;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * Exception thrown by the {@link GmailSender} if there are no Google accounts configured on the device.
 */
public class GmailPreferredAccountNotSetException extends Exception {
    public GmailPreferredAccountNotSetException() {
    }

    public GmailPreferredAccountNotSetException(String detailMessage) {
        super(detailMessage);
    }

    public GmailPreferredAccountNotSetException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public GmailPreferredAccountNotSetException(Throwable throwable) {
        super(throwable);
    }
}
