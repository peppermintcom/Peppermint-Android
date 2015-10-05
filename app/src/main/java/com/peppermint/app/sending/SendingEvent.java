package com.peppermint.app.sending;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Event sent by the SenderManager through an EventBus.
 */
public class SendingEvent {

    public static final int EVENT_STARTED = 1;
    public static final int EVENT_CANCELLED = 2;
    public static final int EVENT_FINISHED = 3;
    public static final int EVENT_ERROR = 4;
    public static final int EVENT_PROGRESS = 5;
    public static final int EVENT_QUEUED = 6;

    private SendingTask mTask;
    private int mType;
    private Throwable mError;

    public SendingEvent(SendingTask task, int type) {
        this.mTask = task;
        this.mType = type;
    }

    public SendingEvent(SendingTask task, int type, Throwable error) {
        this.mTask = task;
        this.mType = type;
        this.mError = error;
    }

    public SendingEvent(SendingTask task, Throwable error) {
        this.mTask = task;
        this.mType = EVENT_ERROR;
        this.mError = error;
    }

    public SendingTask getSendingTask() {
        return mTask;
    }

    public void setSendingTask(SendingTask mTask) {
        this.mTask = mTask;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public Throwable getError() {
        return mError;
    }

    public void setError(Throwable mError) {
        this.mError = mError;
    }
}
