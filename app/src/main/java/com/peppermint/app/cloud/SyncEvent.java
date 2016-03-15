package com.peppermint.app.cloud;

import com.peppermint.app.data.Message;

import java.util.List;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an event dispatched by the {@link MessagesService} related to message synchronization with server.
 */
public class SyncEvent {

    public static final int EVENT_STARTED = 1;
    public static final int EVENT_FINISHED = 2;
    public static final int EVENT_CANCELLED = 3;
    public static final int EVENT_ERROR = 4;

    private Throwable mError;
    private int mType;
    private List<Message> mReceivedMessageList;
    private List<Message> mSentMessageList;

    public SyncEvent() {
        mType = EVENT_STARTED;
    }

    public SyncEvent(int mType, List<Message> mReceivedMessageList, List<Message> mSentMessageList) {
        this.mType = mType;
        this.mReceivedMessageList = mReceivedMessageList;
        this.mSentMessageList = mSentMessageList;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public List<Message> getReceivedMessageList() {
        return mReceivedMessageList;
    }

    public void setReceivedMessageList(List<Message> mReceivedMessageList) {
        this.mReceivedMessageList = mReceivedMessageList;
    }

    public List<Message> getSentMessageList() {
        return mSentMessageList;
    }

    public void setSentMessageList(List<Message> mSentMessageList) {
        this.mSentMessageList = mSentMessageList;
    }

    public Throwable getError() {
        return mError;
    }

    public void setError(Throwable mError) {
        this.mError = mError;
    }

    @Override
    public String toString() {
        return "SyncEvent{" +
                "mType=" + mType +
                ", mError=" + mError +
                ", mReceivedMessageList=" + mReceivedMessageList +
                ", mSentMessageList=" + mSentMessageList +
                '}';
    }
}
