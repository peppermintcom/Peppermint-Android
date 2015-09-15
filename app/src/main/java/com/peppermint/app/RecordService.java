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

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.ui.RecordFragment;
import com.peppermint.app.utils.ExtendedMediaRecorder;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import io.fabric.sdk.android.Fabric;

public class RecordService extends Service {

    private static final String TAG = RecordService.class.getSimpleName();

    public static final String INTENT_DATA_DOSTART = "RecordService_DoStart";
    public static final String INTENT_DATA_FILENAME = "RecordService_Filename";
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

        boolean isRecording() {
            return RecordService.this.isRecording();
        }

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
         * Start a recording.
         */
        void start(String filePrefix, Recipient recipient) {
            RecordService.this.start(filePrefix, recipient);
        }

        void pause() {
            RecordService.this.pause();
        }

        void resume() {
            RecordService.this.resume();
        }

        /**
         * Stop the recording with the specified UUID.
         */
        void stop() {
            RecordService.this.stop();
        }

        void discard() {
            RecordService.this.discard();
        }

        void shutdown() {
            RecordService.this.shutdown();
        }
    }

    public class Event {
        private float mLoudness;
        private long mFullDuration;
        private ArrayList<String> mIntermediateFilePaths;
        private long mCurrentDuration;
        private String mCurrentFilePath;
        private String mFilePath;
        private int mType;
        private Recipient mRecipient;
        private Throwable mError;

        public Event(float mLoudness, long mFullDuration, ArrayList<String> mIntermediateFilePaths, long mCurrentDuration, String mCurrentFilePath, int mType, Recipient recipient) {
            this.mLoudness = mLoudness;
            this.mFullDuration = mFullDuration;
            this.mIntermediateFilePaths = mIntermediateFilePaths;
            this.mCurrentDuration = mCurrentDuration;
            this.mCurrentFilePath = mCurrentFilePath;
            this.mType = mType;
            this.mRecipient = recipient;
        }

        public float getLoudness() {
            return mLoudness;
        }

        public void setLoudness(float mLoudness) {
            this.mLoudness = mLoudness;
        }

        public long getFullDuration() {
            return mFullDuration;
        }

        public void setFullDuration(long mFullDuration) {
            this.mFullDuration = mFullDuration;
        }

        public ArrayList<String> getIntermediateFilePaths() {
            return mIntermediateFilePaths;
        }

        public void setIntermediateFilePaths(ArrayList<String> mIntermediateFilePaths) {
            this.mIntermediateFilePaths = mIntermediateFilePaths;
        }

        public long getCurrentDuration() {
            return mCurrentDuration;
        }

        public void setCurrentDuration(long mCurrentDuration) {
            this.mCurrentDuration = mCurrentDuration;
        }

        public String getCurrentFilePath() {
            return mCurrentFilePath;
        }

        public void setCurrentFilePath(String mCurrentFilePath) {
            this.mCurrentFilePath = mCurrentFilePath;
        }

        public int getType() {
            return mType;
        }

        public void setType(int mType) {
            this.mType = mType;
        }

        public String getFilePath() {
            return mFilePath;
        }

        public void setFilePath(String mFilePath) {
            this.mFilePath = mFilePath;
        }

        public Recipient getRecipient() {
            return mRecipient;
        }

        public void setRecipient(Recipient mRecipient) {
            this.mRecipient = mRecipient;
        }

        public Throwable getError() {
            return mError;
        }

        public void setError(Throwable mError) {
            this.mError = mError;
        }
    }

    private final Handler mHandler = new Handler();
    private final Runnable mLoudnessRunnable = new Runnable() {
        @Override
        public void run() {
            updateLoudness();
        }
    };

    private EventBus mEventBus;
    private ExtendedMediaRecorder mRecorder;
    private Recipient mRecipient;

    public RecordService() {
        mEventBus = new EventBus();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);

        if(intent != null && intent.hasExtra(INTENT_DATA_DOSTART) && intent.hasExtra(INTENT_DATA_RECIPIENT)) {
            if(intent.getBooleanExtra(INTENT_DATA_DOSTART, false)) {
                if(intent.hasExtra(INTENT_DATA_FILENAME)) {
                    start(intent.getStringExtra(INTENT_DATA_FILENAME), (Recipient) intent.getSerializableExtra(INTENT_DATA_RECIPIENT));
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
        stop();
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

        Event e = new Event(0, mRecorder.getFullDuration(), mRecorder.getIntermediateFilePaths(), 0, mRecorder.getCurrentFilePath(), EVENT_START, mRecipient);
        mEventBus.post(e);
    }

    void pause() {
        if(!isRecording()) {
            throw new RuntimeException("Cannot pause. Nothing is currently being recorded. Use start.");
        }

        String currentFilePath = mRecorder.getCurrentFilePath();
        long currentDuration = mRecorder.pause();

        updateNotification();

        Event e = new Event(0, mRecorder.getFullDuration(), mRecorder.getIntermediateFilePaths(), currentDuration, currentFilePath, EVENT_PAUSE, mRecipient);
        mEventBus.post(e);
    }

    void resume() {
        if(!isRecording()) {
            throw new RuntimeException("Cannot resume. Nothing is currently being recorded. Use start.");
        }
        mRecorder.resume();

        updateLoudness();
        updateNotification();

        Event e = new Event(0, mRecorder.getFullDuration(), mRecorder.getIntermediateFilePaths(), 0, mRecorder.getCurrentFilePath(), EVENT_RESUME, mRecipient);
        mEventBus.post(e);
    }

    void stop() {
        try {
            String currentFilePath = mRecorder.getCurrentFilePath();
            long currentDuration = mRecorder.stop();
            Event e = new Event(0, mRecorder.getFullDuration(), mRecorder.getIntermediateFilePaths(), currentDuration, currentFilePath, EVENT_STOP, mRecipient);
            e.setFilePath(mRecorder.getFilePath());
            mEventBus.post(e);
        } catch (Exception ex) {
            mRecorder.discard();
            Event e = new Event(0, mRecorder.getFullDuration(), mRecorder.getIntermediateFilePaths(), 0, mRecorder.getCurrentFilePath(), EVENT_ERROR, mRecipient);
            e.setError(ex);
            mEventBus.post(e);
        } finally {
            stopForeground(true);
        }
    }

    void discard() {
        mRecorder.discard();
    }

    void shutdown() {
        if(isRecording()) {
            stop();
        }
        stopSelf();
    }

    private void updateLoudness() {
        if(isRecording() && !mRecorder.isPaused()) {
            float amplitude = mRecorder.getMaxAmplitude();
            float topAmplitude = 500f;

            while((amplitude > topAmplitude)) {
                topAmplitude += 500f;
            }

            float factor = amplitude / topAmplitude;

            Event e = new Event(factor, mRecorder.getFullDuration(), mRecorder.getIntermediateFilePaths(),
                    ((System.currentTimeMillis() - mRecorder.getCurrentStartTime()) / 1000), mRecorder.getCurrentFilePath(), EVENT_LOUDNESS, mRecipient);
            mEventBus.post(e);

            mHandler.postDelayed(mLoudnessRunnable, 100);
        }
    }

    private Notification getNotification() {

        Intent notificationIntent = new Intent(this, RecordActivity.class);
        notificationIntent.putExtra(RecordFragment.RECIPIENT_EXTRA, mRecipient);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(RecordActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
/*
        Intent notificationIntent = new Intent(RecordService.this, RecordActivity.class);
        notificationIntent.putExtra(RecordFragment.RECIPIENT_EXTRA, mRecipient);
        PendingIntent pendingIntent = PendingIntent.getActivity(RecordService.this, 0, notificationIntent, 0);*/

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
