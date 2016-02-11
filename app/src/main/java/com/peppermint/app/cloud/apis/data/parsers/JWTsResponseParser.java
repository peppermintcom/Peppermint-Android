package com.peppermint.app.cloud.apis.data.parsers;

import com.peppermint.app.cloud.apis.data.AccountsResponse;
import com.peppermint.app.cloud.apis.data.JWTsResponse;
import com.peppermint.app.cloud.apis.data.RecorderResponse;
import com.peppermint.app.cloud.rest.JSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Parser for {@link JWTsResponse}.
 */
public class JWTsResponseParser implements JSONParser<JWTsResponse> {

    @Override
    public JWTsResponse processJson(JSONObject obj) throws JSONException {
        JWTsResponse response = new JWTsResponse();

        JSONObject tokenData = obj.getJSONObject("data").getJSONObject("attributes");
        response.setAccessToken(tokenData.getString("token"));

        JSONArray data = obj.getJSONArray("included");

        for(int i=0; i<data.length(); i++) {
            JSONObject el = data.getJSONObject(i);
            String type = el.getString("type");
            if(type != null) {
                if(type.compareToIgnoreCase("recorders") == 0) {
                    RecorderResponse recorder = new RecorderResponse();
                    recorder.setRecorderId(el.getString("id"));
                    JSONObject attrObject = el.getJSONObject("attributes");

                    if(!attrObject.isNull("recorder_key")) {
                        recorder.setRecorderKey(attrObject.getString("recorder_key"));
                    }
                    recorder.setRecorderClientId(attrObject.getString("recorder_client_id"));
                    if(!attrObject.isNull("description")) {
                        recorder.setDescription(attrObject.getString("description"));
                    }
                    recorder.setRegistrationTimestamp(attrObject.getString("recorder_ts"));
                    if(!attrObject.isNull("gcm_registration_token")) {
                        recorder.setGcmRegistationToken(attrObject.getString("gcm_registration_token"));
                    }
                    if(!attrObject.isNull("description")) {
                        recorder.setDescription(attrObject.getString("description"));
                    }

                    response.addRecorder(recorder);
                } else if (type.compareToIgnoreCase("accounts") == 0) {
                    AccountsResponse account = new AccountsResponse();
                    account.setAccountId(el.getString("id"));

                    JSONObject attrObject = el.getJSONObject("attributes");
                    account.setEmail(attrObject.getString("email"));
                    account.setFullName(attrObject.getString("full_name"));
                    account.setRegistrationTimestamp(attrObject.getString("registration_ts"));
                    account.setVerified(attrObject.getBoolean("is_verified"));

                    response.addAccount(account);
                }
            }
        }

        return response;
    }

    @Override
    public JSONObject toJson(JWTsResponse inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
