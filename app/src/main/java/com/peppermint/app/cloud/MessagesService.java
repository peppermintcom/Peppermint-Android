package com.peppermint.app.cloud;

import android.Manifest;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.StartupActivity;
import com.peppermint.app.authenticator.AuthenticatorUtils;
import com.peppermint.app.cloud.apis.exceptions.GoogleApiNoAuthorizationException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiNoAccountException;
import com.peppermint.app.cloud.gcm.RegistrationIntentService;
import com.peppermint.app.cloud.senders.SenderManager;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.cloud.senders.SenderSupportListener;
import com.peppermint.app.cloud.senders.SenderSupportTask;
import com.peppermint.app.cloud.senders.exceptions.NoInternetConnectionException;
import com.peppermint.app.cloud.senders.exceptions.NoPlayServicesException;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.GlobalManager;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.data.Recording;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.ReceiverEvent;
import com.peppermint.app.events.SenderEvent;
import com.peppermint.app.events.SyncEvent;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.PermissionsPolicyEnforcer;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.utils.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Service handles sending and receiving {@link Message}s.
 */
public class MessagesService extends Service {

    private static final String TAG = MessagesService.class.getSimpleName();

    public static final String ACTION_CANCEL = "com.peppermint.app.cloud.MessagesService.CANCEL";
    public static final String ACTION_DO_PENDING_LOGOUTS = "com.peppermint.app.cloud.MessagesService.DO_PENDING_LOGOUTS";

    // intent parameters to send a message
    public static final String PARAM_MESSAGE_SEND_RECORDING = TAG + "_paramMessageSendRecording";
    public static final String PARAM_MESSAGE_SEND_CHAT = TAG + "_paramMessageSendChat";
    public static final String PARAM_DO_SYNC = TAG + "_paramDoSync";

    // intent parameters to receive a message
    public static final String PARAM_MESSAGE_RECEIVE_DATA = TAG + "_paramMessageReceiveData";

    private static final int FIRST_AUDIO_MESSAGE_NOTIFICATION_ID = 1;

