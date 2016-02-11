package com.peppermint.app.cloud.apis.data;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Data wrapper for HTTP /recorder endpoint responses.
 */
public class RecorderResponse extends AccessTokenResponse {

    private String mRecorderId, mRecorderClientId;
    private String mRecorderKey, mDescription;
    private String mRegistrationTimestamp;
    private String mGcmRegistationToken;

    public RecorderResponse() {
    }

    public RecorderResponse(String mRecorderId, String mRecorderClientId, String mRecorderKey, String mDescription, String mRegistrationTimestamp, String mGcmRegistationToken) {
        this.mRecorderId = mRecorderId;
        this.mRecorderClientId = mRecorderClientId;
        this.mRecorderKey = mRecorderKey;
        this.mDescription = mDescription;
        this.mRegistrationTimestamp = mRegistrationTimestamp;
        this.mGcmRegistationToken = mGcmRegistationToken;
    }

    public String getRecorderId() {
        return mRecorderId;
    }

    public void setRecorderId(String mRecorderId) {
        this.mRecorderId = mRecorderId;
    }

    public String getRecorderClientId() {
        return mRecorderClientId;
    }

    public void setRecorderClientId(String mRecorderClientId) {
        this.mRecorderClientId = mRecorderClientId;
    }

    public String getRecorderKey() {
        return mRecorderKey;
    }

    public void setRecorderKey(String mRecorderKey) {
        this.mRecorderKey = mRecorderKey;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public String getRegistrationTimestamp() {
        return mRegistrationTimestamp;
    }

    public void setRegistrationTimestamp(String mRegistrationTimestamp) {
        this.mRegistrationTimestamp = mRegistrationTimestamp;
    }

    public String getGcmRegistationToken() {
        return mGcmRegistationToken;
    }

    public void setGcmRegistationToken(String mGcmRegistationToken) {
        this.mGcmRegistationToken = mGcmRegistationToken;
    }

    @Override
    public String toString() {
        return "RecorderResponse{" +
                "mRecorderId='" + mRecorderId + '\'' +
                ", mRecorderClientId='" + mRecorderClientId + '\'' +
                ", mRecorderKey='" + mRecorderKey + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mRegistrationTimestamp='" + mRegistrationTimestamp + '\'' +
                ", mGcmRegistationToken='" + mGcmRegistationToken + '\'' +
                '}';
    }
}
