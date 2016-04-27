package com.peppermint.app.cloud.apis.exceptions;

import com.peppermint.app.cloud.senders.exceptions.ElectableForQueueingException;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiRecipientNoAppException extends PeppermintApiException implements ElectableForQueueingException {

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
