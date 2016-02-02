package com.peppermint.app.sending.api.data.parsers;

import com.peppermint.app.rest.JSONParser;
import com.peppermint.app.sending.api.data.UploadsResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class UploadsResponseParser implements JSONParser<UploadsResponse> {
    @Override
    public UploadsResponse processJson(JSONObject obj) throws JSONException {
        UploadsResponse response = new UploadsResponse();
        response.setSignedUrl(obj.getString("signed_url"));
        response.setCanonicalUrl(obj.getString("canonical_url"));
        response.setShortUrl(obj.getString("short_url"));
        return response;
    }

    @Override
    public JSONObject toJson(UploadsResponse inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
