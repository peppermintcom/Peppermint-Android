package com.peppermint.app.sending.api.data;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 28-01-2016.
 */
public class MessagesResponse implements Serializable {

    private String mMessageId;
    private String mAudioUrl, mTranscriptionUrl;
    private String mSenderEmail, mRecipientEmail, mCreatedTimestamp;

    public MessagesResponse() {
    }

    public MessagesResponse(String mMessageId, String mAudioUrl, String mTranscriptionUrl, String mSenderEmail, String mRecipientEmail, String mCreatedTimestamp) {
        this.mMessageId = mMessageId;
        this.mAudioUrl = mAudioUrl;
        this.mTranscriptionUrl = mTranscriptionUrl;
        this.mSenderEmail = mSenderEmail;
        this.mRecipientEmail = mRecipientEmail;
        this.mCreatedTimestamp = mCreatedTimestamp;
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

    public String getTranscriptionUrl() {
        return mTranscriptionUrl;
    }

    public void setTranscriptionUrl(String mTranscriptionUrl) {
        this.mTranscriptionUrl = mTranscriptionUrl;
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
                ", mTranscriptionUrl='" + mTranscriptionUrl + '\'' +
                ", mSenderEmail='" + mSenderEmail + '\'' +
                ", mRecipientEmail='" + mRecipientEmail + '\'' +
                ", mCreatedTimestamp='" + mCreatedTimestamp + '\'' +
                '}';
    }
}
