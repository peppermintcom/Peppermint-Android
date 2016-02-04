package com.peppermint.app.sending;

import com.peppermint.app.data.Message;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an event dispatched by the {@link com.peppermint.app.MessagesService} related to received messages.
 */
public class ReceiverEvent {

    public static final int EVENT_RECEIVED = 1;

    private int mType;
    private String mReceiverEmail;
    private Message mMessage;

    public ReceiverEvent() {
        mType = EVENT_RECEIVED;
    }

    public ReceiverEvent(int mType, String mReceiverEmail, Message mMessage) {
        this.mType = mType;
        this.mReceiverEmail = mReceiverEmail;
        this.mMessage = mMessage;
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

    public Message getMessage() {
        return mMessage;
    }

    public void setMessage(Message mMessage) {
        this.mMessage = mMessage;
    }

    @Override
    public String toString() {
        return "ReceiverEvent{" +
                "mType=" + mType +
                ", mReceiverEmail='" + mReceiverEmail + '\'' +
                ", mMessage=" + mMessage +
                '}';
    }
}
