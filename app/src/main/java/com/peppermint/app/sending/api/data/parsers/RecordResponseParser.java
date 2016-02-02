package com.peppermint.app.sending.api.data.parsers;

import com.peppermint.app.rest.JSONParser;
import com.peppermint.app.sending.api.data.RecordResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class RecordResponseParser implements JSONParser<RecordResponse> {
    @Override
    public RecordResponse processJson(JSONObject obj) throws JSONException {
        RecordResponse response = new RecordResponse();
        response.setCanonicalUrl(obj.getString("canonical_url"));
        response.setShortUrl(obj.getString("short_url"));
        return response;
    }

    @Override
    public JSONObject toJson(RecordResponse inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
