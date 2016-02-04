package com.peppermint.app.sending;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an event dispatched by the {@link com.peppermint.app.MessagesService} related to received messages.
 */
public class ReceiverEvent {

    public static final int EVENT_RECEIVED = 1;

    private int mType;
    private String mReceiverEmail, mSenderEmail, mSenderName;
    private String mAudioUrl, mTranscriptionUrl;

    public ReceiverEvent() {
        mType = EVENT_RECEIVED;
    }

    public ReceiverEvent(int mType, String mReceiverEmail, String mSenderEmail, String mSenderName, String mAudioUrl, String mTranscriptionUrl) {
        this.mType = mType;
        this.mReceiverEmail = mReceiverEmail;
        this.mSenderEmail = mSenderEmail;
        this.mSenderName = mSenderName;
        this.mAudioUrl = mAudioUrl;
        this.mTranscriptionUrl = mTranscriptionUrl;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public String getReceiverEmail() {
        return mReceiverEmail;
    }

    public void setReceiverEmail(String mReceiverEmail) {
        this.mReceiverEmail = mReceiverEmail;
    }

    public String getSenderEmail() {
        return mSenderEmail;
    }

    public void setSenderEmail(String mSenderEmail) {
        this.mSenderEmail = mSenderEmail;
    }

    public String getSenderName() {
        return mSenderName;
    }

    public void setSenderName(String mSenderName) {
        this.mSenderName = mSenderName;
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

    @Override
    public String toString() {
        return "ReceiverEvent{" +
                "mType=" + mType +
                ", mReceiverEmail='" + mReceiverEmail + '\'' +
                ", mSenderEmail='" + mSenderEmail + '\'' +
                ", mSenderName='" + mSenderName + '\'' +
                ", mAudioUrl='" + mAudioUrl + '\'' +
                ", mTranscriptionUrl='" + mTranscriptionUrl + '\'' +
                '}';
    }
}
