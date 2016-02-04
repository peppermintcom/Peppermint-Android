package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.ui.views.simple.CustomFontTextView;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * ArrayAdapter to show recipients in a ListView.<br />
 * Uses the {@link RecipientAdapterUtils#getView(PeppermintApp, Context, Recipient, View, ViewGroup)}
 * to fill the view of each item.
 */
public class MessageCursorAdapter extends CursorAdapter {

    private Handler mHandler = new Handler();
    private MessageController mPlayingController;
    private Map<Long, MessageController> mControllers = new HashMap<>();

    private Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if(mPlayingController != null) {
                mPlayingController.updateProgress();
                scheduleProgressMonitoring();
            }
        }
    };

    private void scheduleProgressMonitoring() {
        mHandler.removeCallbacks(mProgressRunnable);
        mHandler.postDelayed(mProgressRunnable, 100);
    }

    private class MessageController implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, OnErrorListener {

        private long mMessageId;
        private String mAudioUrl;
        private long mDuration, mProgress;
        private View mRootView;
        private MediaPlayer mMediaPlayer;

        private MessageController(long mMessageId, String mAudioUrl, long mDuration) {
            this.mMessageId = mMessageId;
            this.mAudioUrl = mAudioUrl;
            this.mDuration = mDuration;
        }

        private MessageController(long mMessageId, String mAudioUrl, long mDuration, View mRootView) {
            this(mMessageId, mAudioUrl, mDuration);
            setRootView(mRootView);
        }

        private void setVisibility(int playVisibility, int pauseVisibility, int progressVisibility) {
            if(mRootView != null) {
                ImageButton btnPlay = (ImageButton) mRootView.findViewById(R.id.btnPlay);
                ImageButton btnPause = (ImageButton) mRootView.findViewById(R.id.btnPause);
                ProgressBar progressBar = (ProgressBar) mRootView.findViewById(R.id.progressBar);
                btnPlay.setVisibility(playVisibility);
                btnPause.setVisibility(pauseVisibility);
                progressBar.setVisibility(progressVisibility);
            }
        }

        private void pause(boolean resetSeekbar) {
            if(mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                if(resetSeekbar) {
                    mMediaPlayer.seekTo(0);
                }
            }

            if(mRootView != null) {
                ImageButton btnPlay = (ImageButton) mRootView.findViewById(R.id.btnPlay);
                ImageButton btnPause = (ImageButton) mRootView.findViewById(R.id.btnPause);
                ProgressBar progressBar = (ProgressBar) mRootView.findViewById(R.id.progressBar);
                btnPlay.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);

                if(resetSeekbar) {
                    resetSeekBar();
                }
            }
        }

        private void play() {
            if(mMediaPlayer != null) {
                onPrepared(mMediaPlayer);
            } else {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    mMediaPlayer.setDataSource(mAudioUrl);
                } catch (Throwable e) {
                    mTrackerManager.logException(e);
                }

                mMediaPlayer.setOnBufferingUpdateListener(this);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setOnPreparedListener(this);
                mMediaPlayer.setOnErrorListener(this);

                setVisibility(View.GONE, View.GONE, View.VISIBLE);

                mMediaPlayer.prepareAsync();
            }
        }

        @Override
        public void onClick(View v) {
            if(mPlayingController != null) {
                mPlayingController.pause(false);
                // release resources to avoid memory consumption
                if(!mPlayingController.equals(this)) {
                    mPlayingController.release();
                }
            }

            if(v.getId() == R.id.btnPlay) {
                mPlayingController = this;
                play();
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mMediaPlayer != null) {
                int ms = Math.round(((float) seekBar.getProgress() / 100f) * (float) mMediaPlayer.getDuration());
                mMediaPlayer.seekTo(ms);
            }
            mProgress = seekBar.getProgress();
            updateProgress();
        }

        private void updateProgress() {
            long ms = 0;

            if(mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                ms = mMediaPlayer.getCurrentPosition();
                mProgress = Math.round((float) ms / (float) mMediaPlayer.getDuration() * 100f);
            } else {
                ms = Math.round(((float) mProgress / 100f) * (float) mDuration);
            }

            if(mRootView != null) {
                CustomFontTextView txtDuration = (CustomFontTextView) mRootView.findViewById(R.id.txtDuration);
                SeekBar seekBar = (SeekBar) mRootView.findViewById(R.id.seekBar);
                txtDuration.setText(Utils.getFriendlyDuration(ms));
                seekBar.setProgress((int) mProgress);
            }
        }

        public View getRootView() {
            return mRootView;
        }

        public void setRootView(View mRootView) {
            this.mRootView = mRootView;

            if(mRootView != null) {
                updateProgress();
                mRootView.findViewById(R.id.btnPlay).setOnClickListener(this);
                mRootView.findViewById(R.id.btnPause).setOnClickListener(this);
                SeekBar seekBar = (SeekBar) mRootView.findViewById(R.id.seekBar);
                seekBar.setOnSeekBarChangeListener(this);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            pause(true);
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            if(mMediaPlayer != null) {
                setVisibility(View.GONE, View.VISIBLE, View.GONE);
                mMediaPlayer.start();
                scheduleProgressMonitoring();
            }
        }

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if(mRootView != null) {
                ProgressBar progressBar = (ProgressBar) mRootView.findViewById(R.id.progressBar);
                progressBar.setProgress(percent);
            }
        }

        private void resetSeekBar() {
            if(mRootView != null) {
                mProgress = 0;
                SeekBar seekBar = (SeekBar) mRootView.findViewById(R.id.seekBar);
                seekBar.setProgress(0);
                CustomFontTextView txtDuration = (CustomFontTextView) mRootView.findViewById(R.id.txtDuration);
                txtDuration.setText(Utils.getFriendlyDuration(mDuration));
            }
        }

        private void release() {
            resetSeekBar();

            if(mMediaPlayer != null) {
                if(mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            // release some memory in case of lots of messages
            if(mRootView == null) {
                mControllers.remove(mMessageId);
            }
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            pause(true);
            release();
            return false;
        }
    }

    private static final int MARGIN_BALLOON_DP = 80;

    private Context mContext;
    private SQLiteDatabase mDb;
    private TrackerManager mTrackerManager;

    private int mBalloonMargin;

    public MessageCursorAdapter(Context context, Cursor cursor, SQLiteDatabase db, TrackerManager trackerManager) {
        super(context, cursor, 0);
        this.mDb = db;
        this.mContext = context;
        this.mTrackerManager = trackerManager;
        this.mBalloonMargin = Utils.dpToPx(context, MARGIN_BALLOON_DP);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(mContext).inflate(R.layout.i_chat_message_layout, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Message message = getMessage(cursor);
        Message prevMessage = null;
        if(cursor.moveToPrevious()) {
            prevMessage = getMessage(cursor);
            cursor.moveToNext();
        }

        RelativeLayout lytBalloon = (RelativeLayout) view.findViewById(R.id.lytBalloon);
        if(message.isReceived()) {
            lytBalloon.setBackgroundResource(R.drawable.img_message_base_received);
            ((LinearLayout.LayoutParams) lytBalloon.getLayoutParams()).setMargins(0, 0, mBalloonMargin, 0);
        } else {
            lytBalloon.setBackgroundResource(R.drawable.img_message_base_sent);
            ((LinearLayout.LayoutParams) lytBalloon.getLayoutParams()).setMargins(mBalloonMargin, 0, 0, 0);
        }

        CustomFontTextView txtTime = (CustomFontTextView) view.findViewById(R.id.txtTime);
        try {
            DateContainer curTime = new DateContainer(DateContainer.TYPE_TIME, message.getRegistrationTimestamp().substring(11));
            txtTime.setText(curTime.getAsString());
        } catch(ParseException e) {
            txtTime.setText(message.getRegistrationTimestamp());
            mTrackerManager.logException(e);
        }

        CustomFontTextView txtDuration = (CustomFontTextView) view.findViewById(R.id.txtDuration);
        if(message.getRecording() != null) {
            txtDuration.setText(Utils.getFriendlyDuration(message.getRecording().getDurationMillis()));
        } else {
            txtDuration.setText("");
        }

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        seekBar.setProgress(0);

        CustomFontTextView txtDay = (CustomFontTextView) view.findViewById(R.id.txtDay);
        txtDay.setVisibility(View.VISIBLE);
        try {
            DateContainer curDate = new DateContainer(DateContainer.TYPE_DATE, message.getRegistrationTimestamp().substring(0, 10));
            txtDay.setText(curDate.getAsString());

            if(prevMessage != null) {
                DateContainer prevDate = new DateContainer(DateContainer.TYPE_DATE, prevMessage.getRegistrationTimestamp().substring(0, 10));
                if(prevDate.equals(curDate)) {
                    txtDay.setVisibility(View.GONE);
                }
            }
        } catch(ParseException e) {
            txtDay.setVisibility(View.GONE);
            mTrackerManager.logException(e);
        }

        if(view.getTag() != null) {
            MessageController prevController = mControllers.get(view.getTag());
            if(prevController != null) {
                prevController.setRootView(null);
            }
        }

        view.setTag(message.getId());
        MessageController controller = mControllers.get(message.getId());
        if(controller == null) {
            controller = new MessageController(message.getId(), message.getRecording().getFilePath() != null ? message.getRecording().getFilePath() : message.getServerCanonicalUrl(), message.getRecording().getDurationMillis());
            mControllers.put(message.getId(), controller);
        }
        controller.setRootView(view);
    }

    public Message getMessage(Cursor cursor) {
        return Message.getFromCursor(mDb, cursor);
    }

    public SQLiteDatabase getDatabase() {
        return mDb;
    }

    public void setDatabase(SQLiteDatabase mDb) {
        this.mDb = mDb;
    }

    public void destroy() {
        if(mPlayingController != null) {
            mPlayingController.pause(true);
            mPlayingController.release();
        }
        mControllers.clear();
    }
}
