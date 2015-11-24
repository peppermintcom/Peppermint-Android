package com.peppermint.app.sending.mail.gmail;

/**
 * Created by Nuno Luz on 19-11-2015.
 */
public class GmailResponseException extends Exception {
    public GmailResponseException() {
    }

    public GmailResponseException(String detailMessage) {
        super(detailMessage);
    }

    public GmailResponseException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public GmailResponseException(Throwable throwable) {
        super(throwable);
    }
}
