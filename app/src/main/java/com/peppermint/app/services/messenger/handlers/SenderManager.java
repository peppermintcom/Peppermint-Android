package com.peppermint.app.services.messenger.handlers;

import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;

import com.peppermint.app.dal.message.Message;
import com.peppermint.app.services.messenger.handlers.gmail.GmailSender;
import com.peppermint.app.trackers.TrackerApi;
import com.peppermint.app.trackers.TrackerManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * <p>
 *     Main class of the Sender API, which allows sending an audio/video messages through
 *     different protocols in different ways.<br />
 *     Each {@link Sender} is associated with one or more contact mime types
 *     (e.g. "vnd.android.cursor.dir/phone_v2" or "vnd.android.cursor.item/email_v2").<br />
 *     When sending, the manager searches for a {@link Sender} that handles the mime type of
 *     the recipient contact, and uses it to execute an upload task.
 *</p>
 *
 * For instance, there's a sender for:
 * <ul>
 *     <li>Emails through the Gmail API</li>
 *     <li>Emails through the native Android email app</li>
 * </ul>
 */
public class SenderManager extends SenderObject implements SenderUploadListener {

    private static final String TAG = SenderManager.class.getSimpleName();

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private SenderManagerListener mListener;

    private ThreadPoolExecutor mExecutor, mCancelExecutor;      // a thread pool for sending tasks

    private Map<String, Sender> mSenderMap;                     // map of senders <mime type, sender>

    private Map<UUID, SenderTask> mTaskMap;                     // map of sending tasks under execution

    private class CancelRunnable implements Runnable {
        private SenderTask _taskToCancel;
        public CancelRunnable(SenderTask taskToCancel) {
            this._taskToCancel = taskToCancel;
        }
        @Override
        public void run() {
            if(this._taskToCancel != null) {
                this._taskToCancel.cancel();
            }
        }
    }

