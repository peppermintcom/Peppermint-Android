package com.peppermint.app.rest;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 */
public class HttpWrappedResponse<T> extends HttpResponse {

    private static final String TAG = HttpWrappedResponse.class.getSimpleName();

    protected HttpWrappedResponseBody<T> mBody;
    protected HttpWrappedResponseBodyJSONParser<T> mBodyParser;

    public HttpWrappedResponse(JSONParser<T> dataParser) {
        super();
        this.mBodyParser = new HttpWrappedResponseBodyJSONParser<>(dataParser);
    }

    @Override
    public void readBody(InputStream istream, HttpRequest request) throws IOException {
        super.readBody(istream, request);
        String jsonStr = super.getBody().toString();

        try {
            mBody = mBodyParser.processJson(new JSONObject(jsonStr));
        } catch (Exception e) {
            mBody = null;
            throw new RuntimeException("Error parsing wrapped response from server.", e);
        }
    }

    @Override
    public Object getBody() {
        return mBody;
    }
}
