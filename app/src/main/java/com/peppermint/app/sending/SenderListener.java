package com.peppermint.app.sending;

import com.peppermint.app.data.SendingRequest;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Listener of sending events triggered by a Sender (SendingTask and SenderErrorHandler).
 */
public interface SenderListener {
    /**
     * Triggered when the sending task starts.
     * This method may not execute on the main thread!
     *
     * @param sendingTask the sending task instance
     * @param sendingRequest the sending request
     */
    void onSendingTaskStarted(SendingTask sendingTask, SendingRequest sendingRequest);

    /**
     * Triggered when the sending task is cancelled.
     * This method may not execute on the main thread!
     *
     * @param sendingTask the sending task instance
     * @param sendingRequest the sending request
     */
    void onSendingTaskCancelled(SendingTask sendingTask, SendingRequest sendingRequest);

    /**
     * Triggered when the sending task finishes successfully.
     * This method may not execute on the main thread!
     *
     * @param sendingTask the sending task instance
     * @param sendingRequest the sending request
     */
    void onSendingTaskFinished(SendingTask sendingTask, SendingRequest sendingRequest);

    /**
     * Triggered when the sending task finishes with an error.
     * This method may not execute on the main thread!
     *
     * @param sendingTask the sending task instance
     * @param sendingRequest the sending request
     * @param error the thrown error
     */
    void onSendingTaskError(SendingTask sendingTask, SendingRequest sendingRequest, Throwable error);

    /**
     * Triggered when the sending task progresses.
     * Not all senders provide progress information!
     * This method may not execute on the main thread!
     *
     * @param sendingTask the sending task instance
     * @param sendingRequest the sending request
     * @param progressValue the current progress value
     */
    void onSendingTaskProgress(SendingTask sendingTask, SendingRequest sendingRequest, float progressValue);

    /**
     * Triggered by the {@link SendingErrorHandler} when the {@link Sender} has recovered from a failed {@link SendingTask}.
     * This method may not execute on the main thread!
     *
     * @param previousSendingTask the sending task instance that failed
     * @param sendingRequest the sending request
     */
    void onSendingRequestRecovered(SendingTask previousSendingTask, SendingRequest sendingRequest);

    /**
     * Triggered by the {@link SendingErrorHandler} when the {@link Sender} cannot recover from a failed {@link SendingTask}.
     * This method may not execute on the main thread!
     *
     * @param previousSendingTask the sending task instance that failed
     * @param sendingRequest the sending request
     * @param error the thrown error
     */
    void onSendingRequestNotRecovered(SendingTask previousSendingTask, SendingRequest sendingRequest, Throwable error);
}

