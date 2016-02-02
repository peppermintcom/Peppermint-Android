package com.peppermint.app.sending.api.data.parsers;

import com.peppermint.app.rest.JSONParser;
import com.peppermint.app.sending.api.data.MessagesResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class MessagesResponseParser implements JSONParser<MessagesResponse> {

    public MessagesResponseParser() {
    }

    @Override
    public MessagesResponse processJson(JSONObject obj) throws JSONException {
        MessagesResponse response = new MessagesResponse();

        JSONObject jsonData = obj.getJSONObject("application/vnd.api+json");
        response.setMessageId(jsonData.getString("id"));

        JSONObject attrData = jsonData.getJSONObject("attributes");
        response.setAudioUrl(attrData.getString("audio_url"));
        response.setTranscriptionUrl(attrData.getString("transcription_url"));
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
