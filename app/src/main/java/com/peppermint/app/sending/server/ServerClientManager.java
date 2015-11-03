package com.peppermint.app.sending.server;

import android.content.Context;
import android.os.ResultReceiver;
import android.util.Log;

import com.peppermint.app.rest.HttpClientManager;
import com.peppermint.app.rest.HttpRequest;
import com.peppermint.app.rest.HttpRequestFileData;
import com.peppermint.app.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 28-10-2015.
 */
public class ServerClientManager extends HttpClientManager {

    private static final String TAG = ServerClientManager.class.getSimpleName();

    private static final String API_KEY = "abc123";
    protected static final String BASE_ENDPOINT_URL = "https://qdkkavugcd.execute-api.us-west-2.amazonaws.com/prod/v1/";

    protected static final String RECORD_ENDPOINT = BASE_ENDPOINT_URL + "record";
    protected static final String RECORDER_ENDPOINT = BASE_ENDPOINT_URL + "recorder";
    protected static final String RECORDER_TOKEN_ENDPOINT = BASE_ENDPOINT_URL + "recorder-token";
    protected static final String UPLOADS_ENDPOINT = BASE_ENDPOINT_URL + "uploads";

    protected String mAccessToken = null;

    public ServerClientManager(Context context) {
        super(context);
    }

    // --- RECORDER
    public UUID authenticate(String clientId, String clientKey) {
        return performRequest(RECORDER_TOKEN_ENDPOINT, HttpRequest.METHOD_POST, null, null, "Basic " + Utils.getBasicAuthenticationToken(clientId, clientKey));
    }

    public UUID register(String clientId, String clientKey, String description) {

        /*try {
            JSONObject obj = new JSONObject();
            obj.put("api_key", API_KEY);
            JSONObject recorderObj = new JSONObject();
            recorderObj.put("description", description);
            recorderObj.put("recorder_client_id", clientId);
            recorderObj.put("recorder_key", clientKey);
            obj.put("recorder", recorderObj);

            return performRequest(RECORDER_ENDPOINT, HttpRequest.METHOD_POST, obj.toString(), null, null);
        } catch (JSONException e) {
            Log.e(TAG, "Error generating request JSON!", e);
        }*/

        return performRequest(RECORDER_ENDPOINT, HttpRequest.METHOD_POST, "{ \"api_key\": \"" + API_KEY + "\", \"recorder\": { \"description\": \"" + description +
                "\", \"recorder_client_id\": \"" + clientId + "\", \"recorder_key\": \"" + clientKey + "\" } }", null, null);
    }

    // --- MESSAGE UPLOAD
    public UUID finishUpload(String signedUrl) {
        return performRequest(RECORD_ENDPOINT, HttpRequest.METHOD_POST, "{ \"signed_url\": \"" + signedUrl + "\" }", null, "Bearer " + mAccessToken);
    }

    public UUID performUpload(String signedUrl, File fileToSend, String contentType, ResultReceiver listener) {
        HttpRequestFileData request = new HttpRequestFileData(signedUrl, HttpRequest.METHOD_PUT, false, fileToSend);
        request.setHeaderParam("Content-Type", contentType);
        request.removeHeaderParam("Accept");
        request.removeUrlParam("_ts");
        request.setUploadResultReceiver(listener);
        return performRequest(request);
    }

    public UUID startUpload(String contentType) {
        return performRequest(UPLOADS_ENDPOINT, HttpRequest.METHOD_POST, "{\"content_type\":\"" + contentType + "\"}", null, "Bearer " + mAccessToken);
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String mAccessToken) {
        this.mAccessToken = mAccessToken;
    }

}
