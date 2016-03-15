package com.peppermint.app.ui.chat.recorder;

import android.graphics.Rect;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.RecordService;
import com.peppermint.app.RecordServiceManager;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.Recording;
import com.peppermint.app.ui.Overlay;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.TouchInterceptable;
import com.peppermint.app.ui.views.simple.CustomToast;
import com.peppermint.app.utils.NoMicDataIOException;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 06-02-2016.
 */
public class ChatRecordOverlay extends Overlay implements RecordServiceManager.Listener, View.OnTouchListener, OverlayManager.OverlayVisibilityChangeListener {

    public interface OnRecordingFinishedCallback {
        void onRecordingFinished(RecordService.Event event);
    }

    public static final String SCREEN_RECORDING_ID = "Recording";

    private static final int RECORDING_OVERLAY_HIDE_DELAY = 1000;

    private static final long MIN_DURATION_MILLIS = 2000;
    private static final long MAX_DURATION_MILLIS = 300000; // 5min

    private static final int MIN_SWIPE_DISTANCE_DP = 90;        // min swipe distance
    private static final int MAX_SWIPE_DURATION = 300;        // max swipe duration

    // contextual/external instances
    private TouchInterceptable mTouchInterceptable;
    private RecordServiceManager mRecordServiceManager;

    // actual View of the overlay
    private ChatRecordOverlayView mView;
    private View mViewBounds;

    // start recording pop sound player
    private MediaPlayer mRecordSoundPlayer;

    // state
    private boolean mRecording = false;

    // sender and recipient data
    private Chat mChat;
    private String mSenderName;

    // swipe-related
    private Rect mContactRect = new Rect();
    private float mX1, mY1;
    private long mT1;
    private int mMinSwipeDistance;

    // callback
    private OnRecordingFinishedCallback mOnRecordingFinishedCallback;

    public ChatRecordOverlay(TouchInterceptable touchInterceptable) {
        super(SCREEN_RECORDING_ID, R.layout.v_recording_overlay_layout, false, true);
        this.mTouchInterceptable = touchInterceptable;
    }

    /**
     * Binds to the {@link RecordService}
     * Also starts listening for touch events that trigger the overlay.
     */
    public void bindService() {
        if(mRecordServiceManager != null) {
            mRecordServiceManager.bind();
        }
        mTouchInterceptable.addTouchEventInterceptor(this);
    }

    /**
     * Unbinds from the {@link RecordService}
     * Also stops listening for touch events that trigger the overlay.
     */
    public void unbindService() {
        mTouchInterceptable.removeTouchEventInterceptor(this);
        if(mRecordServiceManager != null) {
            mRecordServiceManager.unbind();
        }
    }

    @Override
    protected View onCreateView(LayoutInflater layoutInflater) {
        mView = (ChatRecordOverlayView) super.onCreateView(layoutInflater);

        mRecordServiceManager = new RecordServiceManager(getContext());
        mRecordServiceManager.setListener(this);
        mRecordServiceManager.start(false);

        mRecordSoundPlayer = MediaPlayer.create(getContext(), R.raw.s_record);
        mMinSwipeDistance = Utils.dpToPx(getContext(), MIN_SWIPE_DISTANCE_DP);

        getOverlayManager().addOverlayVisibilityChangeListener(this);

        return mView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getOverlayManager().removeOverlayVisibilityChangeListener(this);

        unbindService();

        if(mRecordSoundPlayer != null) {
            mRecordSoundPlayer.release();
        }
    }

    @Override
    public void onOverlayShown(Overlay overlay) {
        mView.doSlideIn();
    }

    @Override
    public void onOverlayHidden(Overlay overlay, boolean wasCancelled) {
    }

    @Override
    public boolean show(boolean animated) {
        setRecordDuration(0);

        boolean shown = super.show(animated);

        if(shown) {
            mViewBounds.getGlobalVisibleRect(mContactRect);
            mView.setContentPosition(mContactRect);

            if(mRecordSoundPlayer.isPlaying()) {
                mRecordSoundPlayer.stop();
            }
            mRecordSoundPlayer.seekTo(0);
            mRecordSoundPlayer.start();

            mView.setName(mChat.getTitle());
            String filename = getContext().getString(R.string.filename_message_from) + Utils.normalizeAndCleanString(mSenderName);

            mRecordServiceManager.startRecording(filename, mChat, MAX_DURATION_MILLIS);

            mView.dispatchTouchEvent(MotionEvent.obtain(System.currentTimeMillis(), System.currentTimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
        }

        return shown;
    }

    public boolean hide(boolean animated, boolean cancel) {
        return hide(animated, RECORDING_OVERLAY_HIDE_DELAY, cancel);
    }

    @Override
    public boolean hide(boolean animated, long delayMs, boolean cancel) {
        if(mRecording) {
            mRecordServiceManager.stopRecording(true);
        }

        if(cancel) {
            mView.doSlideOut();
        } else {
            mView.doFadeOut();
        }

        return super.hide(animated, RECORDING_OVERLAY_HIDE_DELAY, cancel);
    }

    // RECORDER LISTENER METHODS
    @Override
    public void onStartRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getChat(), event.getLoudness());
        mRecording = true;
    }

