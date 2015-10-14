package com.peppermint.app.sending;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an event dispatched by the {@link SenderManager} and triggered by the Sender API.
 */
public class SendingEvent {

    /**
     * When a {@link SendingTask} starts executing a {@link com.peppermint.app.data.SendingRequest}
     */
    public static final int EVENT_STARTED = 1;

    /**
     * When a {@link SendingTask} is cancelled
     */
    public static final int EVENT_CANCELLED = 2;

    /**
     * When a {@link SendingTask} successfully finishes executing a {@link com.peppermint.app.data.SendingRequest}
     */
    public static final int EVENT_FINISHED = 3;

    /**
     * When a {@link SendingTask} finishes with an error
     */
    public static final int EVENT_ERROR = 4;

    /**
     * When a {@link SendingTask} executing a {@link com.peppermint.app.data.SendingRequest} progresses
     */
    public static final int EVENT_PROGRESS = 5;

    /**
     * When a {@link SendingTask} fails to execute a {@link com.peppermint.app.data.SendingRequest} and is queued for retry
     */
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
