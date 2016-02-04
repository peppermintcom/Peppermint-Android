package com.peppermint.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.data.Chat;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.Recording;
import com.peppermint.app.sending.SenderEvent;
import com.peppermint.app.sending.SenderManager;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

/**
 * Service that allows the background_gradient sending of files through different methods.
 */
public class MessagesService extends Service {

    private static final String TAG = MessagesService.class.getSimpleName();

    /**
         Intent extra key for the {@link Recording} containing the recording information and the file to send.
     **/
    public static final String PARAM_MESSAGE_SEND_RECORDING = TAG + "_paramMessageSendRecording";

    /**
        Intent extra key for the {@link Recipient} of the file.
     **/
    public static final String PARAM_MESSAGE_SEND_CHAT = TAG + "_paramMessageSendChat";

    public static final String PARAM_MESSAGE_SEND_RECIPIENT = TAG + "_paramMessageSendRecipient";

    public static final String PARAM_MESSAGE_SEND_AUTHORIZE = TAG + "_paramMessageSendAuthorize";

    public static final String PARAM_MESSAGE_RECEIVE_DATA = TAG + "_paramMessageReceiveData";

    private static final int FIRST_AUDIO_MESSAGE_NOTIFICATION_ID = 1;

    protected SendRecordServiceBinder mBinder = new SendRecordServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class SendRecordServiceBinder extends Binder {

        /**
         * Register an event listener to receive events.
         * @param listener the event listener
         */
        void register(Object listener) {
            mEventBus.register(listener);
        }

        /**
         * Unregister the specified event listener to stop receiving events.
         * @param listener the event listener
         */
        void unregister(Object listener) {
            mEventBus.unregister(listener);
        }

        /**
         * Start a send file request/task that will send the file at the supplied location to the specified recipient.
         * @param recipient the recipient of the file
         * @param recording the recording/file
         * @return the {@link UUID} of the send file request/task
         */
        Message send(Chat chat, Recipient recipient, Recording recording) {
            return MessagesService.this.send(chat, recipient, recording);
        }

        /**
         * Cancel the send request with the specified {@link UUID}.
         * <b>If the send request is ongoing, it might get sent anyway.</b>
         * @param sendingRequestUuid the UUID of the send request returned by {@link #send(Chat, Recipient, Recording)}
         */
        boolean cancel(UUID sendingRequestUuid) {
            return MessagesService.this.cancel(sendingRequestUuid);
        }
        boolean cancel() {
            return MessagesService.this.cancel(null);
        }

        /**
         * Cancel all pending and ongoing send requests.
         * <b>If a send request is ongoing, it might get sent anyway.</b>
         */
        void shutdown() {
            stopSelf();
        }

        boolean isSending() {
            return MessagesService.this.isSending(null);
        }
        boolean isSending(UUID sendingRequestUuid) {
            return MessagesService.this.isSending(sendingRequestUuid);
        }

        void authorize() { MessagesService.this.authorize(); }
    }

    private TrackerManager mTrackerManager;
    private SenderPreferences mPreferences;
    private EventBus mEventBus;
    private SenderManager mSenderManager;
    private DatabaseHelper mDatabaseHelper;

    private Handler mHandler = new Handler();
    private final Runnable mNotificationRunnable = new Runnable() {
        @Override
        public void run() {
            showFirstAudioMessageNotification();
        }
    };

