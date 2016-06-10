package com.peppermint.app.cloud.apis.sparkpost;

import com.peppermint.app.cloud.rest.HttpResponseException;

public class SparkPostApiResponseException extends HttpResponseException {
    public SparkPostApiResponseException() {
        super();
    }

    public SparkPostApiResponseException(int code, String detailMessage) {
        super(code, detailMessage);
    }
}
