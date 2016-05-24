package com.peppermint.app.data;

import java.io.File;
import java.io.Serializable;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an audio/video recording present in a local file.
 */
public class Recording implements Serializable {

    public static final String CONTENT_TYPE_AUDIO = "audio/mp4";

    private long mId;
    private String mFilePath;
    private String mContentType = CONTENT_TYPE_AUDIO;
    private long mDurationMillis;
    private float mSizeKb;
    private String mRecordedTimestamp;
    private boolean mHasVideo = false;
    private String mTranscription;
    private float mTranscriptionConfidence = -1;
    private String mTranscriptionLanguage;
    private String mTranscriptionUrl;

    public Recording() {
    }

    public Recording(String filePath) {
        this.mFilePath = filePath;
    }

    public Recording(String filePath, long durationMillis, long sizeKb) {
        this.mFilePath = filePath;
        this.mDurationMillis = durationMillis;
        this.mSizeKb = sizeKb;
    }

    public Recording(String filePath, long durationMillis, float sizeKb, boolean hasVideo) {
        this.mFilePath = filePath;
        this.mDurationMillis = durationMillis;
        this.mSizeKb = sizeKb;
        this.mHasVideo = hasVideo;
    }

    public Recording(String filePath, long durationMillis, float sizeKb, boolean hasVideo, String contentType) {
        this.mFilePath = filePath;
        this.mDurationMillis = durationMillis;
        this.mSizeKb = sizeKb;
        this.mHasVideo = hasVideo;
        this.mContentType = contentType;
    }

    public File getFile() {
        return new File(mFilePath);
    }

    public File getValidatedFile() {
        if(mFilePath == null) {
            return null;
        }

        File file = new File(mFilePath);
        if(file.exists() && file.canRead()) {
            return file;
        }

        return null;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public String getContentType() {
        return mContentType;
    }

    public void setContentType(String mContentType) {
        this.mContentType = mContentType;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String mFilePath) {
        this.mFilePath = mFilePath;
    }

    public long getDurationMillis() {
        return mDurationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.mDurationMillis = durationMillis;
    }

    public float getSizeKb() {
        return mSizeKb;
    }

    public void setSizeKb(float mSizeKb) {
        this.mSizeKb = mSizeKb;
    }

    public boolean hasVideo() {
        return mHasVideo;
    }

    public void setHasVideo(boolean mHasVideo) {
        this.mHasVideo = mHasVideo;
    }

    public String getRecordedTimestamp() {
        return mRecordedTimestamp;
    }

    public void setRecordedTimestamp(String mRecordedTimestamp) {
        this.mRecordedTimestamp = mRecordedTimestamp;
    }

    public String getTranscription() {
        return mTranscription;
    }

    public void setTranscription(String mTranscription) {
        this.mTranscription = mTranscription;
    }

    public float getTranscriptionConfidence() {
        return mTranscriptionConfidence;
    }

    public void setTranscriptionConfidence(float mTranscriptionConfidence) {
        this.mTranscriptionConfidence = mTranscriptionConfidence;
    }

    public String getTranscriptionLanguage() {
        return mTranscriptionLanguage;
    }

    public void setTranscriptionLanguage(String mTranscriptionLanguage) {
        this.mTranscriptionLanguage = mTranscriptionLanguage;
    }

    public String getTranscriptionUrl() {
        return mTranscriptionUrl;
    }

    public void setTranscriptionUrl(String mTranscriptionUrl) {
        this.mTranscriptionUrl = mTranscriptionUrl;
    }

    @Override
    public String toString() {
        return "Recording{" +
                "mId=" + mId +
                ", mFilePath='" + mFilePath + '\'' +
                ", mTranscription='" + mTranscription + '\'' +
                ", mTranscriptionLanguage='" + mTranscriptionLanguage + '\'' +
                ", mTranscriptionConfidence=" + mTranscriptionConfidence +
                ", mTranscriptionUrl='" + mTranscriptionUrl + '\'' +
                ", mContentType='" + mContentType + '\'' +
                ", mDurationMillis=" + mDurationMillis +
                ", mSizeKb=" + mSizeKb +
                ", mRecordedTimestamp='" + mRecordedTimestamp + '\'' +
                ", mHasVideo=" + mHasVideo +
                '}';
    }
}
