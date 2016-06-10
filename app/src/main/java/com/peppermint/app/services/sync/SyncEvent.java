package com.peppermint.app.services.sync;

import com.peppermint.app.services.messenger.MessengerService;

import java.util.Set;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an event dispatched by the {@link MessengerService} related to message synchronization with server.
 */
public class SyncEvent {

    public static final int EVENT_STARTED = 1;
    public static final int EVENT_FINISHED = 2;
    public static final int EVENT_CANCELLED = 3;
    public static final int EVENT_ERROR = 4;
    public static final int EVENT_PROGRESS = 5;

    private Throwable mError;
    private int mType;
    private Set<Long> mReceivedMessageIds;
    private Set<Long> mSentMessageIds;
    private Set<Long> mAffectedChatIds;

    public SyncEvent() {
        mType = EVENT_STARTED;
    }

    public SyncEvent(int mType, Set<Long> mReceivedMessageIds, Set<Long> mSentMessageIds, Set<Long> mAffectedChatIds, Throwable mError) {
        this.mType = mType;
        this.mReceivedMessageIds = mReceivedMessageIds;
        this.mSentMessageIds = mSentMessageIds;
        this.mAffectedChatIds = mAffectedChatIds;
        this.mError = mError;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public Set<Long> getReceivedMessageIdSet() {
        return mReceivedMessageIds;
    }

    public void setReceivedMessageIdSet(Set<Long> mReceivedMessageIds) {
        this.mReceivedMessageIds = mReceivedMessageIds;
    }

    public Set<Long> getSentMessageIdSet() {
        return mSentMessageIds;
    }

    public void setSentMessageIdSet(Set<Long> mSentMessageIds) {
        this.mSentMessageIds = mSentMessageIds;
    }

    public Set<Long> getAffectedChatIdSet() {
        return mAffectedChatIds;
    }

    public void setAffectedChatIdSet(Set<Long> mAffectedChatIds) {
        this.mAffectedChatIds = mAffectedChatIds;
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
                "mType=" + (mType == EVENT_STARTED ? "TYPE_STARTED" : (mType == EVENT_FINISHED ? "TYPE_FINISHED" :
                    (mType == EVENT_CANCELLED ? "TYPE_CANCELLED" : (mType == EVENT_ERROR ? "TYPE_ERROR" :
                            (mType == EVENT_PROGRESS ? "TYPE_PROGRESS" : mType))))) +
                ", mError=" + mError +
                '}';
    }
}
