package com.peppermint.app.sending;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Listener of sender events triggered by a SenderTask.
 */
public interface SenderListener {
    /**
     * Triggered when the sender task starts.
     * This method may not execute on the main thread!
     *
     * @param senderTask the sender task instance
     */
    void onSendingTaskStarted(SenderTask senderTask);

    /**
     * Triggered when the sender task is cancelled.
     * This method may not execute on the main thread!
     *
     * @param senderTask the sender task instance
     */
    void onSendingTaskCancelled(SenderTask senderTask);

    /**
     * Triggered when the sender task finishes successfully.
     * This method may not execute on the main thread!
     *
     * @param senderTask the sender task instance
     */
    void onSendingTaskFinished(SenderTask senderTask);

    /**
     * Triggered when the sending task finishes with an error.
     * This method may not execute on the main thread!
     *
     * @param senderTask the sender task instance
     * @param error the thrown error
     */
    void onSendingTaskError(SenderTask senderTask, Throwable error);

    /**
     * Triggered when the sender task progresses.
     * Not all sender tasks provide progress information!
     * This method may not execute on the main thread!
     *
     * @param senderTask the sender task instance
     * @param progressValue the current progress value
     */
    void onSendingTaskProgress(SenderTask senderTask, float progressValue);

    /**
     * Triggered by the {@link SenderErrorHandler} when the {@link Sender} has recovered from a failed {@link SenderTask}.
     * This method may not execute on the main thread!
     *
     * @param previousSenderTask the sender task instance that failed
     */
    void onSendingRequestRecovered(SenderTask previousSenderTask);

    /**
     * Triggered by the {@link SenderErrorHandler} when the {@link Sender} cannot recover from a failed {@link SenderTask}.
     * This method may not execute on the main thread!
     *
     * @param previousSenderTask the sender task instance that failed
     * @param error the thrown error
     */
    void onSendingRequestNotRecovered(SenderTask previousSenderTask, Throwable error);
}

