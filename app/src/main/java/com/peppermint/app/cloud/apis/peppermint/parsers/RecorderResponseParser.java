package com.peppermint.app.cloud.apis.peppermint.parsers;

import com.peppermint.app.cloud.apis.peppermint.objects.RecorderResponse;
import com.peppermint.app.cloud.rest.JSONParser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Parser for {@link RecorderResponse}.
 */
public class RecorderResponseParser implements JSONParser<RecorderResponse> {

    private boolean mReadRootOnly = false;

    public RecorderResponseParser() {
    }

    public RecorderResponseParser(boolean mReadRootOnly) {
        this.mReadRootOnly = mReadRootOnly;
    }

    @Override
    public RecorderResponse processJson(JSONObject obj) throws JSONException {
        RecorderResponse response = new RecorderResponse();
        JSONObject userObj = obj;

        if(!mReadRootOnly) {
            response.setAccessToken(obj.getString("at"));
            userObj = obj.getJSONObject("recorder");
        }

        if(!userObj.isNull("recorder_id")) {
            response.setRecorderId(userObj.getString("recorder_id"));
        }

        if(!userObj.isNull("recorder_key")) {
            response.setRecorderKey(userObj.getString("recorder_key"));
        }
        response.setRecorderClientId(userObj.getString("recorder_client_id"));
        if(!userObj.isNull("description")) {
            response.setDescription(userObj.getString("description"));
        }
        response.setRegistrationTimestamp(userObj.getString("recorder_ts"));
        if(!userObj.isNull("gcm_registration_token")) {
            response.setGcmRegistationToken(userObj.getString("gcm_registration_token"));
        }

        if(!userObj.isNull("description")) {
            response.setDescription(userObj.getString("description"));
        }

        return response;
    }

    @Override
    public JSONObject toJson(RecorderResponse inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
