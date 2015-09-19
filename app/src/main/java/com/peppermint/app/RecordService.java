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
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.peppermint.app.data.Recipient;
import com.peppermint.app.ui.recording.RecordFragment;
import com.peppermint.app.ui.recording.RecordingActivity;
import com.peppermint.app.utils.ExtendedMediaRecorder;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

/**
 * Service that allows the background_gradient recording audio/video files.
 */
public class RecordService extends Service {

    private static final String TAG = RecordService.class.getSimpleName();

    /**
        Intent extra key for a boolean flag indicating if the service should start
        recording right after starting.
     **/
    public static final String INTENT_DATA_DOSTART = "RecordService_DoStart";
    /**
        Intent extra key for a string with the filename prefix for the recorded file.
        This should be supplied if the DOSTART flag is true.
     **/
    public static final String INTENT_DATA_FILEPREFIX = "RecordService_FilePrefix";
    /**
        Intent extra key for the {@link Recipient} of the recorded file.
        This service doesn't handle the sending of files but the recipient is required to
        keep track of the sending process in case the interface (activity) gets closed.
        This <b>must</b> be supplied if the DOSTART flag is true.
     **/
    public static final String INTENT_DATA_RECIPIENT = "RecordService_Recipient";

    public static final int EVENT_START = 1;
    public static final int EVENT_RESUME = 2;
    public static final int EVENT_PAUSE = 3;
    public static final int EVENT_STOP = 4;
    public static final int EVENT_LOUDNESS = 5;
    public static final int EVENT_ERROR = 6;

    protected RecordServiceBinder mBinder = new RecordServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class RecordServiceBinder extends Binder {

        /**
         * Checks if the service is recording. Notice that the recording process can be paused.
         * It is still considered as an ongoing recording.
         * @return true if recording; false if not
         */
        boolean isRecording() {
            return RecordService.this.isRecording();
        }

        /**
         * Checks if the service is recording and paused.
         * @return true if recording and paused; false if not
         */
        boolean isPaused() {
            return RecordService.this.isPaused();
        }

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
         * Start a recording. You can only start a recording if no other recording is currently
         * active (even if it is paused).
         * @param filePrefix the filename prefix of the record
         * @param recipient the recipient of the record
         */
        void start(String filePrefix, Recipient recipient) {
            RecordService.this.start(filePrefix, recipient);
        }

        /**
         * Pause the current recording.
         */
        void pause() {
            RecordService.this.pause();
        }

        /**
         * Resume the current recording.
         */
        void resume() {
            RecordService.this.resume();
        }

        /**
         * Stop and finish the current recording.
         */
        void stop() {
            RecordService.this.stop();
        }

        /**
         * Discard the current recording and all files associated with it.
         * It also stops the current recording if it is ongoing.
         */
        void discard() {
            RecordService.this.discard();
        }

        /**
         * Shutdown the service, stopping and discarding the current recording.
         */
        void shutdown() {
            stopSelf();
        }
    }

    /**
     * Event associated with the recording process of the {@link RecordService}.
     */
    public static class Event {
        // intermediate process data
        private float mLoudness;
        private long mCurrentDuration;
        private String mCurrentFilePath;
        private ArrayList<String> mIntermediateFilePaths;

        // final relevant data
        private long mFullDuration;
        private String mFilePath;
        private Recipient mRecipient;

        // event data
        private int mType;              // type of the event
        private Throwable mError;

        public Event(ExtendedMediaRecorder recorder, Recipient recipient, int type) {
            this.mLoudness = 0;
            this.mFullDuration = recorder.getFullDuration();
            this.mIntermediateFilePaths = recorder.getIntermediateFilePaths();
            this.mCurrentDuration = System.currentTimeMillis() - recorder.getCurrentStartTime();
            this.mCurrentFilePath = recorder.getCurrentFilePath();
            this.mFilePath = recorder.getFilePath();
            this.mType = type;
            this.mRecipient = recipient;

            if(type == EVENT_LOUDNESS) {
                float amplitude = recorder.getMaxAmplitude();
                float topAmplitude = 500f;

                while((amplitude > topAmplitude)) {
                    topAmplitude += 500f;
                }

                this.mLoudness = amplitude / topAmplitude;
            }
        }

        public Event(ExtendedMediaRecorder recorder, Recipient recipient, Throwable error) {
            this(recorder, recipient, EVENT_ERROR);
            this.mError = error;
        }

        public float getLoudness() {
            return mLoudness;
        }

        public long getFullDuration() {
            return mFullDuration;
        }

