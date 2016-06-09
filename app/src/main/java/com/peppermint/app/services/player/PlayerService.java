package com.peppermint.app.services.player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.io.File;

import de.greenrobot.event.EventBus;

/**
 * Service that plays {@link Message}s. Can be controlled and used from any other app component.
 */
public class PlayerService extends Service {

    private static final String TAG = PlayerService.class.getSimpleName();

    public static final String PARAM_MESSAGE = TAG + "_paramMessage";
    public static final String PARAM_RESET_PROGRESS = TAG + "_paramResetProgress";
    public static final String PARAM_START_PROGRESS = TAG + "_paramStartProgress";

    public static final String ACTION_PLAY = "com.peppermint.app.PlayerService.PLAY";
    public static final String ACTION_PAUSE = "com.peppermint.app.PlayerService.PAUSE";

    private static final EventBus EVENT_BUS = new EventBus();

    static {
        if(PeppermintApp.DEBUG) {
            EVENT_BUS.register(new Object() {
                public void onEventBackgroundThread(PlayerEvent event) {
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

    private static void postPlayerEvent(int type, Message message, int percent, long currentMs, int errorCode) {
        if(EVENT_BUS.hasSubscriberForEvent(PlayerEvent.class)) {
            EVENT_BUS.post(new PlayerEvent(type, message, percent, currentMs, errorCode));
        }
    }

    protected PlayerServiceBinder mBinder = new PlayerServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class PlayerServiceBinder extends Binder {

        /**
         * Play the message starting at startPercent of the total duration.
         * @param message the message
         * @param startPercent the starting point in percentage of the total duration
         */
        void play(Message message, int startPercent) {
            PlayerService.this.play(message, startPercent);
        }

        /**
         * Pause playing the message (if it's the one playing).
         * @param message the message
         * @param resetProgress true to reset the player progress
         * @return true if the message was playing and was paused; false otherwise
         */
        boolean pause(Message message, boolean resetProgress) {
            return PlayerService.this.pause(message, resetProgress);
        }

        /**
         * Stops playing the message (if it's the one playing) and resets the player.
         * @param message the message (can be null to stop anything)
         * @return true if the message was playing and was stopped; false otherwise
         */
        boolean stop(Message message) {
            return PlayerService.this.stop(message);
        }

        /**
         * Set the current position of the player in percentage of the total duration of the message.
         * @param message the message
         * @param percent the position in percentage of the total duration
         * @return true if the message was playing and position was updated; false otherwise
         */
        boolean setPosition(Message message, int percent) {
            return PlayerService.this.setPosition(message, percent);
        }

        /**
         * Stop and release player + shutdown the service.
         */
        void shutdown() {
            stopSelf();
        }

        boolean isPlaying() {
            return PlayerService.this.isPlaying(null);
        }
        boolean isPlaying(Message message) {
            return PlayerService.this.isPlaying(message);
        }

        boolean isLoading() {
            return PlayerService.this.isLoading(null);
        }
        boolean isLoading(Message message) {
            return PlayerService.this.isLoading(message);
        }
    }

    private SensorManager mSensorManager;
    private MediaPlayer mMediaPlayer;
    private Message mMessage;
    private int mStartPercent = 0;
    private boolean mLoading = false;
    private boolean mNearEar = false;
    private boolean mForceReset = false;

    private Handler mHandler = new Handler();

    private Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if(mMessage != null && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mLoading = false;
                postPlayerEvent(PlayerEvent.EVENT_PROGRESS, mMessage, Math.round((float) mMediaPlayer.getCurrentPosition() / (float) mMediaPlayer.getDuration() * 100f), mMediaPlayer.getCurrentPosition(), 0);
                scheduleProgressMonitoring();
            }
        }
    };

    private void scheduleProgressMonitoring() {
        mHandler.removeCallbacks(mProgressRunnable);
        mHandler.postDelayed(mProgressRunnable, 100);
    }

    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mLoading = false;
            postPlayerEvent(PlayerEvent.EVENT_COMPLETED, mMessage, 0, 0, 0);
        }
    };

    private MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mLoading = false;

            if(mMediaPlayer == null) {
                return;
            }

            postPlayerEvent(PlayerEvent.EVENT_PREPARED, mMessage, 0, 0, 0);

            int ms = Math.round(((float) mStartPercent / 100f) * (float) mMediaPlayer.getDuration());
            mMediaPlayer.seekTo(ms);
            mMediaPlayer.start();

            postPlayerEvent(PlayerEvent.EVENT_STARTED, mMessage, 0, 0, 0);

            scheduleProgressMonitoring();
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if(mMediaPlayer == null) {
                return;
            }
            postPlayerEvent(PlayerEvent.EVENT_BUFFERING_UPDATE, mMessage, percent, mMediaPlayer.getCurrentPosition(), 0);
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            final Message message = mMessage;
            stop(false);
            postPlayerEvent(PlayerEvent.EVENT_ERROR, message, 0, 0, what);
            return false;
        }
    };

    private SensorEventListener mProximitySensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                if (event.values[0] == 0) {
                    //near
                    if(!mNearEar) {
                        mNearEar = true;
                        reloadMediaPlayer();
                    }
                } else {
                    //far
                    if(mNearEar) {
                        mNearEar = false;
                        reloadMediaPlayer();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            /* nothing to do here */
        }
    };

    private void reloadMediaPlayer() {
        if(mMediaPlayer == null) {
            return;
        }

        final boolean wasPlaying = mMediaPlayer.isPlaying();
        pause(mMessage, false);
        mForceReset = true;

        if(wasPlaying) {
            final int startPercent = Math.round((mMediaPlayer.getCurrentPosition() - 2000f) / (float) mMediaPlayer.getDuration() * 100f);
            play(mMessage, startPercent < 0 ? 0 : startPercent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        final Sensor proximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if(proximitySensor != null) {
            mSensorManager.registerListener(mProximitySensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            TrackerManager.getInstance(this).log("No Proximity Sensor Available!");
        }
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(mProximitySensorEventListener);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            if(intent.hasExtra(PARAM_MESSAGE)) {
                Message message = (Message) intent.getSerializableExtra(PARAM_MESSAGE);
                if(intent.getAction() == null || intent.getAction().compareTo(ACTION_PLAY) == 0) {
                    play(message, intent.getIntExtra(PARAM_START_PROGRESS, 0));
                } else if(intent.getAction() != null && intent.getAction().compareTo(ACTION_PAUSE) == 0) {
                    pause(message, intent.getBooleanExtra(PARAM_RESET_PROGRESS, false));
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

    private void play(Message message, int startPercent) {
        this.mStartPercent = startPercent;
        if(mForceReset || mMessage == null || !message.equals(mMessage)) {
            mForceReset = false;

            File dataSourceFile = message.getRecordingParameter().getValidatedFile();
            String dataSourceUri = dataSourceFile != null ? dataSourceFile.getAbsolutePath() : null;
            if(dataSourceUri == null) {
                if(!Utils.isInternetAvailable(PlayerService.this)) {
                    mOnErrorListener.onError(null, PlayerEvent.ERROR_NO_CONNECTIVITY, 0);
                    return;
                }
                dataSourceUri = message.getServerCanonicalUrl();
            }

            stop(true);

            mMessage = message;

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(mNearEar ? AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC);
            try {
                mMediaPlayer.setDataSource(dataSourceUri);
            } catch (Throwable e) {
                TrackerManager.getInstance(this).logException(e);
                mOnErrorListener.onError(mMediaPlayer, PlayerEvent.ERROR_DATA_SOURCE, 0);
                return;
            }
            mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnErrorListener(mOnErrorListener);
            try {
                mMediaPlayer.prepareAsync();
                mLoading = true;
            } catch(IllegalStateException e) {
                TrackerManager.getInstance(this).logException(e);
                mOnErrorListener.onError(mMediaPlayer, PlayerEvent.ERROR_ILLEGAL_STATE, 0);
            }
        } else if(!mLoading) {
            mOnPreparedListener.onPrepared(mMediaPlayer);
        }
    }

    private boolean pause(Message message, boolean resetProgress) {
        if(message != null && mMessage != null && !message.equals(mMessage)) {
            return false;
        }

        if(mMediaPlayer != null && (mMediaPlayer.isPlaying() || mLoading)) {
            mMediaPlayer.pause();
            if(resetProgress) {
                mMediaPlayer.seekTo(0);
            }

            postPlayerEvent(PlayerEvent.EVENT_PAUSED, mMessage, 0, mMediaPlayer.getCurrentPosition(), 0);

            return true;
        }

        return false;
    }

    private boolean stop(Message message) {
        if(message != null && mMessage != null && !message.equals(mMessage)) {
            return false;
        }
        return stop(true);
    }

    private boolean stop(boolean doEvent) {
        if(mMediaPlayer != null) {
            if(mMediaPlayer.isPlaying() || mLoading) {
                mLoading = false;
                mMediaPlayer.stop();
            }

            if(doEvent) {
                postPlayerEvent(PlayerEvent.EVENT_STOPPED, mMessage, Math.round((float) mMediaPlayer.getCurrentPosition() / (float) mMediaPlayer.getDuration() * 100f), mMediaPlayer.getCurrentPosition(), 0);
            }

            mMediaPlayer.release();
            mMediaPlayer = null;
            mMessage = null;
            return true;
        }

        return false;
    }

    private boolean setPosition(Message message, int percent) {
        if(message != null && mMessage != null && !message.equals(mMessage)) {
            return false;
        }

        if(mMediaPlayer == null) {
            return false;
        }

        int ms = Math.round(((float) percent / 100f) * (float) mMediaPlayer.getDuration());
        mMediaPlayer.seekTo(ms);

        postPlayerEvent(PlayerEvent.EVENT_PROGRESS, mMessage, percent, ms, 0);

        return true;
    }

    private boolean isPlaying(Message message) {
        if(message != null && mMessage != null && !message.equals(mMessage)) {
            return false;
        }

        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    private boolean isLoading(Message message) {
        if(message != null && mMessage != null && !message.equals(mMessage)) {
            return false;
        }

        return mMediaPlayer != null && mLoading;
    }
}
