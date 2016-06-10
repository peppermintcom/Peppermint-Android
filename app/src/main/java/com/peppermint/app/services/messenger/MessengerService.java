package com.peppermint.app.services.messenger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.peppermint.app.BuildConfig;
import com.peppermint.app.R;
import com.peppermint.app.StartupActivity;
import com.peppermint.app.cloud.apis.google.GoogleApiNoAuthorizationException;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiNoAccountException;
import com.peppermint.app.dal.DataObjectEvent;
import com.peppermint.app.dal.DataObjectManager;
import com.peppermint.app.dal.DatabaseHelper;
import com.peppermint.app.dal.GlobalManager;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.contact.ContactManager;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.message.MessageManager;
import com.peppermint.app.dal.recording.Recording;
import com.peppermint.app.services.authenticator.AuthenticationService;
import com.peppermint.app.services.authenticator.AuthenticatorUtils;
import com.peppermint.app.services.authenticator.SignOutEvent;
import com.peppermint.app.services.gcm.RegistrationIntentService;
import com.peppermint.app.services.messenger.handlers.NoInternetConnectionException;
import com.peppermint.app.services.messenger.handlers.NoPlayServicesException;
import com.peppermint.app.services.messenger.handlers.SenderManager;
import com.peppermint.app.services.messenger.handlers.SenderManagerListener;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.services.messenger.handlers.SenderSupportTask;
import com.peppermint.app.services.messenger.handlers.SenderTask;
import com.peppermint.app.services.messenger.handlers.SenderUploadTask;
import com.peppermint.app.services.sync.SyncEvent;
import com.peppermint.app.services.sync.SyncService;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.utils.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import de.greenrobot.event.EventBus;
import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Service handles sending and receiving {@link Message}s.
 */
public class MessengerService extends Service {

    private static final String TAG = MessengerService.class.getSimpleName();

    public static final String ACTION_CANCEL = "com.peppermint.app.cloud.MessengerService.CANCEL";

    // intent parameters to send a message
    public static final String PARAM_MESSAGE_SEND_RECORDING = TAG + "_paramMessageSendRecording";
    public static final String PARAM_MESSAGE_SEND_CHAT = TAG + "_paramMessageSendChat";

    // intent parameters to receive a message
    public static final String PARAM_MESSAGE_RECEIVE_DATA = TAG + "_paramMessageReceiveData";

    private static final int FIRST_AUDIO_MESSAGE_NOTIFICATION_ID = 1;

    private static final EventBus EVENT_BUS = new EventBus();

    static {
        if(BuildConfig.DEBUG) {
            EVENT_BUS.register(new Object() {
                public void onEventBackgroundThread(MessengerSendEvent event) {
                    Log.d(TAG, event.toString());
                }
            });
        }
    }

    public static void registerEventListener(Object listener) {
        EVENT_BUS.register(listener);
    }

    public static void unregisterEventListener(Object listener) {
        EVENT_BUS.unregister(listener);
    }

    private static void postSendEvent(int type, SenderTask senderTask, Throwable error) {
        if(EVENT_BUS.hasSubscriberForEvent(MessengerSendEvent.class)) {
            EVENT_BUS.post(new MessengerSendEvent(senderTask, type, error));
        }
    }

