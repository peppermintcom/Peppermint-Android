package com.peppermint.app.sending.api.data.parsers;

import com.peppermint.app.rest.JSONParser;
import com.peppermint.app.sending.api.data.AccountsResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class AccountsResponseParser implements JSONParser<AccountsResponse> {

    private boolean mReadRootOnly = false;

    public AccountsResponseParser() {
    }

    public AccountsResponseParser(boolean mReadRootOnly) {
        this.mReadRootOnly = mReadRootOnly;
    }

    @Override
    public AccountsResponse processJson(JSONObject obj) throws JSONException {
        AccountsResponse response = new AccountsResponse();
        JSONObject userObj = obj;

        if(!mReadRootOnly) {
            response.setAccessToken(obj.getString("at"));
            userObj = obj.getJSONObject("u");
        }

        response.setAccountId(userObj.getString("account_id"));
        response.setEmail(userObj.getString("email"));
        response.setFullName(userObj.getString("full_name"));
        response.setRegistrationTimestamp(userObj.getString("registration_ts"));
        return response;
    }

    @Override
    public JSONObject toJson(AccountsResponse inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
