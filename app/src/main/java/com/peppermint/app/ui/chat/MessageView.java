package com.peppermint.app.ui.chat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.peppermint.app.PlayerServiceManager;
import com.peppermint.app.R;
import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recording;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.PlayerEvent;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.base.views.CustomFontButton;
import com.peppermint.app.ui.base.views.CustomFontTextView;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.Utils;

import java.text.ParseException;
import java.util.TimeZone;

/**
 * Created by Nuno Luz on 26-05-2016.
 *
 * A message balloon in the UI. Automatically updates based on player/sender message events.<br />
 * {@link #setMessage(Message, Message)} should be invoked with null parameters to stop this view
 * from listening to further events.
 */
public class MessageView extends LinearLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    public interface ExclamationClickListener {
        void onClick(View v, long messageId);
    }

    private static final int MARGIN_BALLOON_DP = 100;
    private static final int MARGIN_BALLOON_WITH_EXCLAMATION_DP = 20;

    // buttons
    private CustomFontButton mBtnCancel;
    private ImageButton mBtnPlay, mBtnPause;
    private ImageButton mBtnExclamation;

    // text views
    private CustomFontTextView mTxtDuration, mTxtTime, mTxtDay;
    private CustomFontTextView mTxtAutomaticTranscriptionTitle, mTxtTranscription;

    private View mLytTranscriptionBorder;
    private SeekBar mSeekBar;
    private ProgressBar mProgressBar;

    // balloon container
    private ViewGroup mLytBalloon;

    private PlayerServiceManager mSharedPlayerServiceManager;
    private MessagesServiceManager mSharedMessageServiceManager;
    private Message mMessage, mPreviousMessage;

    private int mBalloonMargin, mBalloonMarginWithExclamation;

    private ExclamationClickListener mExclamationClickListener;

    public MessageView(Context context) {
        super(context);
        init(context, null);
    }

    public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MessageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MessageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);

        mBalloonMargin = Utils.dpToPx(context, MARGIN_BALLOON_DP);
        mBalloonMarginWithExclamation = Utils.dpToPx(context, MARGIN_BALLOON_WITH_EXCLAMATION_DP);

        final int dp10 = Utils.dpToPx(context, 10);
        setPadding(dp10, dp10, dp10, dp10);

        mTxtDay = new CustomFontTextView(context);
        mTxtDay.setGravity(Gravity.CENTER);
        mTxtDay.setTypeface(R.string.font_bold);
        mTxtDay.setAllCaps(true);
        mTxtDay.setTextColor(ResourceUtils.getColor(context, R.color.dark_grey_text));
        mTxtDay.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

        LayoutParams txtDayLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        txtDayLayoutParams.setMargins(0, Utils.dpToPx(context, 15), 0, dp10);
        addView(mTxtDay, txtDayLayoutParams);

        final ViewGroup rootLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.i_chat_message_layout, this, true);

        mLytBalloon = (ViewGroup) rootLayout.findViewById(R.id.lytBalloon);

        mBtnPlay = (ImageButton) rootLayout.findViewById(R.id.btnPlay);
        mBtnPause = (ImageButton) rootLayout.findViewById(R.id.btnPause);
        mBtnExclamation = (ImageButton) rootLayout.findViewById(R.id.btnExclamation);
        mBtnCancel = (CustomFontButton) rootLayout.findViewById(R.id.btnCancel);

        mTxtTime = (CustomFontTextView) rootLayout.findViewById(R.id.txtTime);
        mTxtDuration = (CustomFontTextView) rootLayout.findViewById(R.id.txtDuration);
        mSeekBar = (SeekBar) rootLayout.findViewById(R.id.seekBar);
        mProgressBar = (ProgressBar) rootLayout.findViewById(R.id.progressBar);

        mLytTranscriptionBorder = rootLayout.findViewById(R.id.lytTranscriptionBorder);
        mTxtAutomaticTranscriptionTitle = (CustomFontTextView) rootLayout.findViewById(R.id.txtAutomaticTranscriptionTitle);
        mTxtTranscription = (CustomFontTextView) rootLayout.findViewById(R.id.txtTranscription);

        // listeners setup
        mBtnPlay.setOnClickListener(this);
        mBtnPause.setOnClickListener(this);
        mBtnExclamation.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);

        mSeekBar.setOnSeekBarChangeListener(this);
    }

    protected void refreshData() {
        if(mMessage == null) {
            return;
        }

        final Recording recording = mMessage.getRecordingParameter();

        // transcription
        if(recording != null && recording.getTranscription() != null) {
            mLytTranscriptionBorder.setVisibility(View.VISIBLE);
            mTxtAutomaticTranscriptionTitle.setVisibility(View.VISIBLE);
            mTxtTranscription.setVisibility(View.VISIBLE);
            mTxtTranscription.setText(recording.getTranscription());
        } else {
            mLytTranscriptionBorder.setVisibility(View.GONE);
            mTxtAutomaticTranscriptionTitle.setVisibility(View.GONE);
            mTxtTranscription.setVisibility(View.GONE);
            mTxtTranscription.setText("");
        }

        // play/pause buttons
        if(hasWorkingSharedPlayerServiceManager() && mSharedPlayerServiceManager.isPlaying(mMessage)) {
            setPlayerOngoing();
        } else if(hasWorkingSharedPlayerServiceManager() && mSharedPlayerServiceManager.isLoading(mMessage)) {
            setPlayerLoading();
        } else {
            setPlayerStopped();
        }

        // time received/sent
        try {
            DateContainer messageTime = new DateContainer(DateContainer.TYPE_TIME, mMessage.getRegistrationTimestamp());
            mTxtTime.setText(messageTime.getAsFriendlyTime(getContext(), TimeZone.getDefault()));
        } catch(ParseException e) {
            mTxtTime.setText(mMessage.getRegistrationTimestamp());
            TrackerManager.getInstance(getContext()).logException(e);
        }

        // total duration of the message / current position of the playing message
        if(recording != null) {
            mTxtDuration.setText(Utils.getFriendlyDuration(recording.getDurationMillis()));
        } else {
            mTxtDuration.setText("");
        }

        // day of the message separator
        mTxtDay.setVisibility(View.VISIBLE);
        try {
            DateContainer messageDate = new DateContainer(DateContainer.TYPE_DATE, mMessage.getRegistrationTimestamp());

            if(mPreviousMessage != null) {
                DateContainer prevMessageDate = new DateContainer(DateContainer.TYPE_DATE, mPreviousMessage.getRegistrationTimestamp());
                if(prevMessageDate.equals(messageDate, TimeZone.getDefault())) {
                    mTxtDay.setVisibility(View.GONE);
                }
            }

            if(mTxtDay.getVisibility() == View.VISIBLE) {
                String dayLabel = DateContainer.getRelativeLabelToToday(getContext(), messageDate, TimeZone.getDefault());
                mTxtDay.setText(dayLabel);
            }
        } catch(ParseException e) {
            mTxtDay.setVisibility(View.GONE);
            TrackerManager.getInstance(getContext()).logException(e);
        }

        mSeekBar.setProgress(0);

        // distinctions between received/sent message
        if(mMessage.isReceived()) {
            mBtnCancel.setVisibility(View.GONE);
            mBtnExclamation.setVisibility(View.GONE);

            mLytBalloon.setBackgroundResource(R.drawable.img_message_base_received);
            ((RelativeLayout.LayoutParams) mLytBalloon.getLayoutParams()).setMargins(0, 0, mBalloonMargin, 0);
        } else {
            mLytBalloon.setBackgroundResource(R.drawable.img_message_base_sent);
            ((RelativeLayout.LayoutParams) mLytBalloon.getLayoutParams()).setMargins(mBalloonMargin, 0, 0, 0);

            if(hasWorkingSharedMessageServiceManager() && mSharedMessageServiceManager.isSending(mMessage)) {
                setStatusSending(mSharedMessageServiceManager.isSendingAndCancellable(mMessage));
            } else if(!hasWorkingSharedMessageServiceManager() || mMessage.isSent()) {
                setStatusNormal();
            } else {
                setStatusError();
                ((RelativeLayout.LayoutParams) mLytBalloon.getLayoutParams()).setMargins(mBalloonMarginWithExclamation, 0, 0, 0);
            }
        }
    }

    private void setPlayerOngoing() {
        mBtnPlay.setVisibility(GONE);
        mBtnPause.setVisibility(VISIBLE);
        mProgressBar.setVisibility(GONE);
    }

    private void setPlayerLoading() {
        mBtnPlay.setVisibility(GONE);
        mBtnPause.setVisibility(GONE);
        mProgressBar.setVisibility(VISIBLE);
    }

    private void setPlayerStopped() {
        mBtnPlay.setVisibility(VISIBLE);
        mBtnPause.setVisibility(GONE);
        mProgressBar.setVisibility(GONE);
    }

    private void setStatusSending(boolean isCancellable) {
        ((RelativeLayout.LayoutParams) mLytBalloon.getLayoutParams()).setMargins(mBalloonMargin, 0, 0, 0);
        if(isCancellable) {
            mBtnCancel.setText(R.string.cancel_sending);
            mBtnCancel.setEnabled(true);
        } else {
            mBtnCancel.setText(R.string.sending);
            mBtnCancel.setEnabled(false);
        }
        mBtnCancel.setVisibility(View.VISIBLE);
        mBtnExclamation.setVisibility(View.GONE);
    }

    private void setStatusNormal() {
        ((RelativeLayout.LayoutParams) mLytBalloon.getLayoutParams()).setMargins(mBalloonMargin, 0, 0, 0);
        mBtnCancel.setVisibility(View.GONE);
        mBtnExclamation.setVisibility(View.GONE);
    }

    private void setStatusError() {
        ((RelativeLayout.LayoutParams) mLytBalloon.getLayoutParams()).setMargins(mBalloonMarginWithExclamation, 0, 0, 0);
        mBtnCancel.setVisibility(View.GONE);
        mBtnExclamation.setVisibility(View.VISIBLE);
    }

    public void onEventMainThread(PlayerEvent event) {
        if(!event.getMessage().equals(mMessage)) {
            return;
        }

        switch(event.getType()) {
            case PlayerEvent.EVENT_STARTED:
                setPlayerOngoing();
                break;
            case PlayerEvent.EVENT_BUFFERING_UPDATE:
                mProgressBar.setProgress(event.getPercent());
                break;
            case PlayerEvent.EVENT_PROGRESS:
                mTxtDuration.setText(Utils.getFriendlyDuration(event.getCurrentMs()));
                mSeekBar.setProgress(event.getPercent());
                break;
            case PlayerEvent.EVENT_COMPLETED:
            case PlayerEvent.EVENT_STOPPED:
                pause(true);
                mSeekBar.setProgress(0);
                break;
            case PlayerEvent.EVENT_ERROR:
                if(event.getErrorCode() == PlayerEvent.ERROR_NO_CONNECTIVITY) {
                    Toast.makeText(getContext(), R.string.msg_no_internet_try_again, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), R.string.msg_unable_to_play, Toast.LENGTH_LONG).show();
                }
                pause(true);
                mSeekBar.setProgress(0);
                mTxtDuration.setText(Utils.getFriendlyDuration(mMessage.getRecordingParameter().getDurationMillis()));
                break;
        }
    }

    private void pause(boolean resetSeekbar) {
        if(hasWorkingSharedPlayerServiceManager()) {
            setPlayerStopped();
            mSharedPlayerServiceManager.pause(mMessage, resetSeekbar);
        }
    }

    private void play() {
        if(hasWorkingSharedPlayerServiceManager()) {
            setPlayerLoading();

            int startPercent = mSeekBar.getProgress();

            if (mMessage.isReceived() && hasWorkingSharedMessageServiceManager()) {
                mSharedMessageServiceManager.markAsPlayed(mMessage);
            }

            mSharedPlayerServiceManager.play(mMessage, startPercent);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        setMessage(null, null);
        super.onDetachedFromWindow();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnPlay:
                play();
                break;
            case R.id.btnPause:
                pause(false);
                break;
            case R.id.btnCancel:
                if(hasWorkingSharedMessageServiceManager()) {
                    mSharedMessageServiceManager.cancel(mMessage);
                    mBtnCancel.setText(R.string.cancelling);
                }
                break;
            case R.id.btnExclamation:
                if(mExclamationClickListener != null && mMessage != null) {
                    mExclamationClickListener.onClick(v, mMessage.getId());
                }
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        /* nothing to do here */
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        /* nothing to do here */
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(hasWorkingSharedPlayerServiceManager()) {
            mSharedPlayerServiceManager.setPosition(mMessage, seekBar.getProgress());
        }
    }

    public Message getMessage() {
        return mMessage;
    }

    public void setMessage(Message mMessage, Message mPreviousMessage) {
        if(mMessage != null) {
            if(this.mMessage == null) {
                PeppermintEventBus.registerAudio(this);
            }
        } else {
            if(this.mMessage != null) {
                PeppermintEventBus.unregisterAudio(this);
            }
        }

        this.mMessage = mMessage;
        this.mPreviousMessage = mPreviousMessage;

        refreshData();
    }

    protected boolean hasWorkingSharedPlayerServiceManager() {
        return mSharedPlayerServiceManager != null && mSharedPlayerServiceManager.isBound();
    }

    public void setSharedPlayerServiceManager(PlayerServiceManager mSharedPlayerServiceManager) {
        this.mSharedPlayerServiceManager = mSharedPlayerServiceManager;
    }

    protected boolean hasWorkingSharedMessageServiceManager() {
        return mSharedMessageServiceManager != null && mSharedMessageServiceManager.isBound();
    }

    public void setSharedMessageServiceManager(MessagesServiceManager mSharedMessageServiceManager) {
        this.mSharedMessageServiceManager = mSharedMessageServiceManager;
    }

    public void setOnExclamationClickListener(ExclamationClickListener mExclamationClickListener) {
        this.mExclamationClickListener = mExclamationClickListener;
    }
}