    // broadcast receiver that handles internet connectivity status changes
    private ScheduledThreadPoolExecutor mScheduledExecutor;     // a thread pool that sends queued requests
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
            // it tries to re-execute pending sending requests that failed due to
            // the lack of internet connectivity
            try {
                if (Utils.isInternetAvailable(MessagesService.this)) {
                    SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
                    List<Message> queued = Message.getQueued(db);
                    db.close();
                    if (queued.size() > 0 && Utils.isInternetActive(MessagesService.this)) {
                        // try to resend all queued recordings..
                        for (Message message : queued) {
                            Log.d(TAG, "Re-trying queued request " + message.getId() + " to " + message.getRecipient().getName());
                            if(!mSenderManager.isSending(message.getId())) {
                                mSenderManager.send(message);
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

    public MessagesService() {
        mEventBus = new EventBus();

        // executor for the maintenance routine
        this.mScheduledExecutor = new ScheduledThreadPoolExecutor(1);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mDatabaseHelper = new DatabaseHelper(this);
        mTrackerManager = TrackerManager.getInstance(getApplicationContext());

        mEventBus.register(this, Integer.MAX_VALUE);

        mPreferences = new SenderPreferences(this);

        mSenderManager = new SenderManager(this, mEventBus, new HashMap<String, Object>());
        mSenderManager.init();

        mHandler.postDelayed(mNotificationRunnable, 180000); // after 3mins.

        registerReceiver(mConnectivityChangeReceiver, mConnectivityChangeFilter);
        rescheduleMaintenance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            if(intent.hasExtra(PARAM_MESSAGE_SEND_RECORDING) && intent.hasExtra(PARAM_MESSAGE_SEND_RECIPIENT)) {
                Chat chat = (Chat) intent.getSerializableExtra(PARAM_MESSAGE_SEND_CHAT);
                Recording recording = (Recording) intent.getSerializableExtra(PARAM_MESSAGE_SEND_RECORDING);
                Recipient recipient = (Recipient) intent.getSerializableExtra(PARAM_MESSAGE_SEND_RECIPIENT);
                send(chat, recipient, recording);
            } else if(intent.hasExtra(PARAM_MESSAGE_SEND_AUTHORIZE) && intent.getBooleanExtra(PARAM_MESSAGE_SEND_AUTHORIZE, false)) {
                authorize();
            } else if(intent.hasExtra(PARAM_MESSAGE_RECEIVE_DATA)) {
                Bundle receivedMessageData = intent.getBundleExtra(PARAM_MESSAGE_RECEIVE_DATA);
                Log.d(TAG, "" + receivedMessageData);
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onDestroy() {
        if(mMaintenanceFuture != null) {
            mMaintenanceFuture.cancel(true);
        }
        unregisterReceiver(mConnectivityChangeReceiver);

        mHandler.removeCallbacks(mNotificationRunnable);

        // unregister before deinit() to avoid removing cancelled messages
        // must retry these after reboot
        mEventBus.unregister(this);

        mSenderManager.deinit();

        super.onDestroy();
    }

    private void authorize() {
        mSenderManager.authorize();
    }

    public void onEventMainThread(SenderEvent event) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        switch (event.getType()) {
            case SenderEvent.EVENT_STARTED:
                updateNotification();
                break;
            case SenderEvent.EVENT_ERROR:
                Toast.makeText(this, String.format(getString(R.string.sender_msg_send_error), event.getSenderTask().getMessage().getRecipient().getName()), Toast.LENGTH_SHORT).show();
            case SenderEvent.EVENT_QUEUED:
                if (event.getError().getMessage() != null) {
                    Toast.makeText(this, event.getError().getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case SenderEvent.EVENT_CANCELLED:
                try {
                    Message.delete(db, event.getSenderTask().getMessage());
                } catch (SQLException e) {
                    mTrackerManager.logException(e);
                }
                break;
            case SenderEvent.EVENT_FINISHED:
                // sending request has finished, so update the saved data and mark as sent
                try {
                    event.getSenderTask().getMessage().setSent(true);
                    Message.insertOrUpdate(db, event.getSenderTask().getMessage());
                } catch (SQLException e) {
                    mTrackerManager.logException(e);
                }

                // notifications
                mPreferences.setHasSentMessage(true);
                mHandler.removeCallbacks(mNotificationRunnable);
                dismissFirstAudioMessageNotification();

                if (mSenderManager.isSending()) {
                    removeNotification();
                } else {
                    updateNotification();
                }
                break;
        }
        db.close();
    }

    private Message send(Chat chat, Recipient recipient, Recording recording) {
        // default body if no url is supplied (each sender is currently responsible for building its own message body)
        String body = getString(R.string.sender_default_message);
        Message message = new Message(recording, recipient, getString(R.string.sender_default_mail_subject), body);

        if(chat == null) {
            chat = new Chat(recipient, recording.getRecordedTimestamp());
        }
        message.setChat(chat);

        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        try {
            Message.insertOrUpdate(db, message);
        } catch (SQLException e) {
            mTrackerManager.logException(e);
        }
        db.close();

        mSenderManager.send(message);

        return message;
    }

    private boolean cancel(UUID sendingRequestUuid) {
        if(sendingRequestUuid != null) {
            return mSenderManager.cancel(sendingRequestUuid);
        }
        return mSenderManager.cancel();
    }

    private boolean isSending(UUID sendingRequestUuid) {
        if(sendingRequestUuid != null) {
            return mSenderManager.isSending(sendingRequestUuid);
        }
        return mSenderManager.isSending();
    }

    // UPDATE NOTIFICATION

    private Notification getNotification() {
        Intent notificationIntent = new Intent(MessagesService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(MessagesService.this, 0, notificationIntent, 0);

        // TODO add cancel action to notification perhaps?
        // TODO add progress percentage to the notification whenever possible (depends on sender)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MessagesService.this)
                .setSmallIcon(R.drawable.ic_mail_36dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.uploading))
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    private void removeNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(MessagesService.class.hashCode());
    }

    private void updateNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(MessagesService.class.hashCode(), getNotification());
    }

    // FIRST INSTALL/USE NOTIFICATION

    private boolean showFirstAudioMessageNotification() {
        if(!mPreferences.hasSentMessage()) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            Intent notificationIntent = new Intent(MessagesService.this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(MessagesService.this, 0, notificationIntent, 0);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.you_have_installed_peppermint))
                    .setContentText(getString(R.string.now_send_your_first_audio_message))
                    .setSmallIcon(R.drawable.ic_notification_24dp)
                    .setContentIntent(pendingIntent);

            notificationManager.notify(FIRST_AUDIO_MESSAGE_NOTIFICATION_ID,
                    notificationBuilder.build());

            mPreferences.setHasSentMessage(true);
            return true;
        }

        dismissFirstAudioMessageNotification();
        return false;
    }

    private void dismissFirstAudioMessageNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(FIRST_AUDIO_MESSAGE_NOTIFICATION_ID);
    }
}
