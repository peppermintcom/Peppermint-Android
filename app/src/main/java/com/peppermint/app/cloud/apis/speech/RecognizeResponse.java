package com.peppermint.app.cloud.apis.speech;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 18-05-2016.
 * See https://cloud.google.com/speech/reference/rest/v1/speech/recognize#InitialRecognizeRequest
 * for more info.
 */
public class RecognizeResponse {
    public static final int ENDPOINTER_EVENT_UNSPECIFIED = 0;
    public static final int START_OF_SPEECH = 1;
    public static final int END_OF_SPEECH = 2;
    public static final int END_OF_AUDIO = 3;
    public static final int END_OF_UTTERANCE = 4;

    private String mEndpoint;
    private int mResultIndex;
    private List<SpeechRecognitionResult> mResults = new ArrayList<>();

    private int mErrorCode;
    private String mErrorMessage;

    public RecognizeResponse() {
    }

    public RecognizeResponse(String mEndpoint, int mResultIndex, int mErrorCode, String mErrorMessage) {
        this.mEndpoint = mEndpoint;
        this.mResultIndex = mResultIndex;
        this.mErrorCode = mErrorCode;
        this.mErrorMessage = mErrorMessage;
    }

    public String getEndpoint() {
        return mEndpoint;
    }

    public void setEndpoint(String mEndpoint) {
        this.mEndpoint = mEndpoint;
    }

    public int getResultIndex() {
        return mResultIndex;
    }

    public void setResultIndex(int mResultIndex) {
        this.mResultIndex = mResultIndex;
    }

    public List<SpeechRecognitionResult> getResults() {
        return mResults;
    }

    public void setResults(List<SpeechRecognitionResult> mResults) {
        this.mResults = mResults;
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

    public void addResult(SpeechRecognitionResult result) {
        mResults.add(result);
    }

    public boolean removeResult(SpeechRecognitionResult result) {
        return mResults.remove(result);
    }

    @Override
    public String toString() {
        return "RecognizeResponse{" +
                "mEndpoint='" + mEndpoint + '\'' +
                ", mResultIndex=" + mResultIndex +
                ", mResults=" + mResults +
                ", mErrorCode=" + mErrorCode +
                ", mErrorMessage='" + mErrorMessage + '\'' +
                '}';
    }
}
