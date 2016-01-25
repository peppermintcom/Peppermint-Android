package com.peppermint.app.sending.api;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.telephony.TelephonyManager;

import com.peppermint.app.rest.HttpRequest;
import com.peppermint.app.rest.HttpRequestFileData;
import com.peppermint.app.rest.HttpResponse;
import com.peppermint.app.sending.api.exceptions.PeppermintApiAlreadyRegisteredException;
import com.peppermint.app.sending.api.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.sending.api.exceptions.PeppermintApiResponseCodeException;
import com.peppermint.app.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 22-01-2016.
 */
public class PeppermintApi implements Serializable {

    private static final String API_KEY = "abc123";
    protected static final String BASE_ENDPOINT_URL = "https://qdkkavugcd.execute-api.us-west-2.amazonaws.com/prod/v1/";

    protected static final String RECORD_ENDPOINT = BASE_ENDPOINT_URL + "record";
    protected static final String RECORDER_ENDPOINT = BASE_ENDPOINT_URL + "recorder";
    protected static final String RECORDER_TOKEN_ENDPOINT = BASE_ENDPOINT_URL + "recorder-token";
    protected static final String UPLOADS_ENDPOINT = BASE_ENDPOINT_URL + "uploads";

    public class JSONObjectItemResponse extends HttpResponse {
        private String mKey;
        private String[] mKeySet;
        private Map<String, Object> mValues;
        public JSONObjectItemResponse(String key) {
            this.mKey = key;
        }
        public JSONObjectItemResponse(String... keys) { this.mKeySet = keys; mValues = new HashMap<>(); }
        @Override
        public void readBody(InputStream inStream, HttpRequest request) throws Throwable {
            super.readBody(inStream, request);

            if(getCode() / 100 != 2) {
                return;
            }

            JSONObject obj = new JSONObject((String) getBody());
            if(mKeySet != null) {
                for(String key : mKeySet) {
                    mValues.put(key, obj.get(key));
                }
                setBody(mValues);
            } else {
                setBody(obj.getString(mKey));
            }
        }
        public Object getBody(String key) {
            if(mValues == null || !mValues.containsKey(key)) {
                return null;
            }
            return mValues.get(key);
        }
        public final Creator<JSONObjectItemResponse> CREATOR = new Creator<JSONObjectItemResponse>() {
            public JSONObjectItemResponse createFromParcel(Parcel in) {
                return new JSONObjectItemResponse(in);
            }
            public JSONObjectItemResponse[] newArray(int size) {
                return new JSONObjectItemResponse[size];
            }
        };
        protected JSONObjectItemResponse(Parcel in) {
            super(in);
        }
    }

    private Context mContext;
    private String mAccessToken;

    public PeppermintApi(Context context) {
        this.mContext = context;
    }

    // RECORDER
    public JSONObjectItemResponse authRecorder(String recorderId, String recorderKey) throws PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException {
        HttpRequest request = new HttpRequest(RECORDER_TOKEN_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Basic " + Utils.getBasicAuthenticationToken(recorderId, recorderKey));
        JSONObjectItemResponse response = new JSONObjectItemResponse("at");
        request.execute(response);

        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(request.getBody());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.getBody());
        }

        setAccessToken((String) response.getBody());
        return response;
    }

    public JSONObjectItemResponse registerRecorder(String recorderId, String recorderKey, String description) throws PeppermintApiInvalidAccessTokenException, PeppermintApiResponseCodeException, PeppermintApiAlreadyRegisteredException {
        HttpRequest request = new HttpRequest(RECORDER_ENDPOINT, HttpRequest.METHOD_POST);
        request.setBody("{ \"api_key\": \"" + API_KEY + "\", \"recorder\": { \"description\": \"" + description +
                "\", \"recorder_client_id\": \"" + recorderId + "\", \"recorder_key\": \"" + recorderKey + "\" } }");
        JSONObjectItemResponse response = new JSONObjectItemResponse("at");
        request.execute(response);

        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(request.getBody());
        }
        if(response.getCode() == 409) {
            throw new PeppermintApiAlreadyRegisteredException(request.getBody());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.getBody());
        }

        setAccessToken((String) response.getBody());
        return response;
    }

    // UPLOADS
    public JSONObjectItemResponse getSignedUrl(String senderName, String senderEmail, String contentType) throws PeppermintApiInvalidAccessTokenException, PeppermintApiResponseCodeException {
        HttpRequest request = new HttpRequest(UPLOADS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setBody("{\"content_type\":\"" + contentType + "\", \"sender_name\":\"" + senderName + "\", \"sender_email\":\"" + senderEmail + "\"}");
        JSONObjectItemResponse response = new JSONObjectItemResponse("signed_url");
        request.execute(response);

        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(request.getBody());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.getBody());
        }

        return response;
    }

    public HttpResponse uploadMessage(String signedUrl, File audioFile, String contentType) {
        HttpRequestFileData request = new HttpRequestFileData(signedUrl, HttpRequest.METHOD_PUT, false, audioFile);
        request.setHeaderParam("Content-Type", contentType);
        request.removeHeaderParam("Accept");
        request.removeUrlParam("_ts");
        request.setUploadResultReceiver(null);  // for now, no progress
        HttpResponse response = new HttpResponse();
        request.execute(response);
        return response;
    }

    public JSONObjectItemResponse confirmUploadMessage(String signedUrl) {
        HttpRequest request = new HttpRequest(RECORD_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setBody("{ \"signed_url\": \"" + signedUrl + "\" }");
        JSONObjectItemResponse response = new JSONObjectItemResponse("short_url", "canonical_url");
        request.execute(response);
        return response;
    }

    protected boolean setAccessToken(JSONObjectItemResponse response) {
        if(response.getException() != null && response.getBody() != null && (response.getCode()/100) == 2) {
            setAccessToken((String) response.getBody());
            return true;
        }
        return false;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String at) {
        this.mAccessToken = at;
    }

    /**
     * Gets the keys that uniquely identify the Android device
     * @return the unique identifier of the device at position 0; its serial at position 1
     */
    public static String[] getKeys(Context context) {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = String.valueOf(tm.getDeviceId());
        tmSerial = String.valueOf(tm.getSimSerialNumber());
        androidId = String.valueOf(android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();

        return new String[]{ deviceId, String.valueOf(Build.SERIAL) };
    }

}
