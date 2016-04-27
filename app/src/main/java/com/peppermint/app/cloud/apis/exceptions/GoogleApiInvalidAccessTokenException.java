package com.peppermint.app.cloud.apis.exceptions;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.peppermint.app.cloud.senders.exceptions.ElectableForQueueingException;

/**
 * Created by Nuno Luz on 25-01-2016.
 */
public class GoogleApiInvalidAccessTokenException extends Exception implements ElectableForQueueingException {

    public GoogleApiInvalidAccessTokenException() {
    }

    public GoogleApiInvalidAccessTokenException(GoogleJsonResponseException e) {
        super(e.getMessage(), e);
    }

    public GoogleApiInvalidAccessTokenException(String detailMessage) {
        super(detailMessage);
    }

    public GoogleApiInvalidAccessTokenException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public GoogleApiInvalidAccessTokenException(Throwable throwable) {
        super(throwable);
    }
}
