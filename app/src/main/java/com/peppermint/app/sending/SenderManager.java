package com.peppermint.app.sending;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.exceptions.ElectableForQueueingException;
import com.peppermint.app.sending.mail.gmail.GmailSender;
import com.peppermint.app.sending.mail.nativemail.IntentMailSender;
import com.peppermint.app.sending.nativesms.IntentSMSSender;
import com.peppermint.app.sending.server.ServerSender;
import com.peppermint.app.sending.sms.SMSSender;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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
 *     the recipient, and uses it to execute a sending request.
 *</p>
 *
 * For instance, there's a sender for:
 * <ul>
 *     <li>Emails through the Gmail API</li>
 *     <li>Emails through the native Android email app</li>
 *     <li>SMS/text messages through the native Android API</li>
 * </ul>
 */
public class SenderManager implements SenderListener {

    private static final String TAG = SenderManager.class.getSimpleName();

    // setup a private thread pool to avoid hanging up other AsyncTasks
    // private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private Context mContext;
    private EventBus mEventBus;                                 // event bus (listener)
    private ThreadPoolExecutor mExecutor;                       // a thread pool for sending tasks
    private ScheduledThreadPoolExecutor mScheduledExecutor;     // a thread pool that sends queued requests

    private Map<String, Sender> mSenderMap;                     // map of senders <mime type, sender>

    private Map<UUID, SenderTask> mTaskMap;                    // map of sending tasks under execution
    private DatabaseHelper mDatabaseHelper;                     // database helper

