package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.PlayerEvent;
import com.peppermint.app.PlayerServiceManager;
import com.peppermint.app.R;
import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.cloud.senders.SenderEvent;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.ui.views.simple.CustomFontButton;
import com.peppermint.app.ui.views.simple.CustomFontTextView;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import org.joda.time.DateTimeZone;

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

    private MessageController mPlayingController;
    private Map<Long, MessageController> mControllers = new HashMap<>();

    private class MessageController implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, PlayerServiceManager.PlayerListener {

        private Message mMessage;
        private View mRootView;

        private MessageController(Message mMessage) {
            this.mMessage = mMessage;
            mPlayerServiceManager.addPlayerListener(this);
        }

        private void pause(boolean resetSeekbar) {
            setVisibility(View.VISIBLE, View.GONE, View.GONE);
            mPlayerServiceManager.pause(mMessage, resetSeekbar);
        }

        private void play() {
            setVisibility(View.GONE, View.GONE, View.VISIBLE);
            int startPercent = 0;
            if(mRootView != null) {
                SeekBar seekBar = (SeekBar) this.mRootView.findViewById(R.id.seekBar);
                startPercent = seekBar.getProgress();
            }
            mPlayerServiceManager.play(mMessage, startPercent);
        }

        @Override
        public void onClick(View v) {
            // play/pause controls
            if(v.getId() == R.id.btnPlay || v.getId() == R.id.btnPause) {
                if (mPlayingController != null) {
                    mPlayingController.pause(false);
                    // release resources to avoid memory consumption
                    if (!mPlayingController.equals(this)) {
                        mPlayingController.destroy();
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
                mMessagesServiceManager.cancel(mMessage);
                return;
            }

            if(v.getId() == R.id.btnExclamation) {
                if(mExclamationClickListener != null) {
                    mExclamationClickListener.onClick(v, mMessage.getId());
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
            mPlayerServiceManager.setPosition(mMessage, seekBar.getProgress());
        }

        public void setRootView(View mRootView, Message message, Message prevMessage) {
            if(mRootView == null && this.mRootView != null) {
                ImageButton btnPlay = (ImageButton) this.mRootView.findViewById(R.id.btnPlay);
                ImageButton btnPause = (ImageButton) this.mRootView.findViewById(R.id.btnPause);
                SeekBar seekBar = (SeekBar) this.mRootView.findViewById(R.id.seekBar);

                CustomFontButton btnCancel = (CustomFontButton) this.mRootView.findViewById(R.id.btnCancel);
                ImageView btnExclamation = (ImageView) this.mRootView.findViewById(R.id.btnExclamation);

                // listeners setup
                btnPlay.setOnClickListener(null);
                btnPause.setOnClickListener(null);
                seekBar.setOnSeekBarChangeListener(null);

                btnCancel.setOnClickListener(null);
                btnExclamation.setOnClickListener(null);
            }

            this.mRootView = mRootView;

            if(mRootView != null) {
                FrameLayout lytBalloon = (FrameLayout) mRootView.findViewById(R.id.lytBalloon);

                ImageButton btnPlay = (ImageButton) mRootView.findViewById(R.id.btnPlay);
                ImageButton btnPause = (ImageButton) mRootView.findViewById(R.id.btnPause);
                CustomFontTextView txtTime = (CustomFontTextView) mRootView.findViewById(R.id.txtTime);
                CustomFontTextView txtDuration = (CustomFontTextView) mRootView.findViewById(R.id.txtDuration);
                CustomFontTextView txtDay = (CustomFontTextView) mRootView.findViewById(R.id.txtDay);
                SeekBar seekBar = (SeekBar) mRootView.findViewById(R.id.seekBar);

                CustomFontButton btnCancel = (CustomFontButton) mRootView.findViewById(R.id.btnCancel);
                ImageView btnExclamation = (ImageView) mRootView.findViewById(R.id.btnExclamation);

                // play/pause buttons
                if(mPlayerServiceManager.isPlaying(mMessage)) {
                    setVisibility(View.GONE, View.VISIBLE, View.GONE);
                } else {
                    setVisibility(View.VISIBLE, View.GONE, View.GONE);
                }

                // time received/sent
                try {
                    DateContainer curTime = new DateContainer(DateContainer.TYPE_TIME, message.getRegistrationTimestamp().substring(11));
                    txtTime.setText(curTime.getAsString(DateContainer.FRIENDLY_AMPM_TIME_FORMAT, DateTimeZone.getDefault()).replace(".", ""));
                } catch(ParseException e) {
                    txtTime.setText(message.getRegistrationTimestamp());
                    mTrackerManager.logException(e);
                }

                // total duration of the message / current position of the playing message
                if(message.getRecording() != null) {
                    txtDuration.setText(Utils.getFriendlyDuration(message.getRecording().getDurationMillis()));
                } else {
                    txtDuration.setText("");
                }

                // day of the message separator
                txtDay.setVisibility(View.VISIBLE);
                try {
                    DateContainer curDate = new DateContainer(DateContainer.TYPE_DATE, message.getRegistrationTimestamp().substring(0, 10));

                    if(prevMessage != null) {
                        DateContainer prevDate = new DateContainer(DateContainer.TYPE_DATE, prevMessage.getRegistrationTimestamp().substring(0, 10));
                        if(prevDate.equals(curDate)) {
                            txtDay.setVisibility(View.GONE);
                        }
                    }

                    if(txtDay.getVisibility() == View.VISIBLE) {
                        String dayLabel = DateContainer.getRelativeLabelToToday(mContext, curDate, DateTimeZone.getDefault());
                        txtDay.setText(dayLabel);
                    }
                } catch(ParseException e) {
                    txtDay.setVisibility(View.GONE);
                    mTrackerManager.logException(e);
                }

                // seekbar
                seekBar.setProgress(0);

                // distinctions between received/sent message
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

                // listeners setup
                btnPlay.setOnClickListener(this);
                btnPause.setOnClickListener(this);
                seekBar.setOnSeekBarChangeListener(this);

                btnCancel.setOnClickListener(this);
                btnExclamation.setOnClickListener(this);
            }
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

        private void destroy() {
            // release some memory in case of lots of messages
            if(mRootView == null) {
                mPlayerServiceManager.removePlayerListener(this);
                mControllers.remove(mMessage.getId());
            } else {
                SeekBar seekBar = (SeekBar) mRootView.findViewById(R.id.seekBar);
                seekBar.setProgress(0);
                CustomFontTextView txtDuration = (CustomFontTextView) mRootView.findViewById(R.id.txtDuration);
                txtDuration.setText(Utils.getFriendlyDuration(mMessage.getRecording().getDurationMillis()));
            }
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

        @Override
        public void onPlayerEvent(PlayerEvent event) {
            if(!event.getMessage().equals(mMessage)) {
                return;
            }

            switch(event.getType()) {
                case PlayerEvent.EVENT_STARTED:
                    setVisibility(View.GONE, View.VISIBLE, View.GONE);
                    break;
                case PlayerEvent.EVENT_BUFFERING_UPDATE:
                    if(mRootView != null) {
                        ProgressBar progressBar = (ProgressBar) mRootView.findViewById(R.id.progressBar);
                        progressBar.setProgress(event.getPercent());
                    }
                    break;
                case PlayerEvent.EVENT_PROGRESS:
                    if(mRootView != null) {
                        CustomFontTextView txtDuration = (CustomFontTextView) mRootView.findViewById(R.id.txtDuration);
                        SeekBar seekBar = (SeekBar) mRootView.findViewById(R.id.seekBar);
                        txtDuration.setText(Utils.getFriendlyDuration(event.getCurrentMs()));
                        seekBar.setProgress(event.getPercent());
                    }
                    break;
                case PlayerEvent.EVENT_COMPLETED:
                    pause(true);
                    if(mRootView != null) {
                        SeekBar seekBar = (SeekBar) mRootView.findViewById(R.id.seekBar);
                        seekBar.setProgress(0);
                    }
                    break;
                case PlayerEvent.EVENT_ERROR:
                    pause(true);
                    destroy();
                    break;
            }
        }
    }

    private static final int MARGIN_BALLOON_DP = 80;

    private Context mContext;
    private SQLiteDatabase mDb;
    private TrackerManager mTrackerManager;
    private MessagesServiceManager mMessagesServiceManager;
    private PlayerServiceManager mPlayerServiceManager;
    private ExclamationClickListener mExclamationClickListener;

    private int mBalloonMargin;

    public MessageCursorAdapter(Context context, MessagesServiceManager messagesServiceManager, PlayerServiceManager mPlayerServiceManager, Cursor cursor, SQLiteDatabase db, TrackerManager trackerManager) {
        super(context, cursor, 0);
        this.mDb = db;
        this.mContext = context;
        this.mTrackerManager = trackerManager;
        this.mMessagesServiceManager = messagesServiceManager;
        this.mPlayerServiceManager = mPlayerServiceManager;
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

        mMessagesServiceManager.markAsPlayed(message);

        if(view.getTag() != null) {
            MessageController prevController = mControllers.get(view.getTag());
            if(prevController != null) {
                prevController.setRootView(null, message, prevMessage);
            }
        }

        view.setTag(message.getId());
        MessageController controller = mControllers.get(message.getId());
        if(controller == null) {
            controller = new MessageController(message);
            mControllers.put(message.getId(), controller);
        }
        controller.setRootView(view, message, prevMessage);
    }

    public Message getMessage(Cursor cursor) {
        return Message.getFromCursor(mDb, cursor);
    }

    public Message getMessage(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getMessage(cursor);
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
            mPlayingController.destroy();
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
