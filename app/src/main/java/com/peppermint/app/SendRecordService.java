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
import android.widget.Toast;

import com.peppermint.app.data.Recipient;
import com.peppermint.app.senders.GmailSender;
import com.peppermint.app.senders.IntentMailSender;
import com.peppermint.app.senders.Sender;
import com.peppermint.app.ui.recording.RecordingActivity;
import com.peppermint.app.utils.PepperMintPreferences;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

/**
 * Service that allows the background_gradient sending of files through different methods.
 */
public class SendRecordService extends Service {

    private static final String TAG = SendRecordService.class.getSimpleName();

    /**
         Intent extra key for a string containing the path of the file to send.
     **/
    public static final String INTENT_DATA_FILEPATH = "SendRecordService_FilePath";

    /**
        Intent extra key for the {@link Recipient} of the file.
     **/
    public static final String INTENT_DATA_RECIPIENT = "SendRecordService_Recipient";

    private static final String RECORD_CONTENT_TYPE = "audio/mpeg";

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
         * @param filePath the location of the file
         * @return the {@link UUID} of the send file request/task
         */
        UUID send(Recipient recipient, String filePath) {
            return SendRecordService.this.send(recipient, filePath);
        }

        /**
         * Cancel the send request with the specified {@link UUID}.
         * <b>If the send request is ongoing, it might get sent anyway.</b>
         * @param sendTaskUuid the UUID of the send request returned by {@link #send(Recipient, String)}
         */
        void cancel(UUID sendTaskUuid) {
            SendRecordService.this.cancel(sendTaskUuid);
        }

        /**
         * Cancel all pending and ongoing send requests.
         * <b>If a send request is ongoing, it might get sent anyway.</b>
         */
        void shutdown() {
            stopSelf();
        }
    }

    // setup a private thread pool to avoid hanging up other AsyncTasks
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private ThreadPoolExecutor mExecutor;

    private PepperMintPreferences mPreferences;

    private EventBus mEventBus;
    private Map<String, Sender> mSenderMap;             // <mime type, send handler>
    private Map<UUID, Sender.SenderTask> mTaskMap;      // pending + ongoing send requests

    private boolean mIsInForegroundMode = false;

    public SendRecordService() {
        mEventBus = new EventBus();
        mTaskMap = new HashMap<>();
        mSenderMap = new HashMap<>();

        // private thread pool to avoid hanging up other AsyncTasks
        mExecutor = new ThreadPoolExecutor(CPU_COUNT + 1, CPU_COUNT * 2 + 1,
                60, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mPreferences = new PepperMintPreferences(this);
        mEventBus.register(this);

        // init all available senders
        // gmail api + email intent sender chain
        GmailSender gmailSender = new GmailSender(this, mEventBus, mExecutor);
        gmailSender.getParameters().put(GmailSender.PARAM_DISPLAY_NAME, mPreferences.getDisplayName());
        gmailSender.init(null);
        IntentMailSender intentMailSender = new IntentMailSender(this, mEventBus, mExecutor);
        intentMailSender.init(null);
        gmailSender.setFailureChainSender(intentMailSender);

        mSenderMap.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, gmailSender);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);

        if(intent != null && intent.hasExtra(INTENT_DATA_FILEPATH) && intent.hasExtra(INTENT_DATA_RECIPIENT)) {
            String filePath = intent.getStringExtra(INTENT_DATA_FILEPATH);
            Recipient recipient = (Recipient) intent.getSerializableExtra(INTENT_DATA_RECIPIENT);

            if(filePath != null && recipient != null) {
                send(recipient, filePath);
            }
        }

        return START_NOT_STICKY;
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
                Toast.makeText(this, String.format(getString(R.string.msg_message_send_error), event.getTask().getToName()), Toast.LENGTH_SHORT).show();
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

    private UUID send(Recipient recipient, String filePath) {
        if(!mSenderMap.containsKey(recipient.getMimeType())) {
            throw new NullPointerException("Sender for mime type " + recipient.getMimeType() + " not found!");
        }
        Sender sender = mSenderMap.get(recipient.getMimeType());
        // FIXME use proper URLs and remove the dummy ones
        Sender.SenderTask task = sender.sendAsync(recipient.getName(), recipient.getVia(), mPreferences.getMailSubject(),
                "<p>" + String.format(getString(R.string.default_mail_body_listen), "http://peppermint.com/msg?id=1234-1234-1234-DUMMY") +
                        "</p><br />" + String.format(getString(R.string.default_mail_body_reply), "123456767"),
                filePath, RECORD_CONTENT_TYPE);
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
        Intent notificationIntent = new Intent(SendRecordService.this, RecordingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(SendRecordService.this, 0, notificationIntent, 0);

        // FIXME use proper icons for these notifications
        // TODO add cancel action to notification perhaps?
        // TODO add progress percentage to the notification whenever possible (depends on sender)
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
