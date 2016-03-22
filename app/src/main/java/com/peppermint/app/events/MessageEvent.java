package com.peppermint.app.events;

import com.peppermint.app.data.Message;

/**
 * Created by Nuno Luz on 22-03-2016.
 *
 * Represents a {@link com.peppermint.app.data.Message} related event.
 */
public class MessageEvent {

    public static final int EVENT_MARK_PLAYED = 1;

    private Message mMessage;
    private int mType = EVENT_MARK_PLAYED;

    public MessageEvent(Message message) {
        this.mMessage = message;
    }

    public Message getMessage() {
        return mMessage;
    }

    public void setMessage(Message mMessage) {
        this.mMessage = mMessage;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    @Override
    public String toString() {
        return "MessageEvent{" +
                "mMessage=" + mMessage +
                ", mType=" + mType +
                '}';
    }
}
