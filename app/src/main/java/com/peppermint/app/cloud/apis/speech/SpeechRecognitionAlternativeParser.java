package com.peppermint.app.cloud.apis.speech;

import com.peppermint.app.cloud.rest.JSONParser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Parser for {@link SpeechRecognitionAlternative}.
 */
public class SpeechRecognitionAlternativeParser implements JSONParser<SpeechRecognitionAlternative> {

    public SpeechRecognitionAlternativeParser() {
    }

    @Override
    public SpeechRecognitionAlternative processJson(JSONObject obj) throws JSONException {
        SpeechRecognitionAlternative response = new SpeechRecognitionAlternative();

        if(!obj.isNull("transcript")) {
            response.setTranscript(obj.getString("transcript"));
        }
        if(!obj.isNull("confidence")) {
            response.setConfidence((float) obj.getDouble("confidence"));
        }

        return response;
    }

    @Override
    public JSONObject toJson(SpeechRecognitionAlternative inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
