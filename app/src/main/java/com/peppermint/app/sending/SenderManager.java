package com.peppermint.app.sending;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.exceptions.ElectableForQueueingException;
import com.peppermint.app.sending.mail.gmail.GmailSender;
import com.peppermint.app.sending.mail.gmail.GmailSenderPreferences;
import com.peppermint.app.sending.mail.nativemail.IntentMailSender;
import com.peppermint.app.sending.nativesms.IntentSMSSender;
import com.peppermint.app.sending.sms.SMSSender;
import com.peppermint.app.tracking.TrackerManager;
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
    private ScheduledThreadPoolExecutor mScheduledExecutor;     // a thread pool that sends queued requests

    private Map<String, Sender> mSenderMap;                     // map of senders <mime type, sender>
    private Map<String, Sender> mSenderAuthPrefMap;             // map of preference keys which change triggers authorization requests

    private Map<UUID, SenderTask> mTaskMap;                     // map of sending tasks under execution
    private SharedPreferences mPreferences;                     // raw shared preferences

    // broadcast receiver that handles internet connectivity status changes
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
                                if(!mTaskMap.containsKey(sendingRequest.getId())) {
                                    send(sendingRequest);
                                }
                            }
                        } else {
                            Log.d(TAG, "Either internet is not active or there are no queued sending requests... #" + queued.size());
                        }
                    }
                } catch(Throwable e) {
                    Log.e(TAG, "Error on maintenance thread!", e);
                    mTrackerManager.logException(e);
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
        mMaintenanceFuture = mScheduledExecutor.scheduleAtFixedRate(mMaintenanceRunnable, 10, 3600, TimeUnit.SECONDS);
    }

    // if there's a change to a particular preference, trigger the sender authorization routine
    private SharedPreferences.OnSharedPreferenceChangeListener mAuthorizationListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(mSenderAuthPrefMap.containsKey(key)) {
                Sender sender = mSenderAuthPrefMap.get(key);
                if(sender.getPreferences().isEnabled()) {
                    sender.authorize();
                }
            }
        }
    };

    public SenderManager(Context context, EventBus eventBus, Map<String, Object> defaultSenderParameters) {
        super(context,
                TrackerManager.getInstance(context.getApplicationContext()),
                defaultSenderParameters,
                new SenderPreferences(context),
                new DatabaseHelper(context));

        this.mEventBus = eventBus;
        this.mTaskMap = new HashMap<>();
        this.mSenderMap = new HashMap<>();
        this.mSenderAuthPrefMap = new HashMap<>();
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // private thread pool to avoid hanging up other AsyncTasks
        // only one thread so that messages are sent one at a time (allows better control when cancelling)
        this.mExecutor = new ThreadPoolExecutor(/*CPU_COUNT + 1, CPU_COUNT * 2 + 2*/1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        // executor for the maintenance routine
        this.mScheduledExecutor = new ScheduledThreadPoolExecutor(1);

        setParameter(Sender.PARAM_PEPPERMINT_API, new PeppermintApi());

        // here we add all available sender instances to the sender map
        // add gmail api + email intent sender chain
        GmailSender gmailSender = new GmailSender(this, this);
        gmailSender.getParameters().putAll(defaultSenderParameters);
        gmailSender.setTrackerManager(mTrackerManager);

        IntentMailSender intentMailSender = new IntentMailSender(this, this);
        intentMailSender.getParameters().putAll(defaultSenderParameters);
        intentMailSender.setTrackerManager(mTrackerManager);

        // if sending the email through gmail sender fails, try through intent mail sender
        gmailSender.setFailureChainSender(intentMailSender);

        mSenderAuthPrefMap.put(GmailSenderPreferences.ACCOUNT_NAME_KEY, gmailSender);
        mSenderAuthPrefMap.put(GmailSenderPreferences.getEnabledPreferenceKey(GmailSenderPreferences.class), gmailSender);
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
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.init();
                sender = sender.getFailureChainSender();
            }
        }
        mContext.registerReceiver(mConnectivityChangeReceiver, mConnectivityChangeFilter);
        mPreferences.registerOnSharedPreferenceChangeListener(mAuthorizationListener);
        rescheduleMaintenance();
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
        if(mMaintenanceFuture != null) {
            mMaintenanceFuture.cancel(true);
        }
        mPreferences.unregisterOnSharedPreferenceChangeListener(mAuthorizationListener);
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
                    mTrackerManager.logException(e);
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
    }

    /**
     * Requests authorization for senders that require it.
     */
    public void authorize() {
        Sender firstSender = null;
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                if(firstSender == null) {
                    firstSender = sender;
                }
                sender.authorize();
                sender = sender.getFailureChainSender();
            }
        }
        // do this only once
        if(firstSender != null) {
            firstSender.getSenderErrorHandler().authorizePeppermint(null);
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
        while(sender != null && (sender.getPreferences() != null && !sender.getPreferences().isEnabled())) {
            sender = sender.getFailureChainSender();
        }
        if(sender == null) {
            throw new RuntimeException("No sender available. Make sure that all possible senders are not disabled.");
        }

        SenderUploadTask task = sender.newTask(sendingRequest);
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
    public void onSendingUploadStarted(SenderUploadTask uploadTask) {
        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(uploadTask, SenderEvent.EVENT_STARTED));
        }
    }

    @Override
    public void onSendingUploadCancelled(SenderUploadTask uploadTask) {
        mTrackerManager.log("Cancelled SenderUploadTask " + uploadTask.getId());

        // sending request has been cancelled, so update the saved data
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        try {
            SendingRequest.delete(db, uploadTask.getSendingRequest());
        } catch (SQLException e) {
            mTrackerManager.logException(e);
        }
        db.close();

        mTaskMap.remove(uploadTask.getId());

        if(mEventBus != null) {
            mEventBus.post(new SenderEvent(uploadTask, SenderEvent.EVENT_CANCELLED));
        }
    }

    @Override
    public void onSendingUploadFinished(SenderUploadTask uploadTask) {
        mTrackerManager.log("Finished SenderUploadTask " + uploadTask.getId());

        // sending request has finished, so update the saved data and mark as sent
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        try {
            uploadTask.getSendingRequest().setSent(true);
            SendingRequest.insertOrUpdate(db, uploadTask.getSendingRequest());
        } catch (SQLException e) {
            mTrackerManager.logException(e);
        }
        db.close();

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
        send(previousUploadTask.getSendingRequest(), previousUploadTask.getSender());
    }

    @Override
    public void onSendingUploadRequestNotRecovered(SenderUploadTask previousUploadTask, Throwable error) {
        SendingRequest sendingRequest = previousUploadTask.getSendingRequest();
        Sender nextSender = previousUploadTask.getSender().getFailureChainSender();

        if(nextSender == null || error instanceof ElectableForQueueingException) {
            mTaskMap.remove(previousUploadTask.getId());

            SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
            if (error instanceof ElectableForQueueingException) {
                try {
                    sendingRequest.setSent(false);
                    SendingRequest.insertOrUpdate(db, sendingRequest);

                    if (error.getMessage() != null) {
                        Toast.makeText(mContext, error.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    if (mEventBus != null) {
                        mEventBus.post(new SenderEvent(previousUploadTask, SenderEvent.EVENT_QUEUED, error));
                    }
                } catch (SQLException e) {
                    mTrackerManager.logException(e);
                    if (mEventBus != null) {
                        mEventBus.post(new SenderEvent(previousUploadTask, e));
                    }
                }
            } else {
                try {
                    SendingRequest.delete(db, sendingRequest);
                } catch (SQLException e) {
                    // log the exception but the main exception is in the asynctask
                    mTrackerManager.log("Error deleting sending request " + sendingRequest.getId() + ". May not exist and that's ok. " + e.getMessage());
                }

                if (error != null) {
                    mTrackerManager.logException(error);
                }

                if (mEventBus != null) {
                    mEventBus.post(new SenderEvent(previousUploadTask, error));
                }
            }
            db.close();
        } else {
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
