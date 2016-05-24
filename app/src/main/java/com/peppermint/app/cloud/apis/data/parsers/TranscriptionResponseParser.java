package com.peppermint.app.cloud.apis.data.parsers;

import com.peppermint.app.cloud.apis.data.TranscriptionResponse;
import com.peppermint.app.cloud.rest.JSONParser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 23-05-2016.
 *
 * Parser for {@link TranscriptionResponse}.
 */
public class TranscriptionResponseParser implements JSONParser<TranscriptionResponse> {

    public TranscriptionResponseParser() {
    }

    @Override
    public TranscriptionResponse processJson(JSONObject obj) throws JSONException {
        TranscriptionResponse response = new TranscriptionResponse();

        if(!obj.isNull("audio_url")) {
            response.setAudioUrl(obj.getString("audio_url"));
        }

        if(!obj.isNull("text")) {
            response.setTranscription(obj.getString("text"));
        }

        if(!obj.isNull("transcription_url")) {
            response.setTranscriptionUrl(obj.getString("transcription_url"));
        }

        if(!obj.isNull("timestamp")) {
            response.setTimestamp(obj.getString("timestamp"));
        }

        if(!obj.isNull("language")) {
            response.setLanguageTag(obj.getString("language"));
        }

        if(!obj.isNull("ip_address")) {
            response.setIp(obj.getString("ip_address"));
        }

        if(!obj.isNull("confidence")) {
            response.setConfidence((float) obj.getDouble("confidence"));
        }

        return response;
    }

    @Override
    public JSONObject toJson(TranscriptionResponse inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
