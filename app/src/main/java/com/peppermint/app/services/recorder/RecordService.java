package com.peppermint.app.services.recorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.google.cloud.speech.v1.nano.InitialRecognizeRequest;
import com.peppermint.app.BuildConfig;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.cloud.apis.speech.GoogleSpeechRecognizeClient;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.contact.ContactRaw;
import com.peppermint.app.dal.recording.Recording;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.views.CustomToast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;
import io.grpc.Status;

/**
 * Service that records audio messages.
 */
public class RecordService extends Service {

    private static final String TAG = RecordService.class.getSimpleName();

    /** Intent action to pop recording transcription. **/
    public static final String ACTION_POP_TRANSCRIPTION = "com.peppermint.app.RecordService.POP_TRANSCRIPTION";

    /** Intent action to start recording. **/
    public static final String ACTION_START_RECORDING = "com.peppermint.app.RecordService.START_RECORDING";

    /**
        Intent extra key for a string with the file path for the transcribed file.
        This should be supplied for the POP_TRANSCRIPTION action.
     **/
    public static final String INTENT_DATA_TRANSCRIPTION_FILEPATH = TAG + "_TranscriptionFilePath";

    /**
        Intent extra key for a string with the filename prefix for the recorded file.
        This should be supplied for the START_RECORDING action.
     **/
    public static final String INTENT_DATA_FILEPREFIX = TAG + "_FilePrefix";

    /**
        Intent extra key for the {@link ContactRaw} of the recorded file.
        This service doesn't handle the sending of files but the recipient is required to
        keep track of the sending process in case the interface (activity) gets closed.
        This <b>must</b> be supplied for the START_RECORDING action.
     **/
    public static final String INTENT_DATA_CHAT = TAG + "_Chat";

    /**
        Intent extra key for a long with the max duration for the recorded file in millis.
        This should be supplied for the START_RECORDING action.
     **/
    public static final String INTENT_DATA_MAXDURATION = TAG + "_MaxDuration";

    private static final EventBus EVENT_BUS = new EventBus();

    static {
        if(BuildConfig.DEBUG) {
            EVENT_BUS.register(new Object() {
                public void onEventBackgroundThread(RecorderEvent event) {
                    Log.d(TAG, event.toString());
                }
            });
        }
    }

    public static void registerEventListener(Object listener) {
        EVENT_BUS.register(listener);
    }

    public static void registerEventListener(Object listener, int priority) {
        EVENT_BUS.register(listener, priority);
    }

    public static void unregisterEventListener(Object listener) {
        EVENT_BUS.unregister(listener);
    }

    public static void postRecorderEvent(int type, Recording recording, Chat chat, float loudness, Throwable error) {
        if(EVENT_BUS.hasSubscriberForEvent(RecorderEvent.class)) {
            EVENT_BUS.post(new RecorderEvent(type, recording, chat, loudness, error));
        }
    }

    protected RecordServiceBinder mBinder = new RecordServiceBinder();

    private float mMaxAmplitude;
    private Map<String, GoogleSpeechRecognizeClient> mSpeechRecognizers = new ArrayMap<>();
    private Map<String, GoogleSpeechRecognizeClient.RecognizeResponseWrapper> mTranscriptionResults = new ConcurrentHashMap<>();

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

    private Recording newRecording(ExtendedAudioRecorder recorder) {
        final Object[] transcriptionData = GoogleSpeechRecognizeClient.getBestTranscriptionResults(mTranscriptionResults.get(recorder.getFilePath()));
        final Recording recording = new Recording(recorder.getFilePath(), recorder.getFullDuration(), recorder.getFullSize(), false);
        recording.setTranscription((String) transcriptionData[0]);
        recording.setTranscriptionConfidence((float) transcriptionData[1]);
        recording.setTranscriptionLanguage((String) transcriptionData[2]);
        recording.setRecordedTimestamp(recorder.getStartTimestamp());
        return recording;
    }

