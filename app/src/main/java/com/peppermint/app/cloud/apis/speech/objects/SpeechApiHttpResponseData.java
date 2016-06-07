package com.peppermint.app.cloud.apis.speech.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 18-05-2016.
 * See https://cloud.google.com/speech/reference/rest/v1/speech/recognize#response-body
 * for more info.
 */
public class SpeechApiHttpResponseData implements Serializable {

    private List<RecognizeResponse> mResponses = new ArrayList<>();

    private int mErrorCode;
    private String mErrorMessage;

    public SpeechApiHttpResponseData() {
    }

    public SpeechApiHttpResponseData(int mErrorCode, String mErrorMessage) {
        this.mErrorCode = mErrorCode;
        this.mErrorMessage = mErrorMessage;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(int mErrorCode) {
        this.mErrorCode = mErrorCode;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public void setErrorMessage(String mErrorMessage) {
        this.mErrorMessage = mErrorMessage;
    }

    public void addResponse(RecognizeResponse response) {
        mResponses.add(response);
    }

    public boolean removeResponse(RecognizeResponse response) {
        return mResponses.remove(response);
    }

    public List<RecognizeResponse> getResponses() {
        return mResponses;
    }

    @Override
    public String toString() {
        return "SpeechApiHttpResponse{" +
                "mResponses=" + mResponses +
                ", mErrorCode=" + mErrorCode +
                ", mErrorMessage='" + mErrorMessage + '\'' +
                '}';
    }
}
