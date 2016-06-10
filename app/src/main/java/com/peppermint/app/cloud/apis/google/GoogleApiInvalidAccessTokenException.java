package com.peppermint.app.cloud.apis.google;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

public class GoogleApiInvalidAccessTokenException extends GoogleApiResponseException {
    public GoogleApiInvalidAccessTokenException() { super(); }
    public GoogleApiInvalidAccessTokenException(String detailMessage) {
        super(detailMessage);
    }
}
