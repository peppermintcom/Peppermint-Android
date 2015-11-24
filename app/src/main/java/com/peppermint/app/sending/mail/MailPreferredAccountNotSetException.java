package com.peppermint.app.sending.mail;

import com.peppermint.app.sending.mail.gmail.GmailSender;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * Exception thrown by the {@link GmailSender} if there are no Google accounts configured on the device.
 */
public class MailPreferredAccountNotSetException extends Exception {
    public MailPreferredAccountNotSetException() {
    }

    public MailPreferredAccountNotSetException(String detailMessage) {
        super(detailMessage);
    }

    public MailPreferredAccountNotSetException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public MailPreferredAccountNotSetException(Throwable throwable) {
        super(throwable);
    }
}