    private final ExtendedAudioRecorder.Listener mAudioRecorderListener = new ExtendedAudioRecorder.Listener() {

        private FileOutputStream mMintOutStream;

        @Override
        public void onStart(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp) {
            updateLoudness();

            if(mIsInForegroundMode) {
                updateNotification();
            } else {
                startForeground(RecordService.class.hashCode(), getNotification());
                mIsInForegroundMode = true;
            }

            postRecorderEvent(RecorderEvent.EVENT_START, newRecording(mRecorder), mChat, amplitude, null);
        }

        @Override
        public void onPause(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp) {
            updateNotification();
            postRecorderEvent(RecorderEvent.EVENT_PAUSE, newRecording(mRecorder), mChat, amplitude, null);
        }

        @Override
        public void onResume(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp) {
            updateLoudness();
            updateNotification();
            postRecorderEvent(RecorderEvent.EVENT_RESUME, newRecording(mRecorder), mChat, amplitude, null);
        }

        @Override
        public void onStop(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp) {
            if(mIsInForegroundMode) {
                stopForeground(true);
                mIsInForegroundMode = false;
            }

            finishSpeechToText(filePath);

            postRecorderEvent(RecorderEvent.EVENT_STOP, newRecording(mRecorder), mChat, amplitude, null);
        }

        @Override
        public void onError(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp, Throwable t) {
            if(mIsInForegroundMode) {
                stopForeground(true);
                mIsInForegroundMode = false;
            }

            finishSpeechToText(filePath);

            postRecorderEvent(RecorderEvent.EVENT_ERROR, newRecording(mRecorder), mChat, 0, t);
        }

        @Override
        public void onAudioRecordFound(String filePath, int sampleRate) {
            if(!mPreferences.isAutomaticTranscription()) {
                return;
            }

            final String transcriptionLanguage = mPreferences.getTranscriptionLanguageCode();

            try {
                final GoogleSpeechRecognizeClient client = new GoogleSpeechRecognizeClient(RecordService.this, filePath);
                client.setRecognitionListener(mSpeechRecognitionListener);
                client.startSending(InitialRecognizeRequest.LINEAR16, sampleRate, transcriptionLanguage);
                mSpeechRecognizers.put(filePath, client);
            } catch (Exception e) {
                TrackerManager.getInstance(RecordService.this).logException(e);
            }


            try {
                final File mintFile = new File(filePath + ".mint");
                mMintOutStream = new FileOutputStream(mintFile);
                mMintOutStream.write(ByteBuffer.allocate(4).putInt(sampleRate).array());
                mMintOutStream.write(ByteBuffer.allocate(4).putInt(transcriptionLanguage.length()).array());
                mMintOutStream.write(transcriptionLanguage.getBytes("UTF-8"));
            } catch (java.io.IOException e) {
                TrackerManager.getInstance(RecordService.this).logException(e);
            }
        }

        @Override
        public void onRecorderData(String filePath, long durationInMillis, float sizeKbs, int amplitude, String startTimestamp, byte[] bytes) {
            if(!mPreferences.isAutomaticTranscription()) {
                return;
            }

            if(mMintOutStream != null) {
                try {
                    mMintOutStream.write(bytes);
                } catch (IOException e) {
                    TrackerManager.getInstance(RecordService.this).logException(e);
                }
            }

            final GoogleSpeechRecognizeClient client = mSpeechRecognizers.get(filePath);
            if(client == null) {
                Log.w(TAG, "No SpeechRecognizeClient found for " + filePath);
                return;
            }

            try {
                client.recognize(bytes);
            } catch (Throwable e) {
                TrackerManager.getInstance(RecordService.this).logException(e);
            }
        }

        private void finishSpeechToText(String filePath) {
            if(mMintOutStream != null) {
                try {
                    mMintOutStream.flush();
                    mMintOutStream.close();
                } catch (IOException e) {
                    TrackerManager.getInstance(RecordService.this).logException(e);
                }
                mMintOutStream = null;
            }

            final GoogleSpeechRecognizeClient client = mSpeechRecognizers.get(filePath);
            if(client == null) {
                Log.w(TAG, "No SpeechRecognizeClient found for " + filePath);
                return;
            }

            client.finishSending();
        }
    };

    private final GoogleSpeechRecognizeClient.RecognitionListener mSpeechRecognitionListener = new GoogleSpeechRecognizeClient.RecognitionListener() {
        @Override
        public void onRecognitionStarted(GoogleSpeechRecognizeClient client) {
        }

        @Override
        public void onRecognitionFinished(GoogleSpeechRecognizeClient client, GoogleSpeechRecognizeClient.RecognizeResponseWrapper lastResponse) {
            mSpeechRecognizers.remove(client.getId());
            if(lastResponse != null) {
                mTranscriptionResults.put(client.getId(), lastResponse);
            }
            postRecorderEvent(RecorderEvent.EVENT_TRANSCRIPTION, newRecording(mRecorder), mChat, 0, null);
        }

        @Override
        public void onRecognitionError(GoogleSpeechRecognizeClient client, Status status, GoogleSpeechRecognizeClient.RecognizeResponseWrapper lastResponse) {
            mSpeechRecognizers.remove(client.getId());
            if(lastResponse != null) {
                mTranscriptionResults.put(client.getId(), lastResponse);
            }
            postRecorderEvent(RecorderEvent.EVENT_TRANSCRIPTION, newRecording(mRecorder), mChat, 0, null);
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
    private SenderPreferences mPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = new SenderPreferences(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction() != null) {
            if(intent.getAction().compareTo(ACTION_START_RECORDING) == 0 && intent.hasExtra(INTENT_DATA_CHAT)) {
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
            } else if(intent.getAction().compareTo(ACTION_POP_TRANSCRIPTION) == 0 && intent.hasExtra(INTENT_DATA_TRANSCRIPTION_FILEPATH)) {
                String filePath = intent.getStringExtra(INTENT_DATA_TRANSCRIPTION_FILEPATH);
                if(!mSpeechRecognizers.containsKey(filePath)) {
                    popTranscription(filePath, 0, true);
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

    String popTranscription(String filePath, float minConfidence, boolean sendEvent) {
        if(!mTranscriptionResults.containsKey(filePath)) {
            if(sendEvent) {
                postRecorderEvent(RecorderEvent.EVENT_TRANSCRIPTION, new Recording(filePath), null, 0, null);
            }
            return null;
        }

        final Object[] transcriptionData = GoogleSpeechRecognizeClient.getBestTranscriptionResults(mTranscriptionResults.get(filePath));
        mTranscriptionResults.remove(filePath);

        if(sendEvent) {
            Recording recording = new Recording(filePath);
            recording.setTranscription((String) transcriptionData[0]);
            recording.setTranscriptionConfidence((float) transcriptionData[1]);
            recording.setTranscriptionLanguage((String) transcriptionData[2]);
            postRecorderEvent(RecorderEvent.EVENT_TRANSCRIPTION, recording, null, 0, null);
        }

        if((float) transcriptionData[1] >= minConfidence) {
            return (String) transcriptionData[0];
        }

        return null;
    }

    boolean isTranscribing(String filePath) {
        return mSpeechRecognizers.containsKey(filePath);
    }

    boolean isTranscribing() { return mSpeechRecognizers.size() > 0; }

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
            postRecorderEvent(RecorderEvent.EVENT_LOUDNESS, newRecording(mRecorder), mChat, getLoudnessFromAmplitude(mRecorder.getAmplitude()), null);
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
