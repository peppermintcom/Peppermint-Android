package com.peppermint.app.cloud.apis.exceptions;

import com.peppermint.app.cloud.senders.exceptions.ElectableForQueueingException;

/**
 * Created by Nuno Luz on 28-10-2015.
 */
public class PeppermintApiInvalidAccessTokenException extends PeppermintApiException implements ElectableForQueueingException {

    public PeppermintApiInvalidAccessTokenException() {
    }

    public PeppermintApiInvalidAccessTokenException(String detailMessage) {
        super(detailMessage);
    }

    public PeppermintApiInvalidAccessTokenException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PeppermintApiInvalidAccessTokenException(Throwable throwable) {
        super(throwable);
    }
}