    // bridge directly to the event bus
    private SenderManagerListener mSenderManagerListener = new SenderManagerListener() {

        private void handleError(SenderUploadTask uploadTask, Throwable error) {
            if (error != null) {
                if(error instanceof NoInternetConnectionException) {
                    Toast.makeText(MessengerService.this, R.string.sender_msg_no_internet, Toast.LENGTH_LONG).show();
                } else if(error instanceof SSLException) {
                    Toast.makeText(MessengerService.this, R.string.sender_msg_secure_connection, Toast.LENGTH_LONG).show();
                } else if(error instanceof GoogleApiNoAuthorizationException) {
                    Toast.makeText(MessengerService.this, R.string.sender_msg_unable_to_send_permissions, Toast.LENGTH_LONG).show();
                } else if(error instanceof NoPlayServicesException) {
                    Toast.makeText(MessengerService.this, R.string.sender_msg_no_gplay, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MessengerService.this, String.format(getString(R.string.sender_msg_send_error), uploadTask.getMessage().getChatParameter().getTitle()), Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onSendingStarted(SenderUploadTask uploadTask) {
            DataObjectManager.update(MessageManager.getInstance(MessengerService.this), uploadTask.getMessage());
            postSendEvent(MessengerSendEvent.EVENT_STARTED, uploadTask, null);
        }

        @Override
        public void onSendingCancelled(SenderUploadTask uploadTask) {
            try {
                GlobalManager.getInstance(MessengerService.this).deleteMessageAndRecording(uploadTask.getMessage());
            } catch (SQLException e) {
                mTrackerManager.logException(e);
            }
            postSendEvent(MessengerSendEvent.EVENT_CANCELLED, uploadTask, null);
        }

        @Override
        public void onSendingFinished(SenderUploadTask uploadTask) {
            // message sending has finished, so update the saved data and mark as sent
            uploadTask.getMessage().setSent(true);
            mPreferences.setHasSentMessage(true);
            mHandler.removeCallbacks(mNotificationRunnable);
            dismissFirstAudioMessageNotification();
            DataObjectManager.update(MessageManager.getInstance(MessengerService.this), uploadTask.getMessage());

            postSendEvent(MessengerSendEvent.EVENT_FINISHED, uploadTask, null);
        }

        @Override
        public void onSendingError(SenderUploadTask uploadTask, Throwable error) {
            handleError(uploadTask, error);
            DataObjectManager.update(MessageManager.getInstance(MessengerService.this), uploadTask.getMessage());
            postSendEvent(MessengerSendEvent.EVENT_ERROR, uploadTask, error);
        }

        @Override
        public void onSendingProgress(SenderUploadTask uploadTask, float progressValue) {
            postSendEvent(MessengerSendEvent.EVENT_PROGRESS, uploadTask, null);
        }

        @Override
        public void onSendingQueued(SenderUploadTask uploadTask, Throwable error) {
            handleError(uploadTask, error);
            DataObjectManager.update(MessageManager.getInstance(MessengerService.this), uploadTask.getMessage());
            postSendEvent(MessengerSendEvent.EVENT_QUEUED, uploadTask, error);
        }

        @Override
        public void onSendingNonCancellable(SenderUploadTask uploadTask) {
            DataObjectManager.update(MessageManager.getInstance(MessengerService.this), uploadTask.getMessage());
            postSendEvent(MessengerSendEvent.EVENT_NON_CANCELLABLE, uploadTask, null);
        }
    };

    protected SendRecordServiceBinder mBinder = new SendRecordServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class SendRecordServiceBinder extends Binder {

        Message send(Chat chat, Recording recording) { return MessengerService.this.send(chat, recording); }
        boolean retry(Message message) {
            return MessengerService.this.retry(message);
        }

        boolean cancel(Message message) {
            return MessengerService.this.cancel(message);
        }
        boolean cancel() {
            return MessengerService.this.cancel(null);
        }

        boolean isSending() {
            return MessengerService.this.isSending(null);
        }
        boolean isSending(Message message) {
            return MessengerService.this.isSending(message);
        }
        boolean isSendingAndCancellable(Message message) { return MessengerService.this.isSendingAndCancellable(message); }

        void markAsPlayed(Message message) {
            MessengerService.this.markAsPlayed(message);
        }
    }

    private TrackerManager mTrackerManager;
    private SenderPreferences mPreferences;
    private SenderManager mSenderManager;
    private AuthenticatorUtils mAuthenticatorUtils;

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

        Message message = null;
        try {
            message = GlobalManager.getInstance(this).insertReceivedMessage(receiverEmail, senderName, senderEmail, audioUrl, serverId, transcription, createdTs, durationSeconds, null, false);
        } catch (ContactManager.InvalidViaException | ContactManager.InvalidNameException e) {
            mTrackerManager.log(senderName + " - " + senderEmail);
            mTrackerManager.logException(e);
        } catch (SQLException e) {
            mTrackerManager.logException(e);
        }

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
    private ScheduledExecutorService mScheduledExecutor;     // a thread pool that sends queued requests
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
                if (Utils.isInternetAvailable(MessengerService.this)) {
                    doGcmRegistration();

                    List<Message> queued = MessageManager.getInstance(MessengerService.this).getMessagesQueued(DatabaseHelper.getInstance(MessengerService.this).getReadableDatabase());
                    if (queued.size() > 0 && Utils.isInternetActive(MessengerService.this)) {
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
        public void onEventMainThread(DataObjectEvent<Message> event) {
            if(event.getType() == DataObjectEvent.TYPE_CREATE && event.getDataObject().isReceived() && !event.isSkipNotifications()) {
                showNotification(event.getDataObject());
            }
        }
    };

    private Object mSyncEventReceiver = new Object() {
        @SuppressWarnings("unused") // used through reflection
        public void onEventMainThread(SyncEvent event) {
            if(event.getType() == SyncEvent.EVENT_FINISHED) {
                refreshBadge();
            }
        }
    };

    public MessengerService() {
        // executor for the maintenance routine
        this.mScheduledExecutor = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mTrackerManager = TrackerManager.getInstance(getApplicationContext());
        mAuthenticatorUtils = new AuthenticatorUtils(this);

        MessageManager.getInstance(this).registerDataListener(mNotificationEventReceiver, Integer.MIN_VALUE);
        SyncService.registerEventListener(mSyncEventReceiver);

        mPreferences = new SenderPreferences(this);

        mSenderManager = new SenderManager(this, mSenderManagerListener, new HashMap<String, Object>());
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
                }
            } else if(intent.getAction() != null) {
                if(intent.getAction().compareTo(ACTION_CANCEL) == 0) {
                    cancel(null);
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
        MessageManager.getInstance(this).unregisterDataListener(mNotificationEventReceiver);
        SyncService.unregisterEventListener(mSyncEventReceiver);

        mSenderManager.deinit();

        super.onDestroy();
    }

    private void markAsPlayed(Message message) {
        if(!message.isPlayed()) {
            (new MarkAsPlayedTask(message)).execute((Void) null);
        }
        removeNotification(message);
    }

    private void refreshBadge() {
        final int badgeCount = MessageManager.getInstance(this).getUnopenedCount(DatabaseHelper.getInstance(this).getReadableDatabase());
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

        message = MessageManager.getInstance(this).getMessageByIdOrServerId(DatabaseHelper.getInstance(this).getReadableDatabase(),
                message.getId(), message.getServerId(), false);

        mSenderManager.send(message);
        return true;
    }

    private Message send(final Chat chat, final Recording recording) {
        Message message = null;

        try {
            message = GlobalManager.getInstance(this).insertNotSentMessage(chat, recording);
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
                    GlobalManager.getInstance(this).deleteMessageAndRecording(message);
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
        Intent notificationIntent = new Intent(MessengerService.this, ChatActivity.class);
        notificationIntent.putExtra(ChatActivity.PARAM_CHAT_ID, message.getChatId());
        PendingIntent pendingIntent = PendingIntent.getActivity(MessengerService.this, (int) message.getId(), notificationIntent, 0);

        String title = message.getChatParameter().getRecipientListDisplayNames();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(MessengerService.this)
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

            Intent notificationIntent = new Intent(MessengerService.this, StartupActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(MessengerService.this, 0, notificationIntent, 0);

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

    private class MarkAsPlayedTask extends SenderSupportTask {

        public MarkAsPlayedTask(Message message) {
            super(null, null, null);
            getIdentity().setContext(MessengerService.this);
            getIdentity().setTrackerManager(TrackerManager.getInstance(MessengerService.this));
            getIdentity().setPreferences(new SenderPreferences(MessengerService.this));
            setMessage(message);
        }

        @Override
        protected void execute() throws Throwable {
            Log.d(TAG, "Starting Mark as Played...");

            Message message = getMessage();
            setupPeppermintAuthentication();
            getPeppermintApi().markAsPlayedMessage(getId().toString(), message.getServerId());
        }

        public void onEventMainThread(SignOutEvent event) {
            cancel(true);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            AuthenticationService.registerEventListener(this);
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            AuthenticationService.unregisterEventListener(this);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            AuthenticationService.unregisterEventListener(this);
            final Message message = getMessage();
            if(getError() != null) {
                message.setPlayed(false);
                if(!(getError() instanceof NoInternetConnectionException)) {
                    getTrackerManager().logException(getError());
                }
            } else {
                message.setPlayed(true);
            }
            DataObjectManager.update(MessageManager.getInstance(MessengerService.this), message);
            refreshBadge();
        }
    }
}
