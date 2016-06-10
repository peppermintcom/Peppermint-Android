package com.peppermint.app.cloud.apis.speech.parsers;

import com.peppermint.app.cloud.apis.speech.objects.SpeechRecognitionAlternative;
import com.peppermint.app.cloud.apis.speech.objects.SpeechRecognitionResult;
import com.peppermint.app.cloud.rest.JSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Parser for {@link SpeechRecognitionAlternative}.
 */
public class SpeechRecognitionResultParser implements JSONParser<SpeechRecognitionResult> {

    private final SpeechRecognitionAlternativeParser mAlternativeParser = new SpeechRecognitionAlternativeParser();

    public SpeechRecognitionResultParser() {
    }

    @Override
    public SpeechRecognitionResult processJson(JSONObject obj) throws JSONException {
        SpeechRecognitionResult response = new SpeechRecognitionResult();

        if(!obj.isNull("isFinal")) {
            response.setFinal(obj.getBoolean("isFinal"));
        }
        if(!obj.isNull("stability")) {
            response.setStability((float) obj.getDouble("stability"));
        }
        if(!obj.isNull("alternatives")) {
            final JSONArray arr = obj.getJSONArray("alternatives");
            for(int i=0; i<arr.length(); i++) {
                final JSONObject alternativeJson = arr.getJSONObject(i);
                final SpeechRecognitionAlternative alternative = mAlternativeParser.processJson(alternativeJson);
                response.addAlternative(alternative);
            }
        }
        
        return response;
    }

    @Override
    public JSONObject toJson(SpeechRecognitionResult inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
