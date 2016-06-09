package com.peppermint.app.services.messenger.handlers;

/**
 * Created by Nuno Luz on 07-06-2016.
 *
 * Listener for events triggered by the {@link SenderManager}.
 */
public interface SenderManagerListener {
    void onSendingStarted(SenderUploadTask uploadTask);
    void onSendingCancelled(SenderUploadTask uploadTask);
    void onSendingFinished(SenderUploadTask uploadTask);
    void onSendingError(SenderUploadTask uploadTask, Throwable error);
    void onSendingProgress(SenderUploadTask uploadTask, float progressValue);
    void onSendingQueued(SenderUploadTask uploadTask, Throwable error);

    void onSendingNonCancellable(SenderUploadTask uploadTask);
}

