package com.peppermint.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.Recording;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.SenderManager;
import com.peppermint.app.sending.SendingEvent;
import com.peppermint.app.sending.gmail.GmailSender;
import com.peppermint.app.ui.recording.RecordingActivity;
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
        void cancel(UUID sendingRequestUuid) {
            SenderService.this.cancel(sendingRequestUuid);
        }
        void cancel() {
            SenderService.this.cancel(null);
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
    }

    private PepperMintPreferences mPreferences;
    private EventBus mEventBus;
    private SenderManager mSenderManager;

    private boolean mIsInForegroundMode = false;

    public SenderService() {
        mEventBus = new EventBus();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mEventBus.register(this);

        mPreferences = new PepperMintPreferences(this);

        Map<String, Object> senderParameters = new HashMap<>();
        senderParameters.put(GmailSender.PARAM_DISPLAY_NAME, mPreferences.getDisplayName());
        mSenderManager = new SenderManager(this, mEventBus, senderParameters);
        mSenderManager.init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);

        if(intent != null && intent.hasExtra(INTENT_DATA_RECORDING) && intent.hasExtra(INTENT_DATA_RECIPIENT)) {
            Recording recording = (Recording) intent.getSerializableExtra(INTENT_DATA_RECORDING);
            Recipient recipient = (Recipient) intent.getSerializableExtra(INTENT_DATA_RECIPIENT);

            if(recording != null && recipient != null) {
                send(recipient, recording);
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
        mSenderManager.deinit();
        mEventBus.unregister(this);
        super.onDestroy();
    }

    public void onEventMainThread(SendingEvent event) {
        switch (event.getType()) {
            case SendingEvent.EVENT_STARTED:
                if (!mIsInForegroundMode) {
                    startForeground(SenderService.class.hashCode(), getNotification());
                    mIsInForegroundMode = true;
                } else {
                    updateNotification();
                }
                break;
            case SendingEvent.EVENT_ERROR:
                Toast.makeText(this, String.format(getString(R.string.msg_message_send_error), event.getSendingTask().getSendingRequest().getRecipient().getName()), Toast.LENGTH_SHORT).show();
            case SendingEvent.EVENT_QUEUED:
            case SendingEvent.EVENT_CANCELLED:
            case SendingEvent.EVENT_FINISHED:
                if (!mSenderManager.isSending()) {
                    stopForeground(true);
                    mIsInForegroundMode = false;
                } else {
                    updateNotification();
                }
                break;
        }
    }

    private SendingRequest send(Recipient recipient, Recording recording) {
        // FIXME use proper URLs and remove the dummy ones
        String body = "<p>" + String.format(getString(R.string.default_mail_body_listen), "http://peppermint.com/msg?id=1234-1234-1234-DUMMY") +
                "</p><br />" + getString(R.string.default_mail_body_reply);
        SendingRequest sendingRequest = new SendingRequest(recording, recipient, mPreferences.getMailSubject(), body);

        mSenderManager.send(sendingRequest);

        return sendingRequest;
    }

    private void cancel(UUID sendingRequestUuid) {
        if(sendingRequestUuid != null) {
            mSenderManager.cancel(sendingRequestUuid);
        } else {
            mSenderManager.cancel();
        }
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

        // FIXME use proper icons for these notifications
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
