package com.peppermint.app.events;

import com.peppermint.app.cloud.senders.SenderManager;
import com.peppermint.app.cloud.senders.SenderTask;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an event dispatched by the {@link SenderManager} and triggered by the Sender API.
 */
public class SenderEvent {

    /**
     * When a {@link SenderTask} starts
     */
    public static final int EVENT_STARTED = 1;

    /**
     * When a {@link SenderTask} is cancelled
     */
    public static final int EVENT_CANCELLED = 2;

    /**
     * When a {@link SenderTask} successfully finishes executing
     */
    public static final int EVENT_FINISHED = 3;

    /**
     * When a {@link SenderTask} finishes with an error
     */
    public static final int EVENT_ERROR = 4;

    /**
     * When a {@link SenderTask} progresses
     */
    public static final int EVENT_PROGRESS = 5;

    /**
     * When a {@link SenderTask} fails to execute and is queued for retry
     */
    public static final int EVENT_QUEUED = 6;

    private SenderTask mTask;
    private int mType;
    private Throwable mError;

    public SenderEvent(SenderTask task, int type) {
        this.mTask = task;
        this.mType = type;
    }

    public SenderEvent(SenderTask task, int type, Throwable error) {
        this.mTask = task;
        this.mType = type;
        this.mError = error;
    }

    public SenderEvent(SenderTask task, Throwable error) {
        this.mTask = task;
        this.mType = EVENT_ERROR;
        this.mError = error;
    }

    public SenderTask getSenderTask() {
        return mTask;
    }

    public void setSenderTask(SenderTask mTask) {
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

    @Override
    public String toString() {
        return "SenderEvent{" +
                "mTask=" + mTask +
                ", mType=" + mType +
                ", mError=" + mError +
                '}';
    }
}
