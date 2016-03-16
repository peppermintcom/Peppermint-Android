package com.peppermint.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.Recording;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.RecorderEvent;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.views.simple.CustomToast;
import com.peppermint.app.utils.ExtendedAudioRecorder;
import com.peppermint.app.utils.NoAccessToExternalStorageException;

/**
 * Service that records audio messages.
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
        Intent extra key for the {@link ContactRaw} of the recorded file.
        This service doesn't handle the sending of files but the recipient is required to
        keep track of the sending process in case the interface (activity) gets closed.
        This <b>must</b> be supplied if the DOSTART flag is true.
     **/
    public static final String INTENT_DATA_CHAT = "RecordService_Chat";

    /**
     Intent extra key for a long with the max duration for the recorded file in millis.
     This should be supplied if the DOSTART flag is true.
     **/
    public static final String INTENT_DATA_MAXDURATION = "RecordService_MaxDuration";

    protected RecordServiceBinder mBinder = new RecordServiceBinder();

    private float mMaxAmplitude;

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
         * Start a recording. You can only start a recording if no other recording is currently
         * active (even if it is paused).
         * @param filePrefix the filename prefix of the record
         * @param chat the chat of the record
         */
        void start(String filePrefix, Chat chat, long maxDurationMillis) {
            try {
                RecordService.this.start(filePrefix, chat, maxDurationMillis);
            } catch (NoAccessToExternalStorageException e) {
                CustomToast.makeText(RecordService.this, R.string.msg_no_external_storage, Toast.LENGTH_LONG).show();
                Log.e(TAG, e.getMessage(), e);
                TrackerManager.getInstance(getApplicationContext()).logException(e);
            }
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
        void stop(boolean discard) {
            RecordService.this.stop(discard);
        }

        /**
         * Shutdown the service, stopping and discarding the current recording.
         */
        void shutdown() {
            stopSelf();
        }

        long getCurrentFullDuration() {
            return mRecorder == null ? 0 : mRecorder.getFullDuration();
        }

        float getCurrentLoudness() {
            return mRecorder == null ? 0 : getLoudnessFromAmplitude(mRecorder.getAmplitude());
        }

        String getCurrentFilePath() {
            return mRecorder == null ? null : mRecorder.getFilePath();
        }

        Chat getCurrentChat() {
            return mChat;
        }

        Recording getCurrentRecording() {
            if(mRecorder == null) {
                return null;
            }
            return newRecording(mRecorder);
        }
    }

    private float getLoudnessFromAmplitude(float amplitude) {
        while((amplitude / mMaxAmplitude) > 0.9f) {
            mMaxAmplitude += 200f;
        }

        // gradually adapt the max amplitude to allow useful loudness range values
        while(mMaxAmplitude > 300f && (amplitude / mMaxAmplitude) < 0.1f) {
            mMaxAmplitude -= 300f;
        }

        if(mMaxAmplitude < 300) {
            mMaxAmplitude = 300;
        }

        return Math.min(1, amplitude / mMaxAmplitude);
    }

    private static Recording newRecording(ExtendedAudioRecorder recorder) {
        Recording recording = new Recording(recorder.getFilePath(), recorder.getFullDuration(), recorder.getFullSize(), false);
        recording.setRecordedTimestamp(recorder.getStartTimestamp());
        return recording;
    }

    private final ExtendedAudioRecorder.Listener mAudioRecorderListener = new ExtendedAudioRecorder.Listener() {

        @Override
        public void onStart(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp) {
            updateLoudness();

            if(mIsInForegroundMode) {
                updateNotification();
            } else {
                startForeground(RecordService.class.hashCode(), getNotification());
                mIsInForegroundMode = true;
            }

            PeppermintEventBus.postRecorderEvent(RecorderEvent.EVENT_START, newRecording(mRecorder), mChat, amplitude, null);
        }

        @Override
        public void onPause(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp) {
            updateNotification();
            PeppermintEventBus.postRecorderEvent(RecorderEvent.EVENT_PAUSE, newRecording(mRecorder), mChat, amplitude, null);;
        }

        @Override
        public void onResume(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp) {
            updateLoudness();
            updateNotification();
            PeppermintEventBus.postRecorderEvent(RecorderEvent.EVENT_RESUME, newRecording(mRecorder), mChat, amplitude, null);
        }

        @Override
        public void onStop(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp) {
            if(mIsInForegroundMode) {
                stopForeground(true);
                mIsInForegroundMode = false;
            }

            PeppermintEventBus.postRecorderEvent(RecorderEvent.EVENT_STOP, newRecording(mRecorder), mChat, amplitude, null);
        }

        @Override
        public void onError(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp, Throwable t) {
            if(mIsInForegroundMode) {
                stopForeground(true);
                mIsInForegroundMode = false;
            }

            PeppermintEventBus.postRecorderEvent(RecorderEvent.EVENT_ERROR, newRecording(mRecorder), mChat, 0, t);
        }
    };

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

    private transient ExtendedAudioRecorder mRecorder;    // the recorder
    private Chat mChat;                                   // the chat of the current recording
    private boolean mIsInForegroundMode = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.hasExtra(INTENT_DATA_DOSTART) && intent.hasExtra(INTENT_DATA_CHAT)) {
            if(intent.getBooleanExtra(INTENT_DATA_DOSTART, false)) {
                try {
                    if (intent.hasExtra(INTENT_DATA_FILEPREFIX)) {
                        start(intent.getStringExtra(INTENT_DATA_FILEPREFIX), (Chat) intent.getSerializableExtra(INTENT_DATA_CHAT), intent.getLongExtra(INTENT_DATA_MAXDURATION, -1));
                    } else {
                        start(null, (Chat) intent.getSerializableExtra(INTENT_DATA_CHAT), intent.getLongExtra(INTENT_DATA_MAXDURATION, -1));
                    }
                } catch (NoAccessToExternalStorageException e) {
                    CustomToast.makeText(RecordService.this, R.string.msg_no_external_storage, Toast.LENGTH_LONG).show();
                    Log.e(TAG, e.getMessage(), e);
                    TrackerManager.getInstance(getApplicationContext()).logException(e);
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
            stop(true);
        }
        if(mIsInForegroundMode) {
            stopForeground(true);
            mIsInForegroundMode = false;
        }
        super.onDestroy();
    }

    boolean isRecording() {
        return mRecorder != null && mRecorder.isRecording();
    }

    boolean isPaused() {
        return mRecorder != null && mRecorder.isPaused();
    }

    void start(String filePrefix, Chat chat, long maxDurationMillis) throws NoAccessToExternalStorageException {
        if(isRecording()) {
            throw new RuntimeException("A recording is already in progress. Available actions are pause, resume and stop.");
        }

        mChat = chat;
        mMaxAmplitude = 1500f;

        if(filePrefix == null) {
            mRecorder = new ExtendedAudioRecorder(RecordService.this);
        } else {
            mRecorder = new ExtendedAudioRecorder(RecordService.this, filePrefix);
        }

        mRecorder.setListener(mAudioRecorderListener);
        mRecorder.start(maxDurationMillis);
    }

    void pause() {
        if(!isRecording()) {
            throw new RuntimeException("Cannot pause. Nothing is currently being recorded. Use start or resume.");
        }

        mRecorder.pause();
    }

    void resume() {
        if(!isPaused()) {
            throw new RuntimeException("Cannot resume. Must be paused to resume!");
        }

        mRecorder.resume();
    }

    void stop(boolean discard) {
        mRecorder.stop(discard);
    }

    private void updateLoudness() {
        if(isRecording()) {
            PeppermintEventBus.postRecorderEvent(RecorderEvent.EVENT_LOUDNESS, newRecording(mRecorder), mChat, getLoudnessFromAmplitude(mRecorder.getAmplitude()), null);
            mHandler.postDelayed(mLoudnessRunnable, 50);
        }
    }

    private Notification getNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(RecordService.this)
                .setSmallIcon(R.drawable.ic_mic_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(mBinder.isPaused() ? R.string.paused : R.string.recording));
        return builder.build();
    }

    private void updateNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(RecordService.class.hashCode(), getNotification());
    }
}
