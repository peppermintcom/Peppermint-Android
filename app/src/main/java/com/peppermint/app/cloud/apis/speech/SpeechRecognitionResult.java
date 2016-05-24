package com.peppermint.app.cloud.apis.speech;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 18-05-2016.
 * See https://cloud.google.com/speech/reference/rest/v1/speech/recognize#SpeechRecognitionResult
 * for more info.
 */
public class SpeechRecognitionResult {
    private boolean mFinal;
    private float mStability;
    private List<SpeechRecognitionAlternative> mAlternatives = new ArrayList<>();

    public SpeechRecognitionResult() {
    }

    public SpeechRecognitionResult(boolean mFinal, float mStability) {
        this.mFinal = mFinal;
        this.mStability = mStability;
    }

    public boolean isFinal() {
        return mFinal;
    }

    public void setFinal(boolean mFinal) {
        this.mFinal = mFinal;
    }

    public float getStability() {
        return mStability;
    }

    public void setStability(float mStability) {
        this.mStability = mStability;
    }

    public List<SpeechRecognitionAlternative> getAlternatives() {
        return mAlternatives;
    }

    public void setAlternatives(List<SpeechRecognitionAlternative> mAlternatives) {
        this.mAlternatives = mAlternatives;
    }

    public void addAlternative(SpeechRecognitionAlternative alternative) {
        mAlternatives.add(alternative);
    }

    public boolean removeAlternative(SpeechRecognitionAlternative alternative) {
        return mAlternatives.remove(alternative);
    }

    @Override
    public String toString() {
        return "SpeechRecognitionResult{" +
                "mFinal=" + mFinal +
                ", mStability=" + mStability +
                ", mAlternatives=" + mAlternatives +
                '}';
    }
}
