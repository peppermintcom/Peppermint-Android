package com.peppermint.app.cloud.apis.data.parsers;

import com.peppermint.app.cloud.apis.data.RecordResponse;
import com.peppermint.app.cloud.rest.JSONParser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Parser for {@link RecordResponse}.
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