    public SenderManager(Context context, SenderManagerListener mListener, Map<String, Object> defaultSenderParameters) {
        super(context,
                TrackerManager.getInstance(context.getApplicationContext()),
                defaultSenderParameters,
                new SenderPreferences(context));

        this.mListener = mListener;

        this.mTaskMap = new ConcurrentHashMap<>();
        this.mSenderMap = new HashMap<>();

        this.mCancelExecutor = new ThreadPoolExecutor(CPU_COUNT + 1, CPU_COUNT * 2 + 2,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        // private thread pool to avoid hanging up other AsyncTasks
        // only one thread so that messages are sent one at a time (allows better control when cancelling)
        this.mExecutor = new ThreadPoolExecutor(/*CPU_COUNT + 1, CPU_COUNT * 2 + 2*/1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        // here we add all available sender instances to the sender map
        // add gmail api + email intent sender chain
        GmailSender gmailSender = new GmailSender(this, this);
        gmailSender.getParameters().putAll(defaultSenderParameters);
        gmailSender.setTrackerManager(mTrackerManager);

        mSenderMap.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, gmailSender);
    }

    /**
     * Initializes the manager and all {@link Sender}s
     * <p><strong>{@link #deinit()} must always be invoked when the manager is no longer needed.</strong></p>
     */
    public void init() {
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.init();
                sender = sender.getFailureChainSender();
            }
        }
    }

    /**
     * De-initializes the manager:
     * <ol>
     *     <li>Cancels all running sending tasks and saves them for later re-sending</li>
     *     <li>De-initializes all {@link Sender}s</li>
     * </ol>
     */
    public void deinit() {
        // cancel all tasks
        cancel();

        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.deinit();
                sender = sender.getFailureChainSender();
            }
        }
    }

    /**
     * Tries to send the specified message using one of the available {@link Sender}s.
     * @param message the message
     */
    public void send(Message message) {
        send(message, null);
    }

    /**
     * Tries to send the specified message using one of the available {@link Sender}s.
     * If a previouslyFailedTask is supplied, tries to send the specified message using the
     * following {@link Sender} in the previouslyFailedTask failure chain.
     *
     * @param message the message
     * @param previousFailedTask the previously failed upload task
     */
    public void send(Message message, SenderUploadTask previousFailedTask) {
        String mimeType = message.getChatParameter().getRecipientList().get(0).getMimeType();

        // check if there's a sender for the specified recipient mime type
        if(!mSenderMap.containsKey(mimeType)) {
            throw new NullPointerException("Sender for mime type " + mimeType + " not found!");
        }

        Sender sender = mSenderMap.get(mimeType);
        send(message, sender, previousFailedTask);
    }

    /**
     * Tries to send the specified message using the supplied {@link Sender}.
     * If a previouslyFailedTask is supplied, tries to send the specified message using the
     * following {@link Sender} in the previouslyFailedTask failure chain.
     *
     * @param message the message
     * @param sender the sender
     * @param previousFailedTask the previously failed upload task
     */
    private void send(Message message, Sender sender, SenderUploadTask previousFailedTask) {
        while(sender != null && !sender.isEnabled()) {
            sender = sender.getFailureChainSender();
        }
        if(sender == null) {
            throw new RuntimeException("No sender available. Make sure that all possible senders are not disabled.");
        }

        SenderUploadTask task = sender.newTask(message, previousFailedTask == null ? null : previousFailedTask.getId());
        task.setRecovering(previousFailedTask != null);
        mTaskMap.put(task.getId(), task);
        task.executeOnExecutor(mExecutor);
    }

    /**
     * Cancels the upload task sending the supplied message.
     * @param message the message
     * @return true if the message upload was cancelled; false otherwise
     */
    public boolean cancel(final Message message) {
        boolean cancelled = false;
        for(SenderTask senderTask : mTaskMap.values()) {
            if(senderTask.getMessage().equals(message)) {
                cancelled = true;
                mCancelExecutor.execute(new CancelRunnable(senderTask));
            }
        }
        return cancelled;
    }

    /**
     * Cancels all message uploads under execution.
     */
    public boolean cancel() {
        boolean canceledSome = false;
        for(SenderTask senderTask : mTaskMap.values()) {
            canceledSome = true;
            mCancelExecutor.execute(new CancelRunnable(senderTask));
        }
        return canceledSome;
    }

    /**
     * Checks if there's an upload task sending the supplied message.
     * @param message the message
     * @return true if sending/uploading; false otherwise
     */
    public boolean isSending(Message message) {
        for(SenderTask senderTask : mTaskMap.values()) {
            if(!senderTask.isCancelled() && senderTask.getMessage().equals(message)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there's an upload task sending the supplied message and if it's possible to cancel
     * the operation.
     * @param message the message
     * @return true if sending/uploading and cancellable; false otherwise
     */
    public boolean isSendingAndCancellable(Message message) {
        for(SenderTask senderTask : mTaskMap.values()) {
            if(!senderTask.isCancelled() && !(senderTask instanceof SenderUploadTask && ((SenderUploadTask) senderTask).isNonCancellable()) &&
                    senderTask.getMessage().equals(message)) {
                return true;
            }
        }
        return false;

    }

    /**
     * Checks if there's at least one upload task under execution.
     * @return true if there is; false otherwise
     */
    public boolean isSending() {
        if(mTaskMap.size() <= 0) {
            return false;
        }

        boolean someOngoing = false;
        Iterator<SenderTask> it = mTaskMap.values().iterator();
        while(it.hasNext() && !someOngoing) {
            if(!it.next().isCancelled()) {
                someOngoing = true;
            }
        }

        return someOngoing;
    }

    @Override
    public void onSendingUploadNonCancellable(SenderUploadTask uploadTask) {
        mListener.onSendingNonCancellable(uploadTask);
    }

    @Override
    public void onSendingUploadStarted(SenderUploadTask uploadTask) {
        mListener.onSendingStarted(uploadTask);
    }

    @Override
    public void onSendingUploadCancelled(SenderUploadTask uploadTask) {
        Log.d(TAG, "Cancelled SenderUploadTask " + uploadTask.getId());
        mTaskMap.remove(uploadTask.getId());
        mListener.onSendingCancelled(uploadTask);
    }

    @Override
    public void onSendingUploadFinished(SenderUploadTask uploadTask) {
        Log.d(TAG, "Finished SenderUploadTask " + uploadTask.getId());
        mTaskMap.remove(uploadTask.getId());
        mListener.onSendingFinished(uploadTask);
    }

    @Override
    public void onSendingUploadError(SenderUploadTask uploadTask, Throwable error) {
        // just try to recover
        SenderErrorHandler errorHandler = uploadTask.getSender().getSenderErrorHandler();
        if(errorHandler != null) {
            Log.d(TAG, "Error on SenderUploadTask (Handling...) " + uploadTask.getId(), error);
            errorHandler.tryToRecover(uploadTask);
        } else {
            Log.d(TAG, "Error on SenderUploadTask (Cannot Handle) " + uploadTask.getId(), error);
            onSendingUploadRequestNotRecovered(uploadTask, error);
        }
    }

    @Override
    public void onSendingUploadProgress(SenderUploadTask uploadTask, float progressValue) {
        mListener.onSendingProgress(uploadTask, progressValue);
    }

    @Override
    public void onSendingUploadRequestRecovered(SenderUploadTask previousUploadTask) {
        // try again
        send(previousUploadTask.getMessage(), previousUploadTask.getSender(), previousUploadTask);
    }

    @Override
    public void onSendingUploadRequestNotRecovered(SenderUploadTask previousUploadTask, Throwable error) {
        Message message = previousUploadTask.getMessage();
        Sender nextSender = previousUploadTask.getSender().getFailureChainSender();

        if(nextSender == null) {
            mTaskMap.remove(previousUploadTask.getId());
            if(error != null) {
                mTrackerManager.track(TrackerApi.TYPE_EVENT, error, TAG);
            }
            mListener.onSendingQueued(previousUploadTask, error);
        } else {
            send(message, nextSender, previousUploadTask);
        }
    }

    public Context getContext() {
        return mContext;
    }
}
