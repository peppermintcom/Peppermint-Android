package com.peppermint.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import de.greenrobot.event.EventBus;

public class RecordService extends Service {

    private static final String TAG = RecordService.class.getSimpleName();

    public static final String INTENT_DATA_DOSTART = "RecordService_DoStart";

    private static final String FILENAME = "PepperMintRecord";

    public static final int EVENT_START = 1;
    public static final int EVENT_RESUME = 2;
    public static final int EVENT_PAUSE = 3;
    public static final int EVENT_STOP = 4;
    public static final int EVENT_LOUDNESS = 5;

    public static final int EVENT_START_SEND = 6;
    public static final int EVENT_FINISHED_SEND = 7;

    protected RecordServiceBinder mBinder = new RecordServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class RecordServiceBinder extends Binder {

        boolean isRecording() {
            return mOngoing;
        }

        boolean isPaused() {
            return mOngoing && mRecorder == null;
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
        void start() {
            if(mOngoing) {
                throw new RuntimeException("A recording is already in progress. Available actions are pause, resume and stop.");
            }

            mFullFilePaths = new ArrayList<>();
            mFullDuration = 0;
            mOngoing = true;
            startRecording();

            startForeground(RecordService.class.hashCode(), getNotification());

            Event e = new Event(0, mFullDuration, mFullFilePaths, ((System.currentTimeMillis() - mCurrentStartTime) / 1000), mCurrentFilePath, EVENT_START);
            mEventBus.post(e);
        }

        void pause() {
            if(!mOngoing) {
                throw new RuntimeException("Cannot pause. Nothing is currently being recorded. Use start.");
            }

            String currentFilePath = mCurrentFilePath;
            long currentDuration = stopRecording();

            updateNotification();

            Event e = new Event(0, mFullDuration, mFullFilePaths, currentDuration, currentFilePath, EVENT_PAUSE);
            mEventBus.post(e);
        }

        void resume() {
            if(!mOngoing) {
                throw new RuntimeException("Cannot resume. Nothing is currently being recorded. Use start.");
            }

            startRecording();

            updateNotification();

            Event e = new Event(0, mFullDuration, mFullFilePaths, 0, mCurrentFilePath, EVENT_RESUME);
            mEventBus.post(e);
        }

        /**
         * Stop the recording with the specified UUID.
         */
        void stop() {
            String currentFilePath = mCurrentFilePath;
            long currentDuration = stopRecording();

            stopForeground(true);
            mOngoing = false;

            Event e = new Event(0, mFullDuration, mFullFilePaths, currentDuration, currentFilePath, EVENT_STOP);
            mEventBus.post(e);
        }

        void discard() {
            if(mOngoing) {
                stop();
            }

            RecordService.this.discard();
        }

        void shutdown() {
            if(mOngoing) {
                stop();
            }
            stopSelf();
        }

        ArrayList<String> getFullFilePaths() {
            return mFullFilePaths;
        }
    }

    public class Event {
        private float mLoudness;
        private long mFullDuration;
        private ArrayList<String> mFullFilePaths;
        private long mCurrentDuration;
        private String mCurrentFilePath;
        private int mType;

        public Event(float mLoudness, long mFullDuration, ArrayList<String> mFullFilePaths, long mCurrentDuration, String mCurrentFilePath, int mType) {
            this.mLoudness = mLoudness;
            this.mFullDuration = mFullDuration;
            this.mFullFilePaths = mFullFilePaths;
            this.mCurrentDuration = mCurrentDuration;
            this.mCurrentFilePath = mCurrentFilePath;
            this.mType = mType;
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

        public ArrayList<String> getFullFilePaths() {
            return mFullFilePaths;
        }

        public void setFullFilePaths(ArrayList<String> mFullFilePaths) {
            this.mFullFilePaths = mFullFilePaths;
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
    }

    private final Handler mHandler = new Handler();
    private final Runnable mLoudnessRunnable = new Runnable() {
        @Override
        public void run() {
            updateLoudness();
        }
    };

    private EventBus mEventBus;

    private ArrayList<String> mFullFilePaths;

    private MediaRecorder mRecorder = null;
    private boolean mOngoing = false;

    private long mFullDuration = 0;
    private long mCurrentStartTime = 0;
    private String mCurrentFilePath;

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

        if(intent != null && intent.hasExtra(INTENT_DATA_DOSTART)) {
            if(intent.getBooleanExtra(INTENT_DATA_DOSTART, false)) {
                startRecording();
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
        mBinder.stop();
        super.onDestroy();
    }

    private void startRecording() {
        if(getExternalCacheDir() == null) {
            throw new NullPointerException("No access to external cache directory!");
        }

        mCurrentFilePath = getExternalCacheDir().getAbsolutePath() + "/" + FILENAME + "_" + UUID.randomUUID().toString() + ".mp3";

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mCurrentFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mCurrentStartTime = System.currentTimeMillis();
        mRecorder.start();

        updateLoudness();
    }

    private long stopRecording() {
        if(mRecorder != null) {
            long currentDuration = 0;
            try {
                mRecorder.stop();
                currentDuration = ((System.currentTimeMillis() - mCurrentStartTime) / 1000);
                mFullDuration += currentDuration;
                mFullFilePaths.add(mCurrentFilePath);
                try {
                    mergeFiles();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to merge files", e);
                }
            } catch(RuntimeException e) {
                File f = new File(mCurrentFilePath);
                if(f.exists()) {
                    f.delete();
                }
            } finally {
                mRecorder.release();
                mRecorder = null;
                mCurrentStartTime = 0;
                mCurrentFilePath = null;
            }
            return currentDuration;
        }

        return 0;
    }

    private boolean discard() {
        boolean allDeleted = true;
        for(String filePath : mFullFilePaths) {
            File f = new File(filePath);
            if(f.exists()) {
                allDeleted = f.delete() && allDeleted;
            }
        }
        return allDeleted;
    }

    // https://stackoverflow.com/questions/23129561/how-to-concat-mp4-files/23144266#23144266
    // https://github.com/sannies/mp4parser
    private void mergeFiles() throws Exception {
        if(mFullFilePaths == null || mFullFilePaths.size() <= 1) {
            return;
        }

        if(getExternalCacheDir() == null) {
            throw new NullPointerException("No access to external cache directory!");
        }

        //List<Track> videoTracks = new LinkedList<Track>();
        List<Track> audioTracks = new LinkedList<>();

        for(String filePath : mFullFilePaths) {
            Movie m = MovieCreator.build(filePath);
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
                /*if (t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                }*/
            }
        }

        Movie result = new Movie();
        /*if (videoTracks.size() > 0) {
            result.addTrack(new AppendTrack(videoTracks
                    .toArray(new Track[videoTracks.size()])));
        }*/
        if (audioTracks.size() > 0) {
            result.addTrack(new AppendTrack(audioTracks
                    .toArray(new Track[audioTracks.size()])));
        }

        String newFilePath = getExternalCacheDir().getAbsolutePath() + "/Merged" + FILENAME + "_" + UUID.randomUUID().toString() + ".mp3";
        Container out = new DefaultMp4Builder().build(result);
        RandomAccessFile ram = new RandomAccessFile(newFilePath, "rw");
        FileChannel fc = ram.getChannel();
        out.writeContainer(fc);
        ram.close();
        fc.close();

        discard();
        mFullFilePaths.clear();
        mFullFilePaths.add(newFilePath);
    }

    private void updateLoudness() {
        if(mOngoing && mRecorder != null) {

            float amplitude = mRecorder.getMaxAmplitude();
            float topAmplitude = 500f;

            while((amplitude > topAmplitude)) {
                topAmplitude += 500f;
            }

            float factor = amplitude / topAmplitude;

            Event e = new Event(factor, mFullDuration, mFullFilePaths, ((System.currentTimeMillis() - mCurrentStartTime) / 1000), mCurrentFilePath, EVENT_LOUDNESS);
            mEventBus.post(e);

            mHandler.postDelayed(mLoudnessRunnable, 100);
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(RecordService.this, RecordActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(RecordService.this, 0, notificationIntent, 0);

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