    protected SendRecordServiceBinder mBinder = new SendRecordServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class SendRecordServiceBinder extends Binder {

        /**
         * Start a send file request/task that will send the file at the supplied location to the specified recipient.
         * @param chat the chat to send the recording to
         * @param recording the recording/file
         * @return the {@link UUID} of the send file request/task
         */
        Message send(Chat chat, Recording recording) {
            return MessagesService.this.send(chat, recording);
        }

        boolean retry(Message message) {
            return MessagesService.this.retry(message);
        }

        /**
         * Cancel the message with the specified {@link UUID}.
         * <b>If the message is being sent, it might get sent anyway.</b>
         * @param message the UUID of the message returned by {@link #send(Chat, Recording)}
         */
        boolean cancel(Message message) {
            return MessagesService.this.cancel(message);
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
        boolean isSending(Message message) {
            return MessagesService.this.isSending(message);
        }

        boolean isSendingAndCancellable(Message message) {
            return MessagesService.this.isSendingAndCancellable(message);
        }

        void markAsPlayed(Message message) {
            MessagesService.this.markAsPlayed(message);
        }

        void removeAllNotifications() {
            MessagesService.this.removeAllNotifications();
        }
    }

    private PermissionsPolicyEnforcer mPermissionsManager = new PermissionsPolicyEnforcer(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE);

    private TrackerManager mTrackerManager;
    private SenderPreferences mPreferences;
    private SenderManager mSenderManager;
    private AuthenticatorUtils mAuthenticatorUtils;
    private PendingLogoutPeppermintTask mPendingLogoutPeppermintTask;

    private MessagesSyncTask mMessagesSyncTask;
    private SenderSupportListener mMessagesSyncTaskListener = new SenderSupportListener() {
        @Override
        public void onSendingSupportStarted(SenderSupportTask supportTask) {
            PeppermintEventBus.postSyncEvent(SyncEvent.EVENT_STARTED, mMessagesSyncTask.getReceivedMessages(), mMessagesSyncTask.getSentMessages(), null);
        }

        @Override
        public void onSendingSupportCancelled(SenderSupportTask supportTask) {
            PeppermintEventBus.postSyncEvent(SyncEvent.EVENT_CANCELLED, mMessagesSyncTask.getReceivedMessages(), mMessagesSyncTask.getSentMessages(), null);
        }

        @Override
        public void onSendingSupportFinished(SenderSupportTask supportTask) {
            refreshBadge();
            PeppermintEventBus.postSyncEvent(SyncEvent.EVENT_FINISHED, mMessagesSyncTask.getReceivedMessages(), mMessagesSyncTask.getSentMessages(), null);
        }

        @Override
        public void onSendingSupportError(SenderSupportTask supportTask, Throwable error) {
            if(!(error instanceof NoInternetConnectionException)) {
                mTrackerManager.logException(error);
            }
            PeppermintEventBus.postSyncEvent(SyncEvent.EVENT_ERROR, mMessagesSyncTask.getReceivedMessages(), mMessagesSyncTask.getSentMessages(), error);
        }

        @Override
        public void onSendingSupportProgress(SenderSupportTask supportTask, float progressValue) {
        }
    };

    private void runMessagesSync() {
        if(mPermissionsManager.getPermissionsToAsk(MessagesService.this).size() > 0) {
            Log.d(TAG, "Not triggering sync since permissions are required...");
            return;
        }

        if(mMessagesSyncTask == null || mMessagesSyncTask.getStatus() == AsyncTask.Status.FINISHED) {
            mMessagesSyncTask = new MessagesSyncTask(this, mMessagesSyncTaskListener);
            mMessagesSyncTask.executeOnExecutor(mScheduledExecutor);
        }
    }

    private Message handleReceivedMessage(String emailAddress, String serverId, String audioUrl, String transcription, String receiverEmail, String senderEmail, String senderName, String createdTs, int durationSeconds) {
        if(audioUrl == null || senderEmail == null || receiverEmail == null) {
            mTrackerManager.log("Invalid GCM notification received! Either or both audio URL and sender email are null.");
            return null;
        }

        if(emailAddress == null) {
            return null;
        }

        if(emailAddress.compareToIgnoreCase(receiverEmail.trim()) != 0) {
            mTrackerManager.log("Received wrong message from GCM! Should have gone to email " + receiverEmail);
            return null;
        }

        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
        databaseHelper.lock();
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        Message message = null;
        try {
            message = GlobalManager.insertReceivedMessage(this, db, receiverEmail, senderName, senderEmail, audioUrl, serverId, transcription, createdTs, durationSeconds, null);
            db.setTransactionSuccessful();
        } catch (ContactManager.InvalidViaException | ContactManager.InvalidNameException e) {
            mTrackerManager.log(senderName + " - " + senderEmail);
            mTrackerManager.logException(e);
        } catch (SQLException e) {
            mTrackerManager.logException(e);
        }

        db.endTransaction();
        databaseHelper.unlock();

        return message;
    }

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
                    doPendingLogouts();
                    doGcmRegistration();

                    List<Message> queued = MessageManager.getMessagesQueued(DatabaseHelper.getInstance(MessagesService.this).getReadableDatabase());
                    if (queued.size() > 0 && Utils.isInternetActive(MessagesService.this)) {
                        // try to resend all queued recordings..
                        for (Message message : queued) {
                            if(!mSenderManager.isSending(message)) {
                                Log.d(TAG, "Re-trying queued request " + message.getId() + " to " + message.getChatId());
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

    private synchronized void doGcmRegistration() {
        // register to GCM, and always try until it goes alright
        String gcmToken = null;
        try {
            mAuthenticatorUtils.refreshAccount();
            gcmToken = mAuthenticatorUtils.getAccountData().getGcmRegistration();
        } catch (PeppermintApiNoAccountException e) {
            mTrackerManager.log("No account when getting GCM registration token...", e);
        }

        mTrackerManager.log("GCM Reg. = " + gcmToken);

        if (gcmToken == null) {
            Intent gcmIntent = new Intent(this, RegistrationIntentService.class);
            startService(gcmIntent);
        }
    }

    private Object mNotificationEventReceiver = new Object() {
        @SuppressWarnings("unused") // used through reflection
        public void onEventMainThread(ReceiverEvent event) {
            if(!event.doNotShowNotification()) {
                showNotification(event.getMessage());
            }
        }
    };

    public MessagesService() {
        // executor for the maintenance routine
        this.mScheduledExecutor = new ScheduledThreadPoolExecutor(1);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mTrackerManager = TrackerManager.getInstance(getApplicationContext());
        mAuthenticatorUtils = new AuthenticatorUtils(this);

        PeppermintEventBus.registerMessages(this, Integer.MAX_VALUE);
        PeppermintEventBus.registerMessages(mNotificationEventReceiver, Integer.MIN_VALUE);

        mPreferences = new SenderPreferences(this);

        mSenderManager = new SenderManager(this, new HashMap<String, Object>());
        mSenderManager.init();

        mHandler.postDelayed(mNotificationRunnable, 180000); // after 3 mins.

        registerReceiver(mConnectivityChangeReceiver, mConnectivityChangeFilter);
        rescheduleMaintenance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        doGcmRegistration();

        if(intent != null) {
            if(intent.hasExtra(PARAM_MESSAGE_SEND_RECORDING) && intent.hasExtra(PARAM_MESSAGE_SEND_CHAT)) {
                Recording recording = (Recording) intent.getSerializableExtra(PARAM_MESSAGE_SEND_RECORDING);
                Chat chat = (Chat) intent.getSerializableExtra(PARAM_MESSAGE_SEND_CHAT);
                send(chat, recording);
            } else if(intent.hasExtra(PARAM_MESSAGE_RECEIVE_DATA)) {
                Bundle receivedMessageData = intent.getBundleExtra(PARAM_MESSAGE_RECEIVE_DATA);
                Utils.logBundle(receivedMessageData, null);

                String messageId = receivedMessageData.getString("message_id");
                String audioUrl = receivedMessageData.getString("audio_url");
                String transcription = receivedMessageData.getString("transcription");
                String receiverEmail = receivedMessageData.getString("recipient_email");
                String senderEmail = receivedMessageData.getString("sender_email");
                String senderName = receivedMessageData.getString("sender_name");
                String createdTs = receivedMessageData.getString("created");
                int durationSeconds = Integer.parseInt(receivedMessageData.getString("duration", "0"));

                String emailAddress = null;
                try {
                    emailAddress = mAuthenticatorUtils.getAccountData().getEmail();
                } catch (PeppermintApiNoAccountException e) {
                    mTrackerManager.log("No account when receiving message...", e);
                }

                Message message = handleReceivedMessage(emailAddress, messageId, audioUrl, transcription, receiverEmail, senderEmail, senderName, createdTs, durationSeconds);

                // notify
                if(message != null) {
                    refreshBadge();
                    PeppermintEventBus.postReceiverEvent(receiverEmail, message);
                }
            } else if(intent.hasExtra(PARAM_DO_SYNC) && intent.getBooleanExtra(PARAM_DO_SYNC, false)) {
                runMessagesSync();
            } else if(intent.getAction() != null) {
                if(intent.getAction().compareTo(ACTION_CANCEL) == 0) {
                    cancel(null);
                } else if(intent.getAction().compareTo(ACTION_DO_PENDING_LOGOUTS) == 0) {
                    doPendingLogouts();
                }
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
        PeppermintEventBus.unregisterMessages(this);
        PeppermintEventBus.unregisterMessages(mNotificationEventReceiver);

        mSenderManager.deinit();

        super.onDestroy();
    }

    private void doPendingLogouts() {
        if(mPendingLogoutPeppermintTask == null || mPendingLogoutPeppermintTask.getStatus() == AsyncTask.Status.FINISHED) {
            mPendingLogoutPeppermintTask = new PendingLogoutPeppermintTask(this);
            mPendingLogoutPeppermintTask.execute((Void) null);
        }
    }

    public void onEventMainThread(SenderEvent event) {
        final Message message = event.getSenderTask().getMessage();
        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();

        switch (event.getType()) {
            case SenderEvent.EVENT_ERROR:
            case SenderEvent.EVENT_QUEUED:
                if (event.getError() != null) {
                    if(event.getError() instanceof NoInternetConnectionException) {
                        Toast.makeText(this, R.string.sender_msg_no_internet, Toast.LENGTH_LONG).show();
                    } else if(event.getError() instanceof SSLException) {
                        Toast.makeText(this, R.string.sender_msg_secure_connection, Toast.LENGTH_LONG).show();
                    } else if(event.getError() instanceof GoogleApiNoAuthorizationException) {
                        Toast.makeText(this, R.string.sender_msg_unable_to_send_permissions, Toast.LENGTH_LONG).show();
                    } else if(event.getError() instanceof NoPlayServicesException) {
                        Toast.makeText(this, R.string.sender_msg_no_gplay, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, String.format(getString(R.string.sender_msg_send_error), event.getSenderTask().getMessage().getChatParameter().getTitle()), Toast.LENGTH_LONG).show();
                    }
                }
                break;

            case SenderEvent.EVENT_CANCELLED:
                try {
                    GlobalManager.deleteMessageAndRecording(this, message);
                } catch (SQLException e) {
                    mTrackerManager.logException(e);
                }
                break;

            case SenderEvent.EVENT_FINISHED:
                // message sending has finished, so update the saved data and mark as sent
                message.setSent(true);
                // notifications
                mPreferences.setHasSentMessage(true);
                mHandler.removeCallbacks(mNotificationRunnable);
                dismissFirstAudioMessageNotification();
                break;
        }

        if(event.getType() != SenderEvent.EVENT_CANCELLED &&
                event.getType() != SenderEvent.EVENT_PROGRESS) {
            databaseHelper.lock();
            try {
                MessageManager.update(db, message);
            } catch (SQLException e) {
                mTrackerManager.logException(e);
            }
            databaseHelper.unlock();
        }
    }

    private void markAsPlayed(Message message) {
        if(!message.isPlayed()) {
            try {
                GlobalManager.markAsPlayed(this, message);
                refreshBadge();
            } catch (SQLException e) {
                mTrackerManager.logException(e);
            }
        }
        removeNotification(message);
    }

    private void refreshBadge() {
        final int badgeCount = MessageManager.getUnopenedCount(DatabaseHelper.getInstance(this).getReadableDatabase());
        ShortcutBadger.applyCount(this, badgeCount);
    }

    private void removeAllNotifications() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    private boolean retry(Message message) {
        if(isSending(message)) {
            return false;
        }

        message = MessageManager.getMessageByIdOrServerId(DatabaseHelper.getInstance(this).getReadableDatabase(),
                message.getId(), message.getServerId());

        mSenderManager.send(message);
        return true;
    }

    private Message send(final Chat chat, final Recording recording) {
        Message message = null;

        try {
            message = GlobalManager.insertNotSentMessage(this, chat, recording);
        } catch (SQLException e) {
            mTrackerManager.logException(e);
        }

        if(message != null) {
            mSenderManager.send(message);
        }

        return message;
    }

    private boolean cancel(final Message message) {
        if(message != null) {
            if(!isSending(message)) {
                try {
                    GlobalManager.deleteMessageAndRecording(this, message);
                } catch (SQLException e) {
                    mTrackerManager.logException(e);
                }
            }
            return mSenderManager.cancel(message);
        }
        return mSenderManager.cancel();
    }

    private boolean isSending(final Message message) {
        if(message != null) {
            return mSenderManager.isSending(message);
        }
        return mSenderManager.isSending();
    }

    private boolean isSendingAndCancellable(final Message message) {
        if(message != null) {
            return mSenderManager.isSendingAndCancellable(message);
        }
        return false;
    }

    // UPDATE NOTIFICATION

    private Notification getNotification(final Message message) {
        Intent notificationIntent = new Intent(MessagesService.this, ChatActivity.class);
        notificationIntent.putExtra(ChatActivity.PARAM_AUTO_PLAY_MESSAGE_ID, message.getId());
        notificationIntent.putExtra(ChatActivity.PARAM_CHAT_ID, message.getChatId());
        PendingIntent pendingIntent = PendingIntent.getActivity(MessagesService.this, (int) message.getId(), notificationIntent, 0);

        String title = message.getChatParameter().getRecipientListDisplayNames();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(MessagesService.this)
                .setSmallIcon(R.drawable.ic_mail_36dp)
                .setContentTitle(getString(R.string.new_message))
                .setContentText(title + " " + getString(R.string.sent_you_a_message))
                .setContentIntent(pendingIntent)
                .setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.waterdrop))
                .setGroup(String.valueOf(message.getChatId()));

        if(message.getChatParameter().getRecipientList().get(0).getPhotoUri() != null) {
            try {
                builder.setLargeIcon(MediaStore.Images.Media.getBitmap(getContentResolver(),
                        Uri.parse(message.getChatParameter().getRecipientList().get(0).getPhotoUri())));
            } catch (IOException e) {
                mTrackerManager.log("Unable to use photo URI as notification large icon!", e);
            }
        }

        return builder.build();
    }

    private void removeNotification(final Message message) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(String.valueOf(message.getChatId()), (int) message.getChatId());
    }

    private void showNotification(final Message message) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(String.valueOf(message.getChatId()), (int) message.getChatId(), getNotification(message));
    }

    // FIRST INSTALL/USE NOTIFICATION

    private boolean showFirstAudioMessageNotification() {
        if(!mPreferences.hasSentMessage()) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            Intent notificationIntent = new Intent(MessagesService.this, StartupActivity.class);
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
