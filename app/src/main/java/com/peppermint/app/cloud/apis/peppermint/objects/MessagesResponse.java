package com.peppermint.app.cloud.apis.peppermint.objects;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Data wrapper for HTTP /messages endpoint responses.
 */
public class MessagesResponse implements Serializable {

    private String mMessageId;
    private String mAudioUrl, mTranscription;
    private String mSenderEmail, mRecipientEmail, mCreatedTimestamp;
    private String mSenderName, mReadTimestamp;
    private int mDuration;

    public MessagesResponse() {
    }

    public MessagesResponse(String mMessageId, String mAudioUrl, String mTranscription, String mSenderEmail, String mSenderName, String mRecipientEmail, int mDuration, String mCreatedTimestamp, String mReadTimestamp) {
        this.mMessageId = mMessageId;
        this.mAudioUrl = mAudioUrl;
        this.mTranscription = mTranscription;
        this.mSenderEmail = mSenderEmail;
        this.mSenderName = mSenderName;
        this.mRecipientEmail = mRecipientEmail;
        this.mCreatedTimestamp = mCreatedTimestamp;
        this.mReadTimestamp = mReadTimestamp;
        this.mDuration = mDuration;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int mDuration) {
        this.mDuration = mDuration;
    }

    public String getSenderName() {
        return mSenderName;
    }

    public void setSenderName(String mSenderName) {
        this.mSenderName = mSenderName;
    }

    public String getReadTimestamp() {
        return mReadTimestamp;
    }

    public void setReadTimestamp(String mReadTimestamp) {
        this.mReadTimestamp = mReadTimestamp;
    }

    public String getMessageId() {
        return mMessageId;
    }

    public void setMessageId(String mMessageId) {
        this.mMessageId = mMessageId;
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

    public String getSenderEmail() {
        return mSenderEmail;
    }

    public void setSenderEmail(String mSenderEmail) {
        this.mSenderEmail = mSenderEmail;
    }

    public String getRecipientEmail() {
        return mRecipientEmail;
    }

    public void setRecipientEmail(String mRecipientEmail) {
        this.mRecipientEmail = mRecipientEmail;
    }

    public String getCreatedTimestamp() {
        return mCreatedTimestamp;
    }

    public void setCreatedTimestamp(String mCreatedTimestamp) {
        this.mCreatedTimestamp = mCreatedTimestamp;
    }

    @Override
    public String toString() {
        return "MessagesResponse{" +
                "mMessageId='" + mMessageId + '\'' +
                ", mAudioUrl='" + mAudioUrl + '\'' +
                ", mTranscription='" + mTranscription + '\'' +
                ", mSenderEmail='" + mSenderEmail + '\'' +
                ", mSenderName='" + mSenderName + '\'' +
                ", mRecipientEmail='" + mRecipientEmail + '\'' +
                ", mCreatedTimestamp='" + mCreatedTimestamp + '\'' +
                ", mReadTimestamp='" + mReadTimestamp + '\'' +
                ", mDuration=" + mDuration +
                '}';
    }
}
