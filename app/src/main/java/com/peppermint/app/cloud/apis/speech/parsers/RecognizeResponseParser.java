package com.peppermint.app.cloud.apis.speech.parsers;

import com.peppermint.app.cloud.apis.speech.objects.RecognizeResponse;
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
public class RecognizeResponseParser implements JSONParser<RecognizeResponse> {

    private final SpeechRecognitionResultParser mResultParser = new SpeechRecognitionResultParser();

    public RecognizeResponseParser() {
    }

    @Override
    public RecognizeResponse processJson(JSONObject obj) throws JSONException {
        RecognizeResponse response = new RecognizeResponse();

        if(!obj.isNull("error")) {
            final JSONObject errorJson = obj.getJSONObject("error");
            response.setErrorCode(errorJson.getInt("code"));
            response.setErrorMessage(errorJson.getString("message"));
        }

        if(!obj.isNull("resultIndex")) {
            response.setResultIndex(obj.getInt("resultIndex"));
        }
        if(!obj.isNull("endpoint")) {
            response.setEndpoint(obj.getString("endpoint"));
        }

        if(!obj.isNull("results")) {
            final JSONArray arr = obj.getJSONArray("results");
            for(int i=0; i<arr.length(); i++) {
                final JSONObject resultJson = arr.getJSONObject(i);
                final SpeechRecognitionResult result = mResultParser.processJson(resultJson);
                response.addResult(result);
            }
        }
        
        return response;
    }

    @Override
    public JSONObject toJson(RecognizeResponse inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
