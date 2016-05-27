package com.peppermint.app.cloud.apis.speech;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 18-05-2016.
 * See https://cloud.google.com/speech/reference/rest/v1/speech/recognize#SpeechRecognitionAlternative
 * for more info.
 */
public class SpeechRecognitionAlternative implements Serializable {
    private String mTranscript;
    private float mConfidence;

    public SpeechRecognitionAlternative() {
    }

    public SpeechRecognitionAlternative(String mTranscript, float mConfidence) {
        this.mTranscript = mTranscript;
        this.mConfidence = mConfidence;
    }

    public String getTranscript() {
        return mTranscript;
    }

    public void setTranscript(String mTranscript) {
        this.mTranscript = mTranscript;
    }

    public float getConfidence() {
        return mConfidence;
    }

    public void setConfidence(float mConfidence) {
        this.mConfidence = mConfidence;
    }

    @Override
    public String toString() {
        return "SpeechRecognitionAlternative{" +
                "mTranscript='" + mTranscript + '\'' +
                ", mConfidence=" + mConfidence +
                '}';
    }
}
