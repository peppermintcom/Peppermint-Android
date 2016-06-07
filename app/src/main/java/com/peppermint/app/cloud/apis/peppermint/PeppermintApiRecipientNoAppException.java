package com.peppermint.app.cloud.apis.peppermint;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiRecipientNoAppException extends PeppermintApiResponseException {

    public PeppermintApiRecipientNoAppException() {
    }

    public PeppermintApiRecipientNoAppException(int code) {
        super(code);
    }

    public PeppermintApiRecipientNoAppException(int code, String detailMessage) {
        super(code, detailMessage);
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
