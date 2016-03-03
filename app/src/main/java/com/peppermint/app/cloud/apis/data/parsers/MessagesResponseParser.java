package com.peppermint.app.cloud.apis.data.parsers;

import com.peppermint.app.cloud.apis.data.MessagesResponse;
import com.peppermint.app.cloud.rest.JSONParser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Parser for {@link MessagesResponse}.
 */
public class MessagesResponseParser implements JSONParser<MessagesResponse> {

    public MessagesResponseParser() {
    }

    @Override
    public MessagesResponse processJson(JSONObject obj) throws JSONException {
        MessagesResponse response = new MessagesResponse();

        JSONObject data = obj.getJSONObject("data");

        response.setMessageId(data.getString("id"));

        JSONObject attrData = data.getJSONObject("attributes");
        if(!attrData.isNull("audio_url")) {
            response.setAudioUrl(attrData.getString("audio_url"));
        }
        if(!attrData.isNull("transcription_url")) {
            response.setTranscriptionUrl(attrData.getString("transcription_url"));
        }
        response.setSenderEmail(attrData.getString("sender_email"));
        response.setRecipientEmail(attrData.getString("recipient_email"));
        response.setCreatedTimestamp(attrData.getString("created"));

        return response;
    }

    @Override
    public JSONObject toJson(MessagesResponse inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
