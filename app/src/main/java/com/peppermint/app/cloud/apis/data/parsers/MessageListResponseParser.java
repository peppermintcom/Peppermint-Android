package com.peppermint.app.cloud.apis.data.parsers;

import com.peppermint.app.cloud.apis.data.MessageListResponse;
import com.peppermint.app.cloud.apis.data.MessagesResponse;
import com.peppermint.app.cloud.rest.JSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Parser for {@link MessagesResponse}.
 */
public class MessageListResponseParser implements JSONParser<MessageListResponse> {

    private MessagesResponseParser mMessagesParser = new MessagesResponseParser();

    public MessageListResponseParser() {
    }

    @Override
    public MessageListResponse processJson(JSONObject obj) throws JSONException {
        MessageListResponse response = new MessageListResponse();

        if(!obj.isNull("links")) {
            JSONObject links = obj.getJSONObject("links");
            if(!links.isNull("next")) {
                response.setNextUrl(links.getString("next"));
            }
        }

        JSONArray dataList = obj.getJSONArray("data");
        for(int i=0; i<dataList.length(); i++) {
            response.addMessage(mMessagesParser.processJson(dataList.getJSONObject(i)));
        }

        return response;
    }

    @Override
    public JSONObject toJson(MessageListResponse inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
