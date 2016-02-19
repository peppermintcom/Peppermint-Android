package com.peppermint.app.cloud;

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
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.MainActivity;
import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticatorUtils;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiNoAccountException;
import com.peppermint.app.cloud.gcm.RegistrationIntentService;
import com.peppermint.app.cloud.senders.SenderEvent;
import com.peppermint.app.cloud.senders.SenderManager;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientManager;
import com.peppermint.app.data.Recording;
import com.peppermint.app.data.RecordingManager;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.ui.chat.ChatFragment;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

/**
 * Service handles sending and receiving {@link Message}s.
 */
public class MessagesService extends Service {

    private static final String TAG = MessagesService.class.getSimpleName();

    // intent parameters to send a message
    public static final String PARAM_MESSAGE_SEND_RECORDING = TAG + "_paramMessageSendRecording";
    public static final String PARAM_MESSAGE_SEND_RECIPIENT = TAG + "_paramMessageSendRecipient";

    // intent parameters to receive a message
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
        Message send(Recipient recipient, Recording recording) {
            return MessagesService.this.send(recipient, recording);
        }

        boolean retry(Message message) {
            return MessagesService.this.retry(message);
        }

        /**
         * Cancel the message with the specified {@link UUID}.
         * <b>If the message is being sent, it might get sent anyway.</b>
         * @param message the UUID of the message returned by {@link #send(Recipient, Recording)}
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

        void markAsPlayed(Message message) {
            MessagesService.this.markAsPlayed(message);
        }

        void removeAllNotifications() {
            MessagesService.this.removeAllNotifications();
        }
    }

    private TrackerManager mTrackerManager;
    private SenderPreferences mPreferences;
    private EventBus mEventBus;
    private SenderManager mSenderManager;
    private DatabaseHelper mDatabaseHelper;
    private AuthenticatorUtils mAuthenticatorUtils;

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
                    doGcmRegistration();

                    SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
                    List<Message> queued = MessageManager.getMessagesQueued(MessagesService.this, db);
                    db.close();
                    if (queued.size() > 0 && Utils.isInternetActive(MessagesService.this)) {
                        // try to resend all queued recordings..
                        for (Message message : queued) {
                            if(!mSenderManager.isSending(message)) {
                                Log.d(TAG, "Re-trying queued request " + message.getId() + " to " + message.getRecipientParameter().getDisplayName());
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
        public void onEventMainThread(ReceiverEvent event) {
            if(!event.doNotShowNotification()) {
                showNotification(event.getMessage());
            }
        }
    };

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
        mAuthenticatorUtils = new AuthenticatorUtils(this);

        mEventBus.register(this, Integer.MAX_VALUE);
        mEventBus.register(mNotificationEventReceiver, Integer.MIN_VALUE);

        mPreferences = new SenderPreferences(this);

        mSenderManager = new SenderManager(this, mEventBus, new HashMap<String, Object>());
        mSenderManager.init();

        mHandler.postDelayed(mNotificationRunnable, 180000); // after 3mins.

        registerReceiver(mConnectivityChangeReceiver, mConnectivityChangeFilter);
        rescheduleMaintenance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        doGcmRegistration();

        if(intent != null) {
            if(intent.hasExtra(PARAM_MESSAGE_SEND_RECORDING) && intent.hasExtra(PARAM_MESSAGE_SEND_RECIPIENT)) {
                Recording recording = (Recording) intent.getSerializableExtra(PARAM_MESSAGE_SEND_RECORDING);
                Recipient recipient = (Recipient) intent.getSerializableExtra(PARAM_MESSAGE_SEND_RECIPIENT);
                send(recipient, recording);
            } else if(intent.hasExtra(PARAM_MESSAGE_RECEIVE_DATA)) {
                Bundle receivedMessageData = intent.getBundleExtra(PARAM_MESSAGE_RECEIVE_DATA);

                /*Utils.logBundle(receivedMessageData, null);*/

                String audioUrl = receivedMessageData.getString("audio_url");
                String transcriptionUrl = receivedMessageData.getString("transcription_url");
                String receiverEmail = receivedMessageData.getString("recipient_email");
                String senderEmail = receivedMessageData.getString("sender_email");
                String senderName = receivedMessageData.getString("sender_name");
                String createdTs = receivedMessageData.getString("created");
                int durationSeconds = Integer.parseInt(receivedMessageData.getString("duration", "0"));

