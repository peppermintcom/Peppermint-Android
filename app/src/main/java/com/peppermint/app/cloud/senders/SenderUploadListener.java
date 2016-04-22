package com.peppermint.app.cloud.senders;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * RecordServiceListener of sender events triggered by a {@link SenderUploadTask}.
 */
public interface SenderUploadListener {
    /**
     * Triggered when the sender upload task starts.
     * This method may not execute on the main thread!
     *
     * @param uploadTask the sender upload task instance
     */
    void onSendingUploadStarted(SenderUploadTask uploadTask);

    /**
     * Triggered when the sender upload task is cancelled.
     * This method may not execute on the main thread!
     *
     * @param uploadTask the sender upload task instance
     */
    void onSendingUploadCancelled(SenderUploadTask uploadTask);

    /**
     * Triggered when the sender upload task finishes successfully.
     * This method may not execute on the main thread!
     *
     * @param uploadTask the sender upload task instance
     */
    void onSendingUploadFinished(SenderUploadTask uploadTask);

    /**
     * Triggered when the sender upload task finishes with an error.
     * This method may not execute on the main thread!
     *
     * @param uploadTask the sender upload task instance
     * @param error the thrown error
     */
    void onSendingUploadError(SenderUploadTask uploadTask, Throwable error);

    /**
     * Triggered when the sender upload task progresses.
     * Not all sender upload tasks provide progress information!
     * This method may not execute on the main thread!
     *
     * @param uploadTask the sender upload task instance
     * @param progressValue the current progress value
     */
    void onSendingUploadProgress(SenderUploadTask uploadTask, float progressValue);

    /**
     * Triggered when the sender upload task is running and reaches a state when it's no longer cancellable.
     * @param uploadTask the sender upload task instance
     */
    void onSendingUploadNonCancellable(SenderUploadTask uploadTask);

    // TODO maybe move these to a different listener class
    /**
     * Triggered by the {@link SenderErrorHandler} when the {@link Sender} has recovered from a failed {@link SenderUploadTask}.
     * This method may not execute on the main thread!
     *
     * @param previousSenderUploadTask the sender upload task instance that failed
     */
    void onSendingUploadRequestRecovered(SenderUploadTask previousSenderUploadTask);

    /**
     * Triggered by the {@link SenderErrorHandler} when the {@link Sender} cannot recover from a failed {@link SenderUploadTask}.
     * This method may not execute on the main thread!
     *
     * @param previousSenderUploadTask the sender upload task instance that failed
     * @param error the thrown error
     */
    void onSendingUploadRequestNotRecovered(SenderUploadTask previousSenderUploadTask, Throwable error);

}

