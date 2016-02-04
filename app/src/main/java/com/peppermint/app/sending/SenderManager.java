package com.peppermint.app.sending;

import android.content.Context;
import android.provider.ContactsContract;

import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.sending.exceptions.ElectableForQueueingException;
import com.peppermint.app.sending.mail.gmail.GmailSender;
import com.peppermint.app.sending.mail.nativemail.IntentMailSender;
import com.peppermint.app.sending.nativesms.IntentSMSSender;
import com.peppermint.app.sending.sms.SMSSender;
import com.peppermint.app.tracking.TrackerManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * <p>
 *     Main class of the Sender API, which allows sending an audio/video recording through
 *     different protocols in different ways.<br />
 *     Each {@link Sender} is associated with one or more contact mime types
 *     (e.g. "vnd.android.cursor.dir/phone_v2" or "vnd.android.cursor.item/email_v2").<br />
 *     When sending, the manager searches for a {@link Sender} that handles the mime type of
 *     the recipient contact, and uses it to execute a sending request.
 *</p>
 *
 * For instance, there's a sender for:
 * <ul>
 *     <li>Emails through the Gmail API</li>
 *     <li>Emails through the native Android email app</li>
 *     <li>SMS/text messages through the native Android API</li>
 *     <li>SMS/text messages through the native SMS app</li>
 * </ul>
 */
public class SenderManager extends SenderObject implements SenderUploadListener {

    private static final String TAG = SenderManager.class.getSimpleName();

    private EventBus mEventBus;                                 // event bus (listener)
    private ThreadPoolExecutor mExecutor;                       // a thread pool for sending tasks

    /*private InAppSender mRootSender;                            // root in-app sender (always try it first)*/
    private Map<String, Sender> mSenderMap;                     // map of senders <mime type, sender>

    private Map<UUID, SenderTask> mTaskMap;                     // map of sending tasks under execution

    public SenderManager(Context context, EventBus eventBus, Map<String, Object> defaultSenderParameters) {
        super(context,
                TrackerManager.getInstance(context.getApplicationContext()),
                defaultSenderParameters,
                new SenderPreferences(context),
                new DatabaseHelper(context));

        this.mEventBus = eventBus;
        this.mTaskMap = new ConcurrentHashMap<>();
        this.mSenderMap = new HashMap<>();

        // private thread pool to avoid hanging up other AsyncTasks
        // only one thread so that messages are sent one at a time (allows better control when cancelling)
        this.mExecutor = new ThreadPoolExecutor(/*CPU_COUNT + 1, CPU_COUNT * 2 + 2*/1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        // here we add all available sender instances to the sender map
        /*mRootSender = new InAppSender(this, this);
        mRootSender.getParameters().putAll(defaultSenderParameters);
        mRootSender.setTrackerManager(mTrackerManager);*/

        // add gmail api + email intent sender chain
        GmailSender gmailSender = new GmailSender(this, this);
        gmailSender.getParameters().putAll(defaultSenderParameters);
        gmailSender.setTrackerManager(mTrackerManager);

        IntentMailSender intentMailSender = new IntentMailSender(this, this);
        intentMailSender.getParameters().putAll(defaultSenderParameters);
        intentMailSender.setTrackerManager(mTrackerManager);

        // if sending the email through gmail sender fails, try through intent mail sender
        gmailSender.setFailureChainSender(intentMailSender);

        mSenderMap.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, gmailSender);

        //sms/text message sender
        SMSSender smsSender = new SMSSender(this, this);
        smsSender.getParameters().putAll(defaultSenderParameters);
        smsSender.setTrackerManager(mTrackerManager);

        IntentSMSSender intentSmsSender = new IntentSMSSender(this, this);
        intentSmsSender.getParameters().putAll(defaultSenderParameters);
        intentSmsSender.setTrackerManager(mTrackerManager);

        // if sending the email through SMS sender fails, try through intent SMS sender
        smsSender.setFailureChainSender(intentSmsSender);

        mSenderMap.put(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, smsSender);
    }

