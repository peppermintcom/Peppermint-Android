package com.peppermint.app.cloud.apis.peppermint.parsers;

import com.peppermint.app.cloud.apis.peppermint.objects.MessagesResponse;
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

        JSONObject data = obj.isNull("data") ? obj : obj.getJSONObject("data");

        response.setMessageId(data.getString("id"));

        JSONObject attrData = data.getJSONObject("attributes");

        if(!attrData.isNull("audio_url")) {
            response.setAudioUrl(attrData.getString("audio_url"));
        }

        if(!attrData.isNull("transcription")) {
            response.setTranscription(attrData.getString("transcription"));
        }

        if(!attrData.isNull("read")) {
            response.setReadTimestamp(attrData.getString("read"));
        }

        if(!attrData.isNull("sender_name")) {
            response.setSenderName(attrData.getString("sender_name"));
        }

        if(!attrData.isNull("duration")) {
            response.setDuration(attrData.getInt("duration"));
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