                if(audioUrl == null || senderEmail == null || receiverEmail == null) {
                    mTrackerManager.log("Invalid GCM notification received! Either or both audio URL and sender email are null.");
                } else {
                    String emailAddress = null;
                    try {
                        emailAddress = mAuthenticatorUtils.getAccountData().getEmail();
                    } catch (PeppermintApiNoAccountException e) {
                        mTrackerManager.log("No account when receiving message...", e);
                    }

                    Recipient recipient = null;
                    if(emailAddress != null && emailAddress.compareToIgnoreCase(receiverEmail.trim()) == 0) {
                        String[] names = Utils.getFirstAndLastNames(senderName);
                        try {
                            recipient = RecipientManager.insertOrUpdate(this, 0, names[0], names[1], null, senderEmail, null, emailAddress, true);
                        } catch (RecipientManager.InvalidViaException | RecipientManager.InvalidNameException e) {
                            mTrackerManager.log(senderName + " - " + senderEmail);
                            mTrackerManager.logException(e);
                        }
                    } else if(emailAddress != null) {
                        mTrackerManager.log("Received wrong message from GCM! Should have gone to email " + receiverEmail);
                    }

                    if(recipient != null) {
                        Recording recording = null;
                        Chat chat = null;
                        Message message = null;

                        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                        try {
                            // insert recording
                            recording = RecordingManager.insertOrUpdate(db, 0, null, durationSeconds * 1000l, 0, false, createdTs, Recording.CONTENT_TYPE_AUDIO);

                            // insert chat
                            Chat foundChat = ChatManager.getChatByMainRecipient(null, db, recipient.getContactId());
                            if(foundChat != null) {
                                String chatTimestamp = createdTs;
                                if(foundChat.getLastMessageTimestamp() != null && foundChat.getLastMessageTimestamp().compareToIgnoreCase(createdTs) > 0) {
                                    chatTimestamp = foundChat.getLastMessageTimestamp();
                                }
                                chat = ChatManager.update(db, foundChat.getId(), recipient.getContactId(), chatTimestamp);
                            } else {
                                chat = ChatManager.insert(db, recipient.getContactId(), createdTs);
                            }

                            // insert message
                            message = MessageManager.insert(db, chat.getId(), recipient.getContactId(), recording.getId(),
                                null, null, audioUrl, transcriptionUrl, null, null, createdTs, false, true, false);
                            message.setRecipientParameter(recipient);
                            message.setRecordingParameter(recording);
                        } catch (SQLException e) {
                            mTrackerManager.logException(e);
                        }
                        db.close();

                        if(message != null) {
                            mPreferences.addRecentContactUri(recipient.getContactId());

                            ReceiverEvent ev = new ReceiverEvent();
                            ev.setReceiverEmail(receiverEmail);
                            ev.setMessage(message);
                            mEventBus.post(ev);
                        }
                    }
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
        mEventBus.unregister(this);

        mSenderManager.deinit();

        super.onDestroy();
    }

