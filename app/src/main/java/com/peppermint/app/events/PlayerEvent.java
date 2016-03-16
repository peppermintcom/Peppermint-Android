package com.peppermint.app.events;

import com.peppermint.app.PlayerService;
import com.peppermint.app.data.Message;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an event dispatched by the {@link PlayerService}.
 */
public class PlayerEvent {

    public static final int ERROR_ILLEGAL_STATE = -1;
    public static final int ERROR_NO_CONNECTIVITY = -2;
    public static final int ERROR_DATA_SOURCE = -3;

    public static final int EVENT_STARTED = 1;
    public static final int EVENT_PAUSED = 2;
    public static final int EVENT_COMPLETED = 3;
    public static final int EVENT_PREPARED = 4;
    public static final int EVENT_BUFFERING_UPDATE = 5;
    public static final int EVENT_ERROR = 6;
    public static final int EVENT_PROGRESS = 7;

    private int mType;
    private Message mMessage;
    private int mPercent;
    private long mCurrentMs;
    private int mErrorCode;

    public PlayerEvent() {
    }

    public PlayerEvent(int mType, Message mMessage, int mPercent, long mCurrentMs) {
        this.mType = mType;
        this.mMessage = mMessage;
        this.mPercent = mPercent;
        this.mCurrentMs = mCurrentMs;
    }

    public PlayerEvent(int mType, Message mMessage, int mPercent, long mCurrentMs, int mErrorCode) {
        this.mType = mType;
        this.mMessage = mMessage;
        this.mPercent = mPercent;
        this.mCurrentMs = mCurrentMs;
        this.mErrorCode = mErrorCode;
    }

    public PlayerEvent(Message mMessage, int mPercent, long mCurrentMs, int mErrorCode) {
        this(EVENT_ERROR, mMessage, mPercent, mCurrentMs, mErrorCode);
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(int mErrorCode) {
        this.mErrorCode = mErrorCode;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public Message getMessage() {
        return mMessage;
    }

    public void setMessage(Message mMessage) {
        this.mMessage = mMessage;
    }

    public int getPercent() {
        return mPercent;
    }

    public void setPercent(int mPercent) {
        this.mPercent = mPercent;
    }

    public long getCurrentMs() {
        return mCurrentMs;
    }

    public void setCurrentMs(long mCurrentMs) {
        this.mCurrentMs = mCurrentMs;
    }

    @Override
    public String toString() {
        return "PlayerEvent{" +
                "mType=" + mType +
                ", mMessage=" + mMessage +
                ", mPercent=" + mPercent +
                ", mCurrentMs=" + mCurrentMs +
                '}';
    }
}
