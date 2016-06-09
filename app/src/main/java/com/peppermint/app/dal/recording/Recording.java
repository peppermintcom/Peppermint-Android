package com.peppermint.app.dal.recording;

import com.peppermint.app.dal.DataObject;

import java.io.File;
import java.io.Serializable;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an audio/video recording present in a local file.
 */
public class Recording extends DataObject implements Serializable {

    public static final int FIELD_FILE_PATH = 1;
    public static final int FIELD_CONTENT_TYPE = 2;
    public static final int FIELD_DURATION_MILLIS = 3;
    public static final int FIELD_SIZE_KB = 4;
    public static final int FIELD_RECORDED_TIMESTAMP = 5;
    public static final int FIELD_HAS_VIDEO = 6;
    public static final int FIELD_TRANSCRIPTION = 7;
    public static final int FIELD_TRANSCRIPTION_CONFIDENCE = 8;
    public static final int FIELD_TRANSCRIPTION_LANGUAGE = 9;
    public static final int FIELD_TRANSCRIPTION_URL = 10;

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

    public Recording(String mFilePath) {
        setFilePath(mFilePath);
    }

    public Recording(String mFilePath, long mDurationMillis, float mSizeKb, boolean mHasVideo) {
        setFilePath(mFilePath);
        setDurationMillis(mDurationMillis);
        setSizeKb(mSizeKb);
        setHasVideo(mHasVideo);
    }

    public Recording(String mFilePath, long mDurationMillis, float mSizeKb, boolean mHasVideo, String mContentType) {
        setFilePath(mFilePath);
        setDurationMillis(mDurationMillis);
        setSizeKb(mSizeKb);
        setHasVideo(mHasVideo);
        setContentType(mContentType);
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
        String oldValue = this.mContentType;
        this.mContentType = mContentType;
        registerUpdate(FIELD_CONTENT_TYPE, oldValue, mContentType);
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String mFilePath) {
        String oldValue = this.mFilePath;
        this.mFilePath = mFilePath;
        registerUpdate(FIELD_FILE_PATH, oldValue, mFilePath);
    }

    public long getDurationMillis() {
        return mDurationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        long oldValue = this.mDurationMillis;
        this.mDurationMillis = durationMillis;
        registerUpdate(FIELD_DURATION_MILLIS, oldValue, durationMillis);
    }

    public float getSizeKb() {
        return mSizeKb;
    }

    public void setSizeKb(float mSizeKb) {
        float oldValue = this.mSizeKb;
        this.mSizeKb = mSizeKb;
        registerUpdate(FIELD_SIZE_KB, oldValue, mSizeKb);
    }

    public boolean hasVideo() {
        return mHasVideo;
    }

    public void setHasVideo(boolean mHasVideo) {
        boolean oldValue = this.mHasVideo;
        this.mHasVideo = mHasVideo;
        registerUpdate(FIELD_HAS_VIDEO, oldValue, mHasVideo);
    }

    public String getRecordedTimestamp() {
        return mRecordedTimestamp;
    }

    public void setRecordedTimestamp(String mRecordedTimestamp) {
        String oldValue = this.mRecordedTimestamp;
        this.mRecordedTimestamp = mRecordedTimestamp;
        registerUpdate(FIELD_RECORDED_TIMESTAMP, oldValue, mRecordedTimestamp);
    }

    public String getTranscription() {
        return mTranscription;
    }

    public void setTranscription(String mTranscription) {
        String oldValue = this.mTranscription;
        this.mTranscription = mTranscription;
        registerUpdate(FIELD_TRANSCRIPTION, oldValue, mTranscription);
    }

    public float getTranscriptionConfidence() {
        return mTranscriptionConfidence;
    }

    public void setTranscriptionConfidence(float mTranscriptionConfidence) {
        float oldValue = this.mTranscriptionConfidence;
        this.mTranscriptionConfidence = mTranscriptionConfidence;
        registerUpdate(FIELD_TRANSCRIPTION_CONFIDENCE, oldValue, mTranscriptionConfidence);
    }

    public String getTranscriptionLanguage() {
        return mTranscriptionLanguage;
    }

    public void setTranscriptionLanguage(String mTranscriptionLanguage) {
        String oldValue = this.mTranscriptionLanguage;
        this.mTranscriptionLanguage = mTranscriptionLanguage;
        registerUpdate(FIELD_TRANSCRIPTION_LANGUAGE, oldValue, mTranscriptionLanguage);
    }

    public String getTranscriptionUrl() {
        return mTranscriptionUrl;
    }

    public void setTranscriptionUrl(String mTranscriptionUrl) {
        String oldValue = this.mTranscriptionUrl;
        this.mTranscriptionUrl = mTranscriptionUrl;
        registerUpdate(FIELD_TRANSCRIPTION_URL, oldValue, mTranscriptionUrl);
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
                ", " + super.toString() +
                '}';
    }
}
