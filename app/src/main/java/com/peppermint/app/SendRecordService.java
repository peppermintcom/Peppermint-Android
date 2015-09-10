package com.peppermint.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.peppermint.app.data.Recipient;
import com.peppermint.app.senders.GmailSender;
import com.peppermint.app.senders.IntentMailSender;
import com.peppermint.app.senders.Sender;
import com.peppermint.app.utils.PepperMintPreferences;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;

public class SendRecordService extends Service {

    private static final String TAG = SendRecordService.class.getSimpleName();

    public static final String INTENT_DATA_FILEPATH = "SendRecordService_FilePath";
    public static final String INTENT_DATA_TO = "SendRecordService_To";

    private static final String RECORD_CONTENT_TYPE = "audio/mpeg";

    protected SendRecordServiceBinder mBinder = new SendRecordServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class SendRecordServiceBinder extends Binder {

        /**
         * Register an event listener to receive record events.
         * @param listener the event listener
         */
        void register(Object listener) {
            mEventBus.register(listener);
        }

        /**
         * Unregister the specified event listener to stop receiving record events.
         * @param listener the event listener
         */
        void unregister(Object listener) {
            mEventBus.unregister(listener);
        }

        /**
         * Start a recording.
         */
        UUID send(Recipient to, String filePath) {
            return SendRecordService.this.send(to, filePath);
        }

        /**
         * Stop the recording with the specified UUID.
         */
        void cancel(UUID sendTaskUuid) {
            SendRecordService.this.cancel(sendTaskUuid);
        }

        void shutdown() {
            stopSelf();
        }
    }

    private PepperMintPreferences mPreferences;

    private EventBus mEventBus;
    private Map<String, Sender> mSenderMap; // mime type x send handler
    private Map<UUID, Sender.SenderTask> mTaskMap;

    private boolean mIsInForegroundMode = false;

    public SendRecordService() {
        mEventBus = new EventBus();
        mTaskMap = new HashMap<>();
        mSenderMap = new HashMap<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = new PepperMintPreferences(this);
        mEventBus.register(this);

        GmailSender gmailSender = new GmailSender(this, mEventBus);
        gmailSender.init();
        IntentMailSender intentMailSender = new IntentMailSender(this, mEventBus);
        intentMailSender.init();
        gmailSender.setFailureChainSender(intentMailSender);

        mSenderMap.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, gmailSender);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);

        if(intent != null && intent.hasExtra(INTENT_DATA_FILEPATH) && intent.hasExtra(INTENT_DATA_TO)) {
            String filePath = intent.getStringExtra(INTENT_DATA_FILEPATH);
            Recipient recipient = (Recipient) intent.getSerializableExtra(INTENT_DATA_TO);

            if(filePath != null && recipient != null) {
                send(recipient, filePath);
            }
        }

        return START_REDELIVER_INTENT;
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
        cancel(null);
        for(Map.Entry<String, Sender> entry : mSenderMap.entrySet()) {
            Sender sender = entry.getValue();
            while(sender != null) {
                sender.deinit();
                sender = sender.getFailureChainSender();
            }
        }
        mEventBus.unregister(this);
        super.onDestroy();
    }

    public void onEventMainThread(Sender.SenderEvent event) {
        switch (event.getType()) {
            case Sender.SenderEvent.EVENT_STARTED:
                mTaskMap.put(event.getTask().getUUID(), event.getTask());
                if (!mIsInForegroundMode) {
                    startForeground(SendRecordService.class.hashCode(), getNotification());
                    mIsInForegroundMode = true;
                } else {
                    updateNotification();
                }
                break;
            case Sender.SenderEvent.EVENT_ERROR:
            case Sender.SenderEvent.EVENT_CANCELLED:
            case Sender.SenderEvent.EVENT_FINISHED:
                mTaskMap.remove(event.getTask().getUUID());
                if (mTaskMap.size() <= 0) {
                    stopForeground(true);
                    mIsInForegroundMode = false;
                } else {
                    updateNotification();
                }
                break;
        }
    }

    private UUID send(Recipient to, String filePath) {
        if(!mSenderMap.containsKey(to.getMimeType())) {
            throw new NullPointerException("Sender for mime type " + to.getMimeType() + " not found!");
        }
        Sender sender = mSenderMap.get(to.getMimeType());
        Sender.SenderTask task = sender.sendAsync(to.getVia(), mPreferences.getMailSubject(), mPreferences.getMailBody(), filePath, RECORD_CONTENT_TYPE);;
        return task.getUUID();
    }

    private void cancel(UUID sendTaskUuid) {
        if(sendTaskUuid != null) {
            if(mTaskMap.containsKey(sendTaskUuid)) {
                Sender.SenderTask task = mTaskMap.get(sendTaskUuid);
                task.cancel(true);
                mTaskMap.remove(sendTaskUuid);
            }
        } else {
            Iterator<Map.Entry<UUID, Sender.SenderTask>> it = mTaskMap.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<UUID, Sender.SenderTask> entry = it.next();
                entry.getValue().cancel(true);
                mTaskMap.remove(entry.getKey());
            }
        }

        if(mTaskMap.size() <= 0) {
            stopForeground(true);
            mIsInForegroundMode = false;
        } else {
            updateNotification();
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(SendRecordService.this, RecordActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(SendRecordService.this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(SendRecordService.this)
                .setSmallIcon(R.drawable.ic_email_black_36dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.sending))
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(SendRecordService.class.hashCode(), getNotification());
    }
}