    @Override
    public void onStopRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getChat(), event.getLoudness());
        boolean valid = false;
        if(mRecording) {
            valid = event.getRecording().getValidatedFile() != null;
            if(valid && mOnRecordingFinishedCallback != null) {
                mOnRecordingFinishedCallback.onRecordingFinished(event);
            }
        }
        mRecording = false;
        hide(false, RECORDING_OVERLAY_HIDE_DELAY, !valid);
    }

    @Override
    public void onResumeRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getChat(), event.getLoudness());
    }

    @Override
    public void onPauseRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getChat(), event.getLoudness());
    }

    @Override
    public void onLoudnessRecording(RecordService.Event event) {
        mView.setAmplitude(event.getLoudness());
        setRecordDuration(event.getRecording().getDurationMillis());
    }

    @Override
    public void onErrorRecording(RecordService.Event event) {
        if(event.getError() instanceof NoMicDataIOException) {
            Toast.makeText(getContext(), getContext().getString(R.string.msg_nomicdata_error), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), getContext().getString(R.string.msg_record_error), Toast.LENGTH_LONG).show();
        }
        mRecording = false;
        hide(false, RECORDING_OVERLAY_HIDE_DELAY, true);
    }

    @Override
    public void onBoundRecording(Recording currentRecording, Chat currentChat, float currentLoudness) {
        setRecordDuration(currentRecording == null ? 0 : currentRecording.getDurationMillis());
    }

    private void setRecordDuration(float fullDuration) {
        mView.setMillis(fullDuration);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            mX1 = event.getX();
            mY1 = event.getY();
            mT1 = android.os.SystemClock.uptimeMillis();
        }
        if(isVisible()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float mX2 = event.getX();
                    float mY2 = event.getY();
                    long mT2 = android.os.SystemClock.uptimeMillis();
                    float deltaX = mX2 - mX1;
                    float deltaY = mY2 - mY1;

                    boolean discard = (Math.abs(deltaX) > mMinSwipeDistance || Math.abs(deltaY) > mMinSwipeDistance) && (mT2 - mT1) < MAX_SWIPE_DURATION;
                    if (!discard) {
                        boolean lessThanMin = mView.getMillis() < MIN_DURATION_MILLIS;
                        boolean moreThanMax = mView.getMillis() > MAX_DURATION_MILLIS;
                        discard = lessThanMin || moreThanMax;

                        if (lessThanMin) {
                            CustomToast.makeText(getContext(), R.string.msg_record_at_least, Toast.LENGTH_LONG).show();
                        }
                        if (moreThanMax) {
                            CustomToast.makeText(getContext(), R.string.msg_exceeded_maxduration, Toast.LENGTH_LONG).show();
                        }
                    }
                    if (mRecording) {
                        mRecordServiceManager.stopRecording(discard);
                    } else {
                        hide(false, RECORDING_OVERLAY_HIDE_DELAY, discard);
                    }
                    break;
                default:
                    long now = android.os.SystemClock.uptimeMillis();
                    if ((now - mT1) > MAX_SWIPE_DURATION) {
                        mT1 = now;
                        mX1 = event.getX();
                        mY1 = event.getY();
                    }
            }
        }
        return true;
    }

    public OnRecordingFinishedCallback getOnRecordingFinishedCallback() {
        return mOnRecordingFinishedCallback;
    }

    public void setOnRecordingFinishedCallback(OnRecordingFinishedCallback mOnRecordingFinishedCallback) {
        this.mOnRecordingFinishedCallback = mOnRecordingFinishedCallback;
    }

    public View getViewBounds() {
        return mViewBounds;
    }

    public void setViewBounds(View mViewBounds) {
        this.mViewBounds = mViewBounds;
    }

    public Chat getChat() {
        return mChat;
    }

    public void setChat(Chat mChat) {
        this.mChat = mChat;
    }

    public String getSenderName() {
        return mSenderName;
    }

    public void setSenderName(String mSenderName) {
        this.mSenderName = mSenderName;
    }

    @Override
    public void assimilateFrom(Overlay overlay) {
        super.assimilateFrom(overlay);

        this.mViewBounds = ((ChatRecordOverlay) overlay).mViewBounds;
        this.mChat = ((ChatRecordOverlay) overlay).mChat;
        this.mOnRecordingFinishedCallback = null;
    }
}
