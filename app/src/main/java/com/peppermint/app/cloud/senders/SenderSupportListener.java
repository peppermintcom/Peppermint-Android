package com.peppermint.app.cloud.senders;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Listener of sender events triggered by a {@link SenderSupportTask}.
 */
public interface SenderSupportListener {

    /**
     * Triggered when the sender support task starts.
     * This method may not execute on the main thread!
     *
     * @param supportTask the sender support task instance
     */
    void onSendingSupportStarted(SenderSupportTask supportTask);

    /**
     * Triggered when the sender support task is cancelled.
     * This method may not execute on the main thread!
     *
     * @param supportTask the sender support task instance
     */
    void onSendingSupportCancelled(SenderSupportTask supportTask);

    /**
     * Triggered when the sender support task finishes successfully.
     * This method may not execute on the main thread!
     *
     * @param supportTask the sender support task instance
     */
    void onSendingSupportFinished(SenderSupportTask supportTask);

    /**
     * Triggered when the sender support task finishes with an error.
     * This method may not execute on the main thread!
     *
     * @param supportTask the sender support task instance
     * @param error the thrown error
     */
    void onSendingSupportError(SenderSupportTask supportTask, Throwable error);

    /**
     * Triggered when the sender support task progresses.
     * Not all sender support tasks provide progress information!
     * This method may not execute on the main thread!
     *
     * @param supportTask the sender support task instance
     * @param progressValue the current progress value
     */
    void onSendingSupportProgress(SenderSupportTask supportTask, float progressValue);
}