        public ArrayList<String> getIntermediateFilePaths() {
            return mIntermediateFilePaths;
        }

        public long getCurrentDuration() {
            return mCurrentDuration;
        }

        public String getCurrentFilePath() {
            return mCurrentFilePath;
        }

        public int getType() {
            return mType;
        }

        public String getFilePath() {
            return mFilePath;
        }

        public Recipient getRecipient() {
            return mRecipient;
        }

        public Throwable getError() {
            return mError;
        }
    }

    /**
     * Async handler to send loudness update events.
     */
    private final Handler mHandler = new Handler();
    private final Runnable mLoudnessRunnable = new Runnable() {
        @Override
        public void run() {
            updateLoudness();
        }
    };

    private transient EventBus mEventBus;                 // event bus to send events to registered listeners
    private transient ExtendedMediaRecorder mRecorder;    // the recorder
    private Recipient mRecipient;                         // the recipient of the current recording

    public RecordService() {
        mEventBus = new EventBus();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);

        if(intent != null && intent.hasExtra(INTENT_DATA_DOSTART) && intent.hasExtra(INTENT_DATA_RECIPIENT)) {
            if(intent.getBooleanExtra(INTENT_DATA_DOSTART, false)) {
                if(intent.hasExtra(INTENT_DATA_FILEPREFIX)) {
                    start(intent.getStringExtra(INTENT_DATA_FILEPREFIX), (Recipient) intent.getSerializableExtra(INTENT_DATA_RECIPIENT));
                } else {
                    start(null, (Recipient) intent.getSerializableExtra(INTENT_DATA_RECIPIENT));
                }
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
        if(isRecording()) {
            stop();
        }
        super.onDestroy();
    }

    boolean isRecording() {
        return mRecorder != null && mRecorder.isOngoing();
    }

    boolean isPaused() {
        return mRecorder != null && mRecorder.isPaused();
    }

    void start(String filePrefix, Recipient recipient) {
        if(isRecording()) {
            throw new RuntimeException("A recording is already in progress. Available actions are pause, resume and stop.");
        }

        mRecipient = recipient;

        if(filePrefix == null) {
            mRecorder = new ExtendedMediaRecorder(RecordService.this);
        } else {
            mRecorder = new ExtendedMediaRecorder(RecordService.this, filePrefix);
        }

        mRecorder.start();
        updateLoudness();

        startForeground(RecordService.class.hashCode(), getNotification());

        Event e = new Event(mRecorder, mRecipient, EVENT_START);
        mEventBus.post(e);
    }

    void pause() {
        if(!isRecording()) {
            throw new RuntimeException("Cannot pause. Nothing is currently being recorded. Use start.");
        }

        mRecorder.pause();

        updateNotification();

        Event e = new Event(mRecorder, mRecipient, EVENT_PAUSE);
        mEventBus.post(e);
    }

    void resume() {
        if(!isRecording()) {
            throw new RuntimeException("Cannot resume. Nothing is currently being recorded. Use start.");
        }

        mRecorder.resume();

        updateLoudness();
        updateNotification();

        Event e = new Event(mRecorder, mRecipient, EVENT_RESUME);
        mEventBus.post(e);
    }

    void stop() {
        try {
            mRecorder.stop();
            Event e = new Event(mRecorder, mRecipient, EVENT_STOP);
            mEventBus.post(e);
        } catch (Exception ex) {
            mRecorder.discard();
            Event e = new Event(mRecorder, mRecipient, ex);
            mEventBus.post(e);
        } finally {
            stopForeground(true);
        }
    }

    void discard() {
        mRecorder.discard();
    }

    private void updateLoudness() {
        if(isRecording() && !mRecorder.isPaused()) {
            if(mEventBus.hasSubscriberForEvent(Event.class)) {
                Event e = new Event(mRecorder, mRecipient, EVENT_LOUDNESS);
                mEventBus.post(e);
            }
            mHandler.postDelayed(mLoudnessRunnable, 100);
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, RecordingActivity.class);
        notificationIntent.putExtra(RecordFragment.INTENT_RECIPIENT_EXTRA, mRecipient);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // make sure that the main activity of the app is present in the backstack
        stackBuilder.addParentStack(RecordingActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // FIXME use proper icons for these notifications
        // TODO add pause/resume + send + discard? actions to notification perhaps?
        // TODO add progress in time and perhaps also the recipient's name to notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(RecordService.this)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(mBinder.isPaused() ? R.string.paused : R.string.recording))
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(RecordService.class.hashCode(), getNotification());
    }
}
