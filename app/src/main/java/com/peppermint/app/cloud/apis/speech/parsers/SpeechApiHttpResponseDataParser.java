package com.peppermint.app.cloud.apis.speech.parsers;

import com.peppermint.app.cloud.apis.speech.objects.RecognizeResponse;
import com.peppermint.app.cloud.apis.speech.objects.SpeechApiHttpResponseData;
import com.peppermint.app.cloud.apis.speech.objects.SpeechRecognitionAlternative;
import com.peppermint.app.cloud.rest.JSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Parser for {@link SpeechRecognitionAlternative}.
 */
public class SpeechApiHttpResponseDataParser implements JSONParser<SpeechApiHttpResponseData> {

    private final RecognizeResponseParser mResponseParser = new RecognizeResponseParser();

    public SpeechApiHttpResponseDataParser() {
    }

    @Override
    public SpeechApiHttpResponseData processJson(JSONObject obj) throws JSONException {
        SpeechApiHttpResponseData httpResponse = new SpeechApiHttpResponseData();

        if(!obj.isNull("error")) {
            final JSONObject errorJson = obj.getJSONObject("error");
            httpResponse.setErrorCode(errorJson.getInt("code"));
            httpResponse.setErrorMessage(errorJson.getString("message"));
        }

        if(!obj.isNull("responses")) {
            final JSONArray arr = obj.getJSONArray("responses");
            for(int i=0; i<arr.length(); i++) {
                final JSONObject responseJson = arr.getJSONObject(i);
                final RecognizeResponse response = mResponseParser.processJson(responseJson);
                httpResponse.addResponse(response);
            }
        }
        
        return httpResponse;
    }

    @Override
    public JSONObject toJson(SpeechApiHttpResponseData inst) throws JSONException {
        throw new UnsupportedOperationException();
    }
}