    public void onEventMainThread(SenderEvent event) {
        Message message = event.getSenderTask().getMessage();
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        switch (event.getType()) {
            case SenderEvent.EVENT_ERROR:
                Toast.makeText(this, String.format(getString(R.string.sender_msg_send_error), event.getSenderTask().getMessage().getRecipientParameter().getDisplayName()), Toast.LENGTH_SHORT).show();
            case SenderEvent.EVENT_QUEUED:
                if (event.getError().getMessage() != null) {
                    Toast.makeText(this, event.getError().getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case SenderEvent.EVENT_CANCELLED:
                try {
                    // TODO actually delete the local file
                    // discard recording as well
                    RecordingManager.delete(db, message.getRecordingId());
                    MessageManager.delete(db, message.getId());
                } catch (SQLException e) {
                    mTrackerManager.logException(e);
                }
                break;
            case SenderEvent.EVENT_FINISHED:
                // message sending has finished, so update the saved data and mark as sent
                try {
                    message.setSent(true);
                    MessageManager.insertOrUpdate(db, message.getId(), message.getChatId(), message.getRecipientContactId(), message.getRecordingId(),
                    message.getServerId(), message.getServerShortUrl(), message.getServerCanonicalUrl(), message.getServerTranscriptionUrl(),
                            message.getEmailSubject(), message.getEmailBody(),
                            message.getRegistrationTimestamp(), message.isSent(), message.isReceived(), message.isPlayed());
                } catch (SQLException e) {
                    mTrackerManager.logException(e);
                }

                // notifications
                mPreferences.setHasSentMessage(true);
                mHandler.removeCallbacks(mNotificationRunnable);
                dismissFirstAudioMessageNotification();
                break;
        }
        db.close();
    }

    private void markAsPlayed(Message message) {
        if(!message.isPlayed()) {
            SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
            message.setPlayed(true);
            try {
                MessageManager.insertOrUpdate(db, message.getId(), message.getChatId(), message.getRecipientContactId(), message.getRecordingId(),
                        message.getServerId(), message.getServerShortUrl(), message.getServerCanonicalUrl(), message.getServerTranscriptionUrl(),
                        message.getEmailSubject(), message.getEmailBody(),
                        message.getRegistrationTimestamp(), message.isSent(), message.isReceived(), message.isPlayed());
            } catch (SQLException e) {
                mTrackerManager.logException(e);
            }
            db.close();
        }

        removeNotification(message);
    }

    private void removeAllNotifications() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    private boolean retry(Message message) {
        if(isSending(message)) {
            return false;
        }

        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        message = MessageManager.getMessageByIdOrServerId(this, db, message.getId(), message.getServerId());
        db.close();

        mSenderManager.send(message);
        return true;
    }

    private Message send(Recipient recipient, Recording recording) {
        // each sender is currently responsible for building its own message body
        String body = getString(R.string.sender_default_message);
        Message message = null;

        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        try {
            // insert chat
            Chat chat = ChatManager.insertOrUpdate(db, 0, recipient.getContactId(), recording.getRecordedTimestamp());

            // insert recording
            Recording newRecording = RecordingManager.insertOrUpdate(db, recording.getId(), recording.getFilePath(), recording.getDurationMillis(), recording.getSizeKb(),
                    recording.hasVideo(), recording.getRecordedTimestamp(), recording.getContentType());
            recording.setId(newRecording.getId());

            // insert message
            message = MessageManager.insertOrUpdate(db, 0, chat.getId(), recipient.getContactId(), recording.getId(), null, null, null, null,
                    getString(R.string.sender_default_mail_subject), body, DateContainer.getCurrentUTCTimestamp(), false, false, false);
            message.setRecipientParameter(recipient);
            message.setRecordingParameter(recording);
        } catch (SQLException e) {
            mTrackerManager.logException(e);
        }
        db.close();

        if(message != null) {
            mPreferences.addRecentContactUri(recipient.getContactId());
            mSenderManager.send(message);
        }

        return message;
    }

    private boolean cancel(Message message) {
        if(message != null) {
            if(!isSending(message)) {
                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                try {
                    MessageManager.delete(db, message.getId());
                } catch (SQLException e) {
                    mTrackerManager.logException(e);
                }
                db.close();
            }
            return mSenderManager.cancel(message);
        }
        return mSenderManager.cancel();
    }

    private boolean isSending(Message message) {
        if(message != null) {
            return mSenderManager.isSending(message);
        }
        return mSenderManager.isSending();
    }

    // UPDATE NOTIFICATION

    private Notification getNotification(Message message) {
        Intent notificationIntent = new Intent(MessagesService.this, ChatActivity.class);
        notificationIntent.putExtra(ChatFragment.PARAM_AUTO_PLAY_MESSAGE_ID, message.getId());
        notificationIntent.putExtra(ChatFragment.PARAM_CHAT_ID, message.getChatId());
        PendingIntent pendingIntent = PendingIntent.getActivity(MessagesService.this, (int) message.getId(), notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(MessagesService.this)
                .setSmallIcon(R.drawable.ic_mail_36dp)
                .setContentTitle(getString(R.string.new_message))
                .setContentText(message.getRecipientParameter().getDisplayName() + " " + getString(R.string.sent_you_a_message))
                .setContentIntent(pendingIntent);

        if(message.getRecipientParameter().getPhotoUri() != null) {
            try {
                builder.setLargeIcon(MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(message.getRecipientParameter().getPhotoUri())));
            } catch (IOException e) {
                mTrackerManager.log("Unable to use photo URI as notification large icon!", e);
            }
        }

        return builder.build();
    }

    private void removeNotification(Message message) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(message.getServerId(), (int) message.getId());
    }

    private void showNotification(Message message) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(message.getServerId(), (int) message.getId(), getNotification(message));
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
