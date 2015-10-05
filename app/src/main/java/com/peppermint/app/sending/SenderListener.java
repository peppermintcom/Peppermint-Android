package com.peppermint.app.sending;

import com.peppermint.app.data.SendingRequest;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Listener of sending events triggered by a Sender (SendingTask and SenderErrorHandler).
 */
public interface SenderListener {
    // the following are triggered by the SendingTask
    void onSendingTaskStarted(SendingTask sendingTask, SendingRequest sendingRequest);
    void onSendingTaskCancelled(SendingTask sendingTask, SendingRequest sendingRequest);
    void onSendingTaskFinished(SendingTask sendingTask, SendingRequest sendingRequest);
    void onSendingTaskError(SendingTask sendingTask, SendingRequest sendingRequest, Throwable error);
    void onSendingTaskProgress(SendingTask sendingTask, SendingRequest sendingRequest, float progressValue);

    // the following are triggered by the SenderErrorHandler
    void onSendingRequestRecovered(SendingTask previousSendingTask, SendingRequest sendingRequest);
    void onSendingRequestNotRecovered(SendingTask previousSendingTask, SendingRequest sendingRequest);
}

