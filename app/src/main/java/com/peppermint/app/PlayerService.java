package com.peppermint.app;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.peppermint.app.data.Message;
import com.peppermint.app.tracking.TrackerManager;

import java.util.UUID;

import de.greenrobot.event.EventBus;

/**
 * Service that allows the background_gradient sending of files through different methods.
 */
public class PlayerService extends Service {

    private static final String TAG = PlayerService.class.getSimpleName();

    public static final String PARAM_MESSAGE = TAG + "_paramMessage";
    public static final String PARAM_RESET_PROGRESS = TAG + "_paramResetProgress";
    public static final String PARAM_START_PROGRESS = TAG + "_paramStartProgress";

    public static final String ACTION_PLAY = "com.peppermint.app.PlayerService.PLAY";
    public static final String ACTION_PAUSE = "com.peppermint.app.PlayerService.PAUSE";

    protected PlayerServiceBinder mBinder = new PlayerServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class PlayerServiceBinder extends Binder {

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
         * @param message the message
         * @return the {@link UUID} of the send file request/task
         */
        void play(Message message, int startPercent) {
            PlayerService.this.play(message, startPercent);
        }

        /**
         * Cancel the message with the specified {@link UUID}.
         * <b>If the message is being sent, it might get sent anyway.</b>
         * @param message the UUID of the message returned by {@link #pause(Message, boolean)}
         */
        boolean pause(Message message, boolean resetProgress) {
            return PlayerService.this.pause(message, resetProgress);
        }

        boolean setPosition(Message message, int percent) {
            return PlayerService.this.setPosition(message, percent);
        }

        /**
         * Cancel all pending and ongoing send requests.
         * <b>If a send request is ongoing, it might get sent anyway.</b>
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
    }

    private TrackerManager mTrackerManager;
    private EventBus mEventBus;

    private MediaPlayer mMediaPlayer;
    private Message mMessage;
    private int mStartPercent = 0;

    private Handler mHandler = new Handler();

    private Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if(mMessage != null && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mEventBus.post(new PlayerEvent(PlayerEvent.EVENT_PROGRESS, mMessage, Math.round((float) mMediaPlayer.getCurrentPosition() / (float) mMediaPlayer.getDuration() * 100f), mMediaPlayer.getCurrentPosition()));
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
            mEventBus.post(new PlayerEvent(PlayerEvent.EVENT_COMPLETED, mMessage, 0, 0));
        }
    };

    private MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mEventBus.post(new PlayerEvent(PlayerEvent.EVENT_PREPARED, mMessage, 0, 0));
            int ms = Math.round(((float) mStartPercent / 100f) * (float) mMediaPlayer.getDuration());
            mMediaPlayer.seekTo(ms);
            mMediaPlayer.start();
            mEventBus.post(new PlayerEvent(PlayerEvent.EVENT_STARTED, mMessage, 0, 0));
            scheduleProgressMonitoring();
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mEventBus.post(new PlayerEvent(PlayerEvent.EVENT_BUFFERING_UPDATE, mMessage, percent, mMediaPlayer.getCurrentPosition()));
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            if(mMediaPlayer != null) {
                if(mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
            mEventBus.post(new PlayerEvent(PlayerEvent.EVENT_ERROR, mMessage, 0, 0));
            return false;
        }
    };

    public PlayerService() {
        mEventBus = new EventBus();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTrackerManager = TrackerManager.getInstance(getApplicationContext());
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

    @Override
    public void onDestroy() {
        // unregister before deinit() to avoid removing cancelled messages
        // must retry these after reboot
        mEventBus.unregister(this);

        super.onDestroy();
    }

    private void play(Message message, int startPercent) {
        this.mStartPercent = startPercent;
        if(mMessage == null || !message.equals(mMessage)) {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
            }
            mMessage = message;
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mMediaPlayer.setDataSource(message.getRecording().getFilePath() != null ? message.getRecording().getFilePath() : message.getServerCanonicalUrl());
            } catch (Throwable e) {
                mTrackerManager.logException(e);
            }
            mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnErrorListener(mOnErrorListener);
            mMediaPlayer.prepareAsync();
        } else {
            mOnPreparedListener.onPrepared(mMediaPlayer);
        }
    }

    private boolean pause(Message message, boolean resetProgress) {
        if(message != null && mMessage != null && !message.equals(mMessage)) {
            return false;
        }

        if(mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            if(resetProgress) {
                mMediaPlayer.seekTo(0);
            }
            mEventBus.post(new PlayerEvent(PlayerEvent.EVENT_PAUSED, mMessage, 0, mMediaPlayer.getCurrentPosition()));
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
        mEventBus.post(new PlayerEvent(PlayerEvent.EVENT_PROGRESS, mMessage, percent, ms));
        return true;
    }

    private boolean isPlaying(Message message) {
        if(message != null && mMessage != null && !message.equals(mMessage)) {
            return false;
        }

        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }
}
