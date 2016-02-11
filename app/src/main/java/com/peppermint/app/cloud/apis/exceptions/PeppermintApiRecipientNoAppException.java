package com.peppermint.app.cloud.apis.exceptions;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiRecipientNoAppException extends PeppermintApiException {

    public PeppermintApiRecipientNoAppException() {
    }

    public PeppermintApiRecipientNoAppException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiRecipientNoAppException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiRecipientNoAppException(Throwable throwable) {
        super(throwable);
    }
}
