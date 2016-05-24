package com.peppermint.app.cloud.apis.speech;

import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 18-05-2016.
 */
public class GoogleSpeechRecognition {

    private static final String TAG = GoogleSpeechRecognition.class.getSimpleName();
    private static final String ENDPOINT = "https://speech.googleapis.com/v1/speech:recognize?key=AIzaSyDHIurMNCMvOsuhWtVngBx3CdxasALDSjc";

    private HttpURLConnection mConnection;
    private OutputStream mOutputStream;
    private Base64OutputStream mBase64OutputStream;
    private Thread mReceiverThread;

    private boolean mStop = false;
    private boolean mFinished = true;
    private Throwable mError;

    private int mHttpResponseCode;
    private List<RecognizeResponse> mResponses = new ArrayList<>();
    private final RecognizeResponseParser mResponseParser = new RecognizeResponseParser();

    private Runnable mReceiverRunnable = new Runnable() {
        @Override
        public void run() {
            if(mStop || mConnection == null) {
                return;
            }


        }
    };

    public GoogleSpeechRecognition() {

    }

    public void finish() throws IOException, JSONException {
        if(mFinished) {
            throw new IllegalStateException("No ongoing speech recognition! start() must be invoked first.");
        }

        try {
            final String fileSuffix = "\"}}";
            mOutputStream.write(fileSuffix.getBytes(Charset.forName("UTF-8")));
            mOutputStream.close();

            mHttpResponseCode = mConnection.getResponseCode();
            InputStream inputStream = mConnection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String buffer;
            while(!mStop && mHttpResponseCode == 200 && inputStream != null && (buffer = reader.readLine()) != null) {
                builder.append(buffer);
            }

            mResponses.clear();

            if(!mStop && mHttpResponseCode == 200) {
                final JSONObject json = new JSONObject(builder.toString());
                if (!json.isNull("responses")) {
                    final JSONArray jsonArr = json.getJSONArray("responses");
                    for (int i = 0; i < jsonArr.length(); i++) {
                        final RecognizeResponse recognizeResponse = mResponseParser.processJson(jsonArr.getJSONObject(i));
                        mResponses.add(recognizeResponse);
                    }
                }
            }

            if(inputStream != null) {
                inputStream.close();
            }
        } finally {
            // restore state
            mFinished = true;
            mStop = false;
            mReceiverThread = null;
            mOutputStream = null;
            mBase64OutputStream = null;
            mConnection = null;

            Log.d(TAG, "Finished Receiver Thread");
            Log.d(TAG, mResponses.toString());
        }
    }

    public boolean start(String encoding, int sampleRate) throws IOException {
        if(!mFinished) {
            throw new IllegalStateException("There's an ongoing speech recognition! finish() or cancel() must be invoked first.");
        }

        mFinished = false;
        mStop = false;
        try {
            mConnection = createConnection();
            if(mConnection != null) {

                mReceiverThread = new Thread(mReceiverRunnable);
                mReceiverThread.start();

                mOutputStream = mConnection.getOutputStream();
                mBase64OutputStream = new Base64OutputStream(mOutputStream, Base64.NO_WRAP|Base64.NO_PADDING);

                final String filePrefix = "{\"initialRequest\":{\"encoding\":\"" + encoding + "\"," +
                        "\"continuous\":true," +
                        "\"sampleRate\":" + sampleRate + "}," +
                        "\"audioRequest\":{\"content\":\"";
                mOutputStream.write(filePrefix.getBytes(Charset.forName("UTF-8")));
                mOutputStream.flush();
            } else {
                cancel();
                return false;
            }
        } catch (IOException e) {
            cancel();
            throw e;
        }

        return true;
    }

    public void cancel() {
        if(mFinished) {
            return;
        }

        mStop = true;
        if(mReceiverThread != null) {
            mReceiverThread.interrupt();
        }
        if(mConnection != null) {
            mConnection.disconnect();
        }
    }

    public void sendBytes(byte[] bytes) throws IOException {
        if(mFinished) {
            throw new IllegalStateException("No ongoing speech recognition! start() must be invoked first.");
        }

        mBase64OutputStream.write(bytes);
        mBase64OutputStream.flush();
    }

    protected HttpURLConnection createConnection() throws IOException {
        // connection
        URL url = new URL(ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setChunkedStreamingMode(0);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        return conn;
    }

    public List<RecognizeResponse> getResponses() {
        return mResponses;
    }

    public int getHttpResponseCode() {
        return mHttpResponseCode;
    }

    public Throwable getError() {
        return mError;
    }

    public boolean isFinished() {
        return mFinished;
    }
}