    // broadcast receiver that handles connectivity status changes
    private BroadcastReceiver mConnectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Connectivity status has changed! Performing maintenance...");
            rescheduleMaintenance();
        }
    };
    private final IntentFilter mConnectivityChangeFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    private final Runnable mMaintenanceRunnable = new Runnable() {
        @Override
        public void run() {
            // this task is scheduled for execution once the connectivity status changes
            // it basically tries to re-execute pending sending requests that failed due to
            // the lack of internet connectivity
            synchronized (SenderManager.this) {
                try {
                    if (Utils.isInternetAvailable(mContext)) {
                        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
                        List<SendingRequest> queued = SendingRequest.getQueued(db);
                        db.close();
                        if (queued.size() > 0 && Utils.isInternetActive(mContext)) {
                            // try to resend all queued recordings..
                            for (SendingRequest sendingRequest : queued) {
                                Log.d(TAG, "Re-trying queued request " + sendingRequest.getId() + " to " + sendingRequest.getRecipient().getName());
                                send(sendingRequest);
                            }
                        } else {
                            Log.d(TAG, "Either internet is not active or there are no queued sending requests... #" + queued.size());
                        }
                    }
                } catch(Throwable e) {
                    Log.e(TAG, "Error on maintenance thread!", e);
                    Crashlytics.logException(e);
                }
            }
        }
    };
    private ScheduledFuture<?> mMaintenanceFuture;

    private void rescheduleMaintenance() {
        // if it is scheduled, cancel and reschedule
        if(mMaintenanceFuture != null) {
            mMaintenanceFuture.cancel(true);
        }

        // run one time (almost) immediately
        mMaintenanceFuture = mScheduledExecutor.scheduleAtFixedRate(mMaintenanceRunnable, 5, 3600, TimeUnit.SECONDS);
    }

    public SenderManager(Context context, EventBus eventBus, Map<String, Object> defaultSenderParameters) {
        this.mContext = context;
        this.mEventBus = eventBus;
        this.mTaskMap = new HashMap<>();
        this.mSenderMap = new HashMap<>();

        this.mDatabaseHelper = new DatabaseHelper(mContext);

        // private thread pool to avoid hanging up other AsyncTasks
        this.mExecutor = new ThreadPoolExecutor(/*CPU_COUNT + 1, CPU_COUNT * 2 + 2*/1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        this.mScheduledExecutor = new ScheduledThreadPoolExecutor(1);

        // here we add all available sender instances to the sender map
        // add gmail api + email intent sender chain
        ServerSender gmailServerSender = new ServerSender(mContext, this);
        gmailServerSender.getParameters().putAll(defaultSenderParameters);

        GmailSender gmailSender = new GmailSender(mContext, this);
        gmailSender.getParameters().putAll(defaultSenderParameters);

        IntentMailSender intentMailSender = new IntentMailSender(mContext, this);
        intentMailSender.getParameters().putAll(defaultSenderParameters);

        gmailServerSender.setSuccessChainSender(gmailSender);
        gmailSender.setFailureChainSender(intentMailSender);

        mSenderMap.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, gmailServerSender);

        //sms/text message sender
        ServerSender smsServerSender = new ServerSender(mContext, this);
        smsServerSender.getParameters().putAll(defaultSenderParameters);

        SMSSender smsSender = new SMSSender(mContext, this);
        smsSender.getParameters().putAll(defaultSenderParameters);

        smsServerSender.setSuccessChainSender(smsSender);

        IntentSMSSender intentSmsSender = new IntentSMSSender(mContext, this);
        intentSmsSender.getParameters().putAll(defaultSenderParameters);

        smsSender.setFailureChainSender(intentSmsSender);

        mSenderMap.put(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, smsServerSender);
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
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.init();
                sender = sender.getFailureChainSender();
            }
        }
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.init();
                sender = sender.getSuccessChainSender();
            }
        }
        mContext.registerReceiver(mConnectivityChangeReceiver, mConnectivityChangeFilter);
        rescheduleMaintenance();
    }

    /**
     * De-initializes the manager:
     * <ol>
     *     <li>Cancels the queued sending request maintenance task</li>
     *     <li>Unregisters the manager to stop listening for internet connectivity status changes</li>
     *     <li>Cancels all running sending tasks</li>
     *     <li>De-initializes all {@link Sender}s</li>
     * </ol>
     */
    public void deinit() {
        if(mMaintenanceFuture != null) {
            mMaintenanceFuture.cancel(true);
        }
        mContext.unregisterReceiver(mConnectivityChangeReceiver);

        // save tasks that were not cancelled and are being executed
        // these will be re-executed once the service restarts
        DatabaseHelper dbHelper = new DatabaseHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        for(UUID uuid : mTaskMap.keySet()) {
            SenderTask task = mTaskMap.get(uuid);
            if(!task.isCancelled() && !task.getSendingRequest().isSent()) {
                task.cancel(true);
                try {
                    SendingRequest.insertOrUpdate(db, task.getSendingRequest());
                } catch (SQLException e) {
                    Crashlytics.logException(e);
                }
            }
        }
        db.close();

        cancel();

        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.deinit();
                sender = sender.getFailureChainSender();
            }
        }
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.deinit();
                sender = sender.getSuccessChainSender();
            }
        }
    }

    public void authorize() {
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                SenderTask authorizationTask = sender.newAuthorizationTask();
                if(authorizationTask != null) {
                    authorizationTask.executeOnExecutor(mExecutor);
                }
                sender = sender.getFailureChainSender();
            }
        }
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                SenderTask authorizationTask = sender.newAuthorizationTask();
                if(authorizationTask != null) {
                    authorizationTask.executeOnExecutor(mExecutor);
                }
                sender = sender.getSuccessChainSender();
            }
        }
    }

    /**
     * Tries to execute the specified sending request using one of the available {@link Sender}s.
     * @param sendingRequest the sending request
     * @return the UUID of the sending request
     */
    public UUID send(SendingRequest sendingRequest) {
        String mimeType = sendingRequest.getRecipient().getMimeType();

        // check if there's a sender for the specified recipient mime type
        if(!mSenderMap.containsKey(mimeType)) {
            throw new NullPointerException("SenderManager for mime type " + mimeType + " not found!");
        }

        Sender sender = mSenderMap.get(mimeType);
        return send(sendingRequest, sender);
    }

    /**
     * Tries to execute the specified sending request using the specified {@link Sender}.
     * @param sendingRequest the sending request
     * @param sender the sender
     * @return the UUID of the sending request
     */
    private UUID send(SendingRequest sendingRequest, Sender sender) {
        while(sender != null && (sender.getSenderPreferences() != null && !sender.getSenderPreferences().isEnabled())) {
            sender = sender.getFailureChainSender();
        }
        if(sender == null) {
            throw new RuntimeException("No sender available. Make sure that all possible senders are not disabled.");
        }

        SenderTask task = sender.newTask(sendingRequest);
        if(mTaskMap.containsKey(task.getId())) {
            task.setRecovering(true);
        }
        mTaskMap.put(task.getId(), task);
        task.executeOnExecutor(mExecutor);
        return sendingRequest.getId();
    }

    /**
     * Cancels the sending task executing the sending request with the specified UUID.
     * @param uuid the UUID
     * @return true if a sending task was cancelled; false otherwise
     */
    public boolean cancel(UUID uuid) {
        if(mTaskMap.containsKey(uuid)) {
            mTaskMap.get(uuid).cancel(true);
            return true;
        }
        return false;
    }

    /**
     * Cancels all sending tasks under execution.
     */
    public boolean cancel() {
        boolean canceledSome = false;
        for(UUID uuid : mTaskMap.keySet()) {
            if(mTaskMap.get(uuid).cancel(true)) {
                canceledSome = true;
            }
        }
        return canceledSome;
    }

    /**
     * Checks if there's a sending task executing a sending request with the specified UUID.
     * @param uuid the UUID
     * @return true if sending/executing; false otherwise
     */
    public boolean isSending(UUID uuid) {
        return mTaskMap.containsKey(uuid) && !mTaskMap.get(uuid).isCancelled();
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
        Iterator<Map.Entry<UUID, SenderTask>> it = mTaskMap.entrySet().iterator();
        while(it.hasNext() && !someOngoing) {
            if(!it.next().getValue().isCancelled()) {
                someOngoing = true;
            }
        }

        return someOngoing;
    }

    @Override
    public void onSendingTaskStarted(SenderTask sendingTask) {
        if(sendingTask instanceof SenderAuthorizationTask) {
            return;
        }

        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(sendingTask, SenderEvent.EVENT_STARTED));
        }
    }

    @Override
    public void onSendingTaskCancelled(SenderTask sendingTask) {
        if(sendingTask instanceof SenderAuthorizationTask) {
            return;
        }

        mTaskMap.remove(sendingTask.getId());
        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(sendingTask, SenderEvent.EVENT_CANCELLED));
        }
    }

    @Override
    public void onSendingTaskFinished(SenderTask sendingTask) {
        if(sendingTask instanceof SenderAuthorizationTask) {
            return;
        }

        Sender nextSender = sendingTask.getSender().getSuccessChainSender();
        if(nextSender != null) {
            if(mEventBus != null) {
                mEventBus.post(new SenderEvent(sendingTask, SenderEvent.EVENT_INTERMEDIATE_FINISHED));
            }

            try {
                send(sendingTask.getSendingRequest(), nextSender);
            } catch(Throwable t) {
                onSendingRequestNotRecovered(sendingTask, t);
            }

            return;
        }

        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        try {
            sendingTask.getSendingRequest().setSent(true);
            SendingRequest.insertOrUpdate(db, sendingTask.getSendingRequest());
        } catch (SQLException e) {
            Crashlytics.logException(e);
        }
        db.close();

        mTaskMap.remove(sendingTask.getId());
        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(sendingTask, SenderEvent.EVENT_FINISHED));
        }
    }

    @Override
    public void onSendingTaskError(SenderTask sendingTask, Throwable error) {
        // just try to recover
        SenderErrorHandler errorHandler = sendingTask.getSender().getErrorHandler();
        if(errorHandler != null) {
            errorHandler.tryToRecover(sendingTask);
        } else {
            onSendingRequestNotRecovered(sendingTask, sendingTask.getError());
        }
    }

    @Override
    public void onSendingTaskProgress(SenderTask sendingTask, float progressValue) {
        if(sendingTask instanceof SenderAuthorizationTask) {
            return;
        }

        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(sendingTask, SenderEvent.EVENT_PROGRESS));
        }
    }

    @Override
    public void onSendingRequestRecovered(SenderTask previousSendingTask) {
        if(previousSendingTask instanceof SenderAuthorizationTask) {
            return;
        }

        // try again
        send(previousSendingTask.getSendingRequest(), previousSendingTask.getSender());
    }

    @Override
    public void onSendingRequestNotRecovered(SenderTask previousSendingTask, Throwable error) {
        if(previousSendingTask instanceof  SenderAuthorizationTask) {
            Crashlytics.logException(error);
            Log.e(TAG, "Unable to recover from error while requesting authorization", error);
            return;
        }

        SendingRequest sendingRequest = previousSendingTask.getSendingRequest();
        //Throwable error = previousSendingTask.getError();
        Sender nextSender = previousSendingTask.getSender().getFailureChainSender();
        if(nextSender == null || error instanceof ElectableForQueueingException) {
            mTaskMap.remove(previousSendingTask.getId());
            SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
            if(error instanceof ElectableForQueueingException) {
                try {
                    sendingRequest.setSent(false);
                    SendingRequest.insertOrUpdate(db, sendingRequest);

                    if(error.getMessage() != null) {
                        Toast.makeText(mContext, error.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    if (mEventBus != null) {
                        mEventBus.post(new SenderEvent(previousSendingTask, SenderEvent.EVENT_QUEUED, error));
                    }
                } catch (SQLException e) {
                    Crashlytics.logException(e);
                    if (mEventBus != null) {
                        mEventBus.post(new SenderEvent(previousSendingTask, e));
                    }
                }
            } else {
                try {
                    SendingRequest.delete(db, sendingRequest);
                } catch (SQLException e) {
                    // log the exception but the main exception is in the asynctask
                    Log.e(TAG, "Error deleting sending request " + sendingRequest.getId() + ". May not exist and that's ok.", e);
                }

                if(error != null) {
                    Crashlytics.log("Exception message - " + error.getMessage());
                    Crashlytics.logException(error);
                }

                if (mEventBus != null) {
                    mEventBus.post(new SenderEvent(previousSendingTask, error));
                }
            }
            db.close();
        } else {
            Crashlytics.log(Log.WARN, TAG, "Chain sender required due to error... " + error.toString());
            send(sendingRequest, nextSender);
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
