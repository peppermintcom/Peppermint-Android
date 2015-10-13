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
import com.peppermint.app.R;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.exceptions.ElectableForQueueingException;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.sending.gmail.GmailSender;
import com.peppermint.app.sending.nativemail.IntentMailSender;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.HashMap;
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
 * Base class for Senders. A SenderManager represents a method of sending an audio/video recording.
 * For instance, there's a sender for emails and a sender for SMS/text messages.
 */
public class SenderManager implements SenderListener {

    private static final String TAG = SenderManager.class.getSimpleName();

    // setup a private thread pool to avoid hanging up other AsyncTasks
    // private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private Context mContext;
    private EventBus mEventBus;
    private ThreadPoolExecutor mExecutor;
    private ScheduledThreadPoolExecutor mScheduledExecutor;

    private Map<String, Sender> mSenderMap;             // <mime type, sending task factory>

    private Map<UUID, SendingTask> mTaskMap;
    private DatabaseHelper mDatabaseHelper;

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
        // if it's already running do nothing
       /* if(mRunningMaintenance) {
            return;
        }
*/
        // if it is scheduled, cancel and reschedule
        if(mMaintenanceFuture != null) {
            mMaintenanceFuture.cancel(true);
        }

        // run one time immediately
        mMaintenanceFuture = mScheduledExecutor.scheduleAtFixedRate(mMaintenanceRunnable, 5, 3600, TimeUnit.SECONDS);
    }

    public SenderManager(Context context, EventBus eventBus, Map<String, Object> defaultSenderParameters) {
        this.mContext = context;
        this.mEventBus = eventBus;
        this.mTaskMap = new HashMap<>();
        this.mSenderMap = new HashMap<>();

        // private thread pool to avoid hanging up other AsyncTasks
        this.mExecutor = new ThreadPoolExecutor(/*CPU_COUNT + 1, CPU_COUNT * 2 + 2*/1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        this.mScheduledExecutor = new ScheduledThreadPoolExecutor(1);
        //this.mScheduledExecutor.setMaximumPoolSize(1);

        // INIT
        // init all available senders
        // gmail api + email intent sender chain
        GmailSender gmailSender = new GmailSender(mContext, this);
        gmailSender.getParameters().putAll(defaultSenderParameters);

        IntentMailSender intentMailSender = new IntentMailSender(mContext, this);
        intentMailSender.getParameters().putAll(defaultSenderParameters);

        gmailSender.setFailureChainSender(intentMailSender);

        mSenderMap.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, gmailSender);

        mDatabaseHelper = new DatabaseHelper(mContext);
    }

    public void init() {
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.init();
                sender = sender.getFailureChainSender();
            }
        }
        mContext.registerReceiver(mConnectivityChangeReceiver, mConnectivityChangeFilter);
        rescheduleMaintenance();
    }

    public void deinit() {
        if(mMaintenanceFuture != null) {
            mMaintenanceFuture.cancel(true);
        }
        mContext.unregisterReceiver(mConnectivityChangeReceiver);
        cancel();
        for(Sender sender : mSenderMap.values()) {
            while(sender != null) {
                sender.deinit();
                sender = sender.getFailureChainSender();
            }
        }
    }

    public UUID send(SendingRequest sendingRequest) {
        String mimeType = sendingRequest.getRecipient().getMimeType();

        // TODO remove this "IF" once SMS send is implemented
        if(mimeType.compareToIgnoreCase(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) == 0) {
            Toast.makeText(mContext, "Sending through SMS/text message is not yet implemented. Please try again later.", Toast.LENGTH_LONG).show();
            return null;
        }

        // check if there's a sender for the specified recipient mime type
        if(!mSenderMap.containsKey(mimeType)) {
            throw new NullPointerException("SenderManager for mime type " + mimeType + " not found!");
        }

        Sender sender = mSenderMap.get(mimeType);
        return send(sendingRequest, sender);
    }

    private UUID send(SendingRequest sendingRequest, Sender sender) {
        SendingTask task = sender.newTask(sendingRequest);
        task.executeOnExecutor(mExecutor);
        mTaskMap.put(sendingRequest.getId(), task);
        return sendingRequest.getId();
    }

    public boolean cancel(UUID uuid) {
        if(mTaskMap.containsKey(uuid)) {
            mTaskMap.get(uuid).cancel(true);
            return true;
        }
        return false;
    }

    public void cancel() {
        for(UUID uuid : mTaskMap.keySet()) {
            mTaskMap.get(uuid).cancel(true);
        }
    }

    public boolean isSending(UUID uuid) {
        return mTaskMap.containsKey(uuid);
    }

    public boolean isSending() {
        return mTaskMap.size() > 0;
    }

    @Override
    public void onSendingTaskStarted(SendingTask sendingTask, SendingRequest sendingRequest) {
        if(mEventBus != null) {
            mEventBus.post(new SendingEvent(sendingTask, SendingEvent.EVENT_STARTED));
        }
    }

    @Override
    public void onSendingTaskCancelled(SendingTask sendingTask, SendingRequest sendingRequest) {
        if(mEventBus != null) {
            mEventBus.post(new SendingEvent(sendingTask, SendingEvent.EVENT_CANCELLED));
        }
    }

    @Override
    public void onSendingTaskFinished(SendingTask sendingTask, SendingRequest sendingRequest) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        try {
            sendingRequest.setSent(true);
            SendingRequest.insertOrUpdate(db, sendingRequest);
        } catch (SQLException e) {
            Crashlytics.logException(e);
        }
        db.close();

        mTaskMap.remove(sendingRequest.getId());
        if(mEventBus != null) {
            mEventBus.post(new SendingEvent(sendingTask, SendingEvent.EVENT_FINISHED));
        }
    }

    @Override
    public void onSendingTaskError(SendingTask sendingTask, SendingRequest sendingRequest, Throwable error) {
        // just try to recover
        SendingErrorHandler errorHandler = sendingTask.getSender().getErrorHandler();
        if(errorHandler != null) {
            errorHandler.tryToRecover(sendingTask);
        } else {
            onSendingRequestNotRecovered(sendingTask, sendingRequest);
        }
    }

    @Override
    public void onSendingTaskProgress(SendingTask sendingTask, SendingRequest sendingRequest, float progressValue) {
        if(mEventBus != null) {
            mEventBus.post(new SendingEvent(sendingTask, SendingEvent.EVENT_PROGRESS));
        }
    }

    @Override
    public void onSendingRequestRecovered(SendingTask previousSendingTask, SendingRequest sendingRequest) {
        // try again
        send(sendingRequest, previousSendingTask.getSender());
    }

    @Override
    public void onSendingRequestNotRecovered(SendingTask previousSendingTask, SendingRequest sendingRequest) {
        Throwable error = previousSendingTask.getError();
        Sender nextSender = previousSendingTask.getSender().getFailureChainSender();
        if(nextSender == null || error instanceof ElectableForQueueingException) {
            mTaskMap.remove(sendingRequest.getId());
            if(error instanceof ElectableForQueueingException) {
                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                try {
                    sendingRequest.setSent(false);
                    SendingRequest.insertOrUpdate(db, sendingRequest);

                    if(error instanceof NoInternetConnectionException) {
                        Toast.makeText(mContext, R.string.msg_no_internet, Toast.LENGTH_SHORT).show();
                    }

                    if (mEventBus != null) {
                        mEventBus.post(new SendingEvent(previousSendingTask, SendingEvent.EVENT_QUEUED, error));
                    }
                } catch (SQLException e) {
                    Crashlytics.logException(e);
                    if (mEventBus != null) {
                        mEventBus.post(new SendingEvent(previousSendingTask, e));
                    }
                }
                db.close();
            } else {
                Crashlytics.logException(previousSendingTask.getError());
                if (mEventBus != null) {
                    mEventBus.post(new SendingEvent(previousSendingTask, error));
                }
            }
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
