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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.peppermint.app.MessagesServiceManager;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.sending.SenderEvent;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.ui.views.simple.CustomFontButton;
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
public class MessageCursorAdapter extends CursorAdapter implements MessagesServiceManager.SenderListener {

    public interface ExclamationClickListener {
        void onClick(View v, long messageId);
    }

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
            // play/pause controls
            if(v.getId() == R.id.btnPlay || v.getId() == R.id.btnPause) {
                if (mPlayingController != null) {
                    mPlayingController.pause(false);
                    // release resources to avoid memory consumption
                    if (!mPlayingController.equals(this)) {
                        mPlayingController.release();
                    }
                }

                if (v.getId() == R.id.btnPlay) {
                    mPlayingController = this;
                    play();
                }

                return;
            }

            // cancel control
            if(v.getId() == R.id.btnCancel) {
                Message msg = new Message();
                msg.setId(mMessageId);
                mMessagesServiceManager.cancel(msg);
                return;
            }

            if(v.getId() == R.id.btnExclamation) {
                if(mExclamationClickListener != null) {
                    mExclamationClickListener.onClick(v, mMessageId);
                }
                return;
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

        public void setRootView(View mRootView, Message message, Message prevMessage) {
            this.mRootView = mRootView;

            if(mRootView != null) {
                // init view
                updateProgress();

                // play/pause controls
                mRootView.findViewById(R.id.btnPlay).setOnClickListener(this);
                mRootView.findViewById(R.id.btnPause).setOnClickListener(this);

                // time received/sent
                CustomFontTextView txtTime = (CustomFontTextView) mRootView.findViewById(R.id.txtTime);
                try {
                    DateContainer curTime = new DateContainer(DateContainer.TYPE_TIME, message.getRegistrationTimestamp().substring(11));
                    txtTime.setText(curTime.getAsString());
                } catch(ParseException e) {
                    txtTime.setText(message.getRegistrationTimestamp());
                    mTrackerManager.logException(e);
                }

                // total duration of the message / current position of the playing message
                CustomFontTextView txtDuration = (CustomFontTextView) mRootView.findViewById(R.id.txtDuration);
                if(message.getRecording() != null) {
                    txtDuration.setText(Utils.getFriendlyDuration(message.getRecording().getDurationMillis()));
                } else {
                    txtDuration.setText("");
                }

                // day of the message separator
                CustomFontTextView txtDay = (CustomFontTextView) mRootView.findViewById(R.id.txtDay);
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

                // seekbar
                SeekBar seekBar = (SeekBar) mRootView.findViewById(R.id.seekBar);
                seekBar.setOnSeekBarChangeListener(this);
                seekBar.setProgress(0);

                // distinctions between received/sent message
                CustomFontButton btnCancel = (CustomFontButton) mRootView.findViewById(R.id.btnCancel);
                ImageView btnExclamation = (ImageView) mRootView.findViewById(R.id.btnExclamation);
                RelativeLayout lytBalloon = (RelativeLayout) mRootView.findViewById(R.id.lytBalloon);

                btnCancel.setOnClickListener(this);
                btnExclamation.setOnClickListener(this);

                if(message.isReceived()) {
                    btnCancel.setVisibility(View.GONE);
                    btnExclamation.setVisibility(View.GONE);

                    lytBalloon.setBackgroundResource(R.drawable.img_message_base_received);
                    ((RelativeLayout.LayoutParams) lytBalloon.getLayoutParams()).setMargins(0, 0, mBalloonMargin, 0);
                } else {
                    lytBalloon.setBackgroundResource(R.drawable.img_message_base_sent);
                    ((RelativeLayout.LayoutParams) lytBalloon.getLayoutParams()).setMargins(mBalloonMargin, 0, 0, 0);

                    if(mMessagesServiceManager.isSending(message)) {
                        setStatusSending();
                    } else if(message.isSent()) {
                        setStatusNormal();
                    } else {
                        setStatusError();
                    }
                }
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

        public void setStatusSending() {
            if(mRootView != null) {
                CustomFontButton btnCancel = (CustomFontButton) mRootView.findViewById(R.id.btnCancel);
                ImageView btnExclamation = (ImageView) mRootView.findViewById(R.id.btnExclamation);
                btnCancel.setVisibility(View.VISIBLE);
                btnExclamation.setVisibility(View.GONE);
            }
        }

        public void setStatusNormal() {
            if(mRootView != null) {
                CustomFontButton btnCancel = (CustomFontButton) mRootView.findViewById(R.id.btnCancel);
                ImageView btnExclamation = (ImageView) mRootView.findViewById(R.id.btnExclamation);
                btnCancel.setVisibility(View.GONE);
                btnExclamation.setVisibility(View.GONE);
            }
        }

        public void setStatusError() {
            if(mRootView != null) {
                CustomFontButton btnCancel = (CustomFontButton) mRootView.findViewById(R.id.btnCancel);
                ImageView btnExclamation = (ImageView) mRootView.findViewById(R.id.btnExclamation);
                btnCancel.setVisibility(View.GONE);
                btnExclamation.setVisibility(View.VISIBLE);
            }
        }
    }

    private static final int MARGIN_BALLOON_DP = 80;

    private Context mContext;
    private SQLiteDatabase mDb;
    private TrackerManager mTrackerManager;
    private MessagesServiceManager mMessagesServiceManager;
    private ExclamationClickListener mExclamationClickListener;

    private int mBalloonMargin;

    public MessageCursorAdapter(Context context, MessagesServiceManager messagesServiceManager, Cursor cursor, SQLiteDatabase db, TrackerManager trackerManager) {
        super(context, cursor, 0);
        this.mDb = db;
        this.mContext = context;
        this.mTrackerManager = trackerManager;
        this.mMessagesServiceManager = messagesServiceManager;
        this.mBalloonMargin = Utils.dpToPx(context, MARGIN_BALLOON_DP);

        mMessagesServiceManager.addSenderListener(this);
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

        if(view.getTag() != null) {
            MessageController prevController = mControllers.get(view.getTag());
            if(prevController != null) {
                prevController.setRootView(null, message, prevMessage);
            }
        }

        view.setTag(message.getId());
        MessageController controller = mControllers.get(message.getId());
        if(controller == null) {
            controller = new MessageController(message.getId(), message.getRecording().getFilePath() != null ? message.getRecording().getFilePath() : message.getServerCanonicalUrl(), message.getRecording().getDurationMillis());
            mControllers.put(message.getId(), controller);
        }
        controller.setRootView(view, message, prevMessage);
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

        mMessagesServiceManager.removeSenderListener(this);

        if(mPlayingController != null) {
            mPlayingController.pause(true);
            mPlayingController.release();
        }
        mControllers.clear();
    }

    @Override
    public void onSendStarted(SenderEvent event) {
        MessageController controller = mControllers.get(event.getSenderTask().getMessage().getId());
        if(controller != null) {
            controller.setStatusSending();
        }
    }

    @Override
    public void onSendCancelled(SenderEvent event) {
    }

    @Override
    public void onSendError(SenderEvent event) {
        MessageController controller = mControllers.get(event.getSenderTask().getMessage().getId());
        if(controller != null) {
            controller.setStatusError();
        }
    }

    @Override
    public void onSendFinished(SenderEvent event) {
        MessageController controller = mControllers.get(event.getSenderTask().getMessage().getId());
        if(controller != null) {
            controller.setStatusNormal();
        }
    }

    @Override
    public void onSendProgress(SenderEvent event) {

    }

    @Override
    public void onSendQueued(SenderEvent event) {
        MessageController controller = mControllers.get(event.getSenderTask().getMessage().getId());
        if(controller != null) {
            controller.setStatusError();
        }
    }

    public void stopAllPlayers() {
        if(mPlayingController != null) {
            mPlayingController.pause(false);
        }
    }

    public ExclamationClickListener getExclamationClickListener() {
        return mExclamationClickListener;
    }

    public void setExclamationClickListener(ExclamationClickListener mExclamationClickListener) {
        this.mExclamationClickListener = mExclamationClickListener;
    }
}
