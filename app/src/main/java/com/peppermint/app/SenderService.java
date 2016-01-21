package com.peppermint.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.Recording;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.SenderEvent;
import com.peppermint.app.sending.SenderManager;
import com.peppermint.app.utils.PepperMintPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;

/**
 * Service that allows the background_gradient sending of files through different methods.
 */
public class SenderService extends Service {

    private static final String TAG = SenderService.class.getSimpleName();

    /**
         Intent extra key for the {@link Recording} containing the recording information and the file to send.
     **/
    public static final String INTENT_DATA_RECORDING = "SendRecordService_Recording";

    /**
        Intent extra key for the {@link Recipient} of the file.
     **/
    public static final String INTENT_DATA_RECIPIENT = "SendRecordService_Recipient";

    /**
     * Intent extra key for a boolean indicating if the Gmail API authorization process should be triggered.
     */
    public static final String INTENT_DATA_AUTHORIZE = "SendRecordService_Authorize";

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
        SendingRequest send(Recipient recipient, Recording recording) {
            return SenderService.this.send(recipient, recording);
        }

        /**
         * Cancel the send request with the specified {@link UUID}.
         * <b>If the send request is ongoing, it might get sent anyway.</b>
         * @param sendingRequestUuid the UUID of the send request returned by {@link #send(Recipient, Recording)}
         */
        boolean cancel(UUID sendingRequestUuid) {
            return SenderService.this.cancel(sendingRequestUuid);
        }
        boolean cancel() {
            return SenderService.this.cancel(null);
        }

        /**
         * Cancel all pending and ongoing send requests.
         * <b>If a send request is ongoing, it might get sent anyway.</b>
         */
        void shutdown() {
            stopSelf();
        }

        boolean isSending() {
            return SenderService.this.isSending(null);
        }
        boolean isSending(UUID sendingRequestUuid) {
            return SenderService.this.isSending(sendingRequestUuid);
        }

        void authorize() { SenderService.this.authorize(); }
    }

    private static final int FIRST_AUDIO_MESSAGE_NOTIFICATION_ID = 1;

    private PepperMintPreferences mPreferences;
    private EventBus mEventBus;
    private SenderManager mSenderManager;

    private boolean mIsInForegroundMode = false;
    private Handler mHandler = new Handler();
    private final Runnable mNotificationRunnable = new Runnable() {
        @Override
        public void run() {
            showFirstAudioMessageNotification();
        }
    };

    public SenderService() {
        mEventBus = new EventBus();
    }

    private boolean showFirstAudioMessageNotification() {
        if(!mPreferences.hasSentMessage()) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            Intent notificationIntent = new Intent(SenderService.this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(SenderService.this, 0, notificationIntent, 0);

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

    @Override
    public void onCreate() {
        super.onCreate();

        mEventBus.register(this);

        mPreferences = new PepperMintPreferences(this);

        Map<String, Object> senderParameters = new HashMap<>();
        // empty parameters
        mSenderManager = new SenderManager(this, mEventBus, senderParameters);
        mSenderManager.init();

        mHandler.postDelayed(mNotificationRunnable, 180000); // after 3mins.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);

        if(intent != null) {
            if(intent.hasExtra(INTENT_DATA_RECORDING) && intent.hasExtra(INTENT_DATA_RECIPIENT)) {
                Recording recording = (Recording) intent.getSerializableExtra(INTENT_DATA_RECORDING);
                Recipient recipient = (Recipient) intent.getSerializableExtra(INTENT_DATA_RECIPIENT);

                if (recording != null && recipient != null) {
                    send(recipient, recording);
                }
            } else if(intent.hasExtra(INTENT_DATA_AUTHORIZE) && intent.getBooleanExtra(INTENT_DATA_AUTHORIZE, false)) {
                authorize();
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
        mHandler.removeCallbacks(mNotificationRunnable);

        if(mIsInForegroundMode) {
            stopForeground(true);
            mIsInForegroundMode = false;
        }
        mSenderManager.deinit();
        mEventBus.unregister(this);
        super.onDestroy();
    }

    public void onEventMainThread(SenderEvent event) {
        switch (event.getType()) {
            case SenderEvent.EVENT_STARTED:
                if (!mIsInForegroundMode) {
                    startForeground(SenderService.class.hashCode(), getNotification());
                    mIsInForegroundMode = true;
                } else {
                    updateNotification();
                }
                break;
            case SenderEvent.EVENT_ERROR:
                Toast.makeText(this, String.format(getString(R.string.sender_msg_send_error), event.getSenderTask().getSendingRequest().getRecipient().getName()), Toast.LENGTH_SHORT).show();
            case SenderEvent.EVENT_QUEUED:
            case SenderEvent.EVENT_CANCELLED:
            case SenderEvent.EVENT_FINISHED:
                mPreferences.setHasSentMessage(true);
                mHandler.removeCallbacks(mNotificationRunnable);
                dismissFirstAudioMessageNotification();

                if (!mSenderManager.isSending()) {
                    stopForeground(true);
                    mIsInForegroundMode = false;
                } else {
                    updateNotification();
                }
                break;
        }
    }

    private void authorize() {
        mSenderManager.authorize();
    }

    private SendingRequest send(Recipient recipient, Recording recording) {
        // default body if no url is supplied (each sender is currently responsible for building its own message body)
        String body = getString(R.string.sender_default_message);
        SendingRequest sendingRequest = new SendingRequest(recording, recipient, mPreferences.getMailSubject(), body);

        mSenderManager.send(sendingRequest);

        return sendingRequest;
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

    private Notification getNotification() {
        Intent notificationIntent = new Intent(SenderService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(SenderService.this, 0, notificationIntent, 0);

        // TODO add cancel action to notification perhaps?
        // TODO add progress percentage to the notification whenever possible (depends on sender)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(SenderService.this)
                .setSmallIcon(R.drawable.ic_mail_36dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.uploading))
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(SenderService.class.hashCode(), getNotification());
    }
}
