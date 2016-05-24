package com.peppermint.app.cloud.apis.data;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 23-05-2016.
 *
 * Data wrapper for HTTP /transcriptions endpoint responses.
 */
public class TranscriptionResponse implements Serializable {

    private String mTranscriptionUrl;
    private String mAudioUrl, mTranscription;
    private String mIp, mTimestamp;
    private String mLanguageTag;
    private float mConfidence;

    public TranscriptionResponse() {
    }

    public String getTranscriptionUrl() {
        return mTranscriptionUrl;
    }

    public void setTranscriptionUrl(String mTranscriptionUrl) {
        this.mTranscriptionUrl = mTranscriptionUrl;
    }

    public String getAudioUrl() {
        return mAudioUrl;
    }

    public void setAudioUrl(String mAudioUrl) {
        this.mAudioUrl = mAudioUrl;
    }

    public String getTranscription() {
        return mTranscription;
    }

    public void setTranscription(String mTranscription) {
        this.mTranscription = mTranscription;
    }

    public String getIp() {
        return mIp;
    }

    public void setIp(String mIp) {
        this.mIp = mIp;
    }

    public String getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(String mTimestamp) {
        this.mTimestamp = mTimestamp;
    }

    public String getLanguageTag() {
        return mLanguageTag;
    }

    public void setLanguageTag(String mLanguageTag) {
        this.mLanguageTag = mLanguageTag;
    }

    public float getConfidence() {
        return mConfidence;
    }

    public void setConfidence(float mConfidence) {
        this.mConfidence = mConfidence;
    }

    @Override
    public String toString() {
        return "TranscriptionResponse{" +
                "mTranscriptionUrl='" + mTranscriptionUrl + '\'' +
                ", mAudioUrl='" + mAudioUrl + '\'' +
                ", mTranscription='" + mTranscription + '\'' +
                ", mIp='" + mIp + '\'' +
                ", mTimestamp='" + mTimestamp + '\'' +
                ", mLanguageTag='" + mLanguageTag + '\'' +
                ", mConfidence=" + mConfidence +
                '}';
    }
}