    /**
     * Initializes the manager:
     * <ol>
     *     <li>Initializes all {@link Sender}s</li>
     *     <li>Register the manager to listen to internet connectivity status changes</li>
     *     <li>Schedules the queued sending request maintenance task</li>
     * </ol>
     * <p><strong>{@link #deinit()} must always be invoked when the manager is no longer needed.</strong></p>
     */
    public void init() {
        /*mRootSender.init();*/
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
     *     <li>Cancels the queued sending request maintenance task</li>
     *     <li>Unregisters the manager to stop listening for internet connectivity status changes</li>
     *     <li>Cancels all running sending tasks and saves them for later re-sending</li>
     *     <li>De-initializes all {@link Sender}s</li>
     * </ol>
     */
    public void deinit() {
        // cancel all tasks
        cancel();

        /*mRootSender.deinit();*/
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.deinit();
                sender = sender.getFailureChainSender();
            }
        }
    }

    /**
     * Tries to execute the specified sending request using one of the available {@link Sender}s.
     * @param message the sending request
     * @return the UUID of the sending request
     */
    public void send(Message message) {
        send(message, null);
    }

    public void send(Message message, SenderUploadTask previousFailedTask) {
        String mimeType = message.getRecipient().getMimeType();

        // check if there's a sender for the specified recipient mime type
        if(!mSenderMap.containsKey(mimeType)) {
            throw new NullPointerException("Sender for mime type " + mimeType + " not found!");
        }

        Sender sender = mSenderMap.get(mimeType);
        send(message, sender, previousFailedTask);
    }

    /**
     * Tries to execute the specified sending request using the specified {@link Sender}.
     * @param message the sending request
     * @param sender the sender
     * @return the UUID of the sending request
     */
    private void send(Message message, Sender sender, SenderUploadTask previousFailedTask) {
        while(sender != null && (sender.getPreferences() != null && !sender.getPreferences().isEnabled())) {
            sender = sender.getFailureChainSender();
        }
        if(sender == null) {
            throw new RuntimeException("No sender available. Make sure that all possible senders are not disabled.");
        }

        /*if(previousFailedTask == null) {
            SenderUploadTask rootTask = mRootSender.newTask(message, null);
            mTaskMap.put(rootTask.getId(), rootTask);
            rootTask.executeOnExecutor(mExecutor);
        }*/

        SenderUploadTask task = sender.newTask(message, previousFailedTask == null ? null : previousFailedTask.getId());
        task.setRecovering(previousFailedTask != null);
        mTaskMap.put(task.getId(), task);
        task.executeOnExecutor(mExecutor);
    }

    /**
     * Cancels the sending task executing the sending request with the specified UUID.
     * @param message the UUID
     * @return true if a sending task was cancelled; false otherwise
     */
    public boolean cancel(Message message) {
        boolean cancelled = false;
        for(SenderTask senderTask : mTaskMap.values()) {
            if(senderTask.getMessage().equals(message)) {
                cancelled = true;
                senderTask.cancel(true);
            }
        }
        return cancelled;
    }

    /**
     * Cancels all sending tasks under execution.
     */
    public boolean cancel() {
        boolean canceledSome = false;
        for(SenderTask senderTask : mTaskMap.values()) {
            if(senderTask.cancel(true)) {
                canceledSome = true;
            }
        }
        return canceledSome;
    }

    /**
     * Checks if there's a sending task executing a sending request with the specified UUID.
     * @param message the UUID
     * @return true if sending/executing; false otherwise
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
     * Checks if there's at least one sending task under execution.
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
    public void onSendingUploadStarted(SenderUploadTask uploadTask) {
        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(uploadTask, SenderEvent.EVENT_STARTED));
        }
    }

    @Override
    public void onSendingUploadCancelled(SenderUploadTask uploadTask) {
        mTrackerManager.log("Cancelled SenderUploadTask " + uploadTask.getId());

        mTaskMap.remove(uploadTask.getId());

        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(uploadTask, SenderEvent.EVENT_CANCELLED));
        }
    }

    @Override
    public void onSendingUploadFinished(SenderUploadTask uploadTask) {
        mTrackerManager.log("Finished SenderUploadTask " + uploadTask.getId());

        mTaskMap.remove(uploadTask.getId());

        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(uploadTask, SenderEvent.EVENT_FINISHED));
        }
    }

    @Override
    public void onSendingUploadError(SenderUploadTask uploadTask, Throwable error) {
        // just try to recover
        SenderErrorHandler errorHandler = uploadTask.getSender().getSenderErrorHandler();
        if(errorHandler != null) {
            mTrackerManager.log("Error on SenderUploadTask (Handling...) " + uploadTask.getId(), error);
            errorHandler.tryToRecover(uploadTask);
        } else {
            mTrackerManager.log("Error on SenderUploadTask (Cannot Handle) " + uploadTask.getId(), error);
            onSendingUploadRequestNotRecovered(uploadTask, error);
        }
    }

    @Override
    public void onSendingUploadProgress(SenderUploadTask uploadTask, float progressValue) {
        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(uploadTask, SenderEvent.EVENT_PROGRESS));
        }
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

        if(nextSender == null || error instanceof ElectableForQueueingException) {
            mTaskMap.remove(previousUploadTask.getId());
            if (error instanceof ElectableForQueueingException) {
                if (mEventBus != null) {
                    mEventBus.post(new SenderEvent(previousUploadTask, SenderEvent.EVENT_QUEUED, error));
                }
            } else {
                mTrackerManager.logException(error);

                if (mEventBus != null) {
                    mEventBus.post(new SenderEvent(previousUploadTask, error));
                }
            }
        } else {
            send(message, nextSender, previousUploadTask);
        }
    }

    public EventBus getEventBus() {
        return mEventBus;
    }

    public void setEventBus(EventBus mEventBus) {
        this.mEventBus = mEventBus;
    }

    public Context getContext() {
        return mContext;
    }
}
