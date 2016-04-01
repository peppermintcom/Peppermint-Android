package com.peppermint.app.ui.chat.recorder;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.ui.AnimatorBuilder;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 16-10-2015.
 *
 * The recording overlay that shows up while recording.
 *
 */
public class ChatRecordOverlayView extends FrameLayout {

    private TextView mTxtRecordingFor, mTxtDuration;
    private LinearLayout mLytContainer;
    private LinearLayout mLytMicContainer;
    private LinearLayout mLytTip;
    private ImageView mImgMic;
    private float mMillis;

    private float mMinAmplitude = 0, mMaxAmplitude = 1;
    private int mShadowSize = 0;
    private AnimatorBuilder mAnimatorBuilder;

    private Animator mSlideAnimator;
    private Point mScreenSize;
    private int mStatusBarHeight;

    public ChatRecordOverlayView(Context context) {
        super(context);
        init(context, null);
    }

    public ChatRecordOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ChatRecordOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChatRecordOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mAnimatorBuilder = new AnimatorBuilder();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        removeAllViews();
        View contentView = inflater.inflate(R.layout.v_recording_layout, null);
        addView(contentView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mTxtRecordingFor = (TextView) findViewById(R.id.txtRecordingFor);
        mTxtDuration = (TextView) findViewById(R.id.txtDuration);
        mLytContainer = (LinearLayout) findViewById(R.id.lytContainer);
        mImgMic = (ImageView) findViewById(R.id.imgMic);
        mLytMicContainer = (LinearLayout) findViewById(R.id.lytMicContainer);
        mLytTip = (LinearLayout) findViewById(R.id.lytTip);

        mShadowSize = Utils.dpToPx(context, 12);
        mScreenSize = Utils.getScreenSize(context);
        mStatusBarHeight = Utils.getStatusBarHeight(context);
    }

    public void doSlideIn() {
        if(mSlideAnimator != null) {
            mSlideAnimator.cancel();
        }

        setMillis(0);

        mSlideAnimator = new AnimatorSet();

        Animator slideInAnimator = mAnimatorBuilder.buildSlideInLeftAnimator(mLytContainer);
        Animator fadeInAnimator = mAnimatorBuilder.buildFadeInAnimator(mImgMic, mLytTip);

        ((AnimatorSet) mSlideAnimator).playTogether(slideInAnimator, fadeInAnimator);
        mLytContainer.setAlpha(1f);
        mSlideAnimator.start();
    }

    public void doSlideOut() {
        if(mSlideAnimator != null) {
            mSlideAnimator.cancel();
        }

        mSlideAnimator = new AnimatorSet();

        Animator slideOutAnimator = mAnimatorBuilder.buildSlideOutRightAnimator(0, mLytContainer, 0);
        Animator fadeOutAnimator = mAnimatorBuilder.buildFadeOutAnimator(mImgMic, mLytTip);

        ((AnimatorSet) mSlideAnimator).playTogether(slideOutAnimator, fadeOutAnimator);
        mSlideAnimator.start();
    }

    public void doFadeOut() {
        if(mSlideAnimator != null) {
            mSlideAnimator.cancel();
        }

        mSlideAnimator = new AnimatorSet();
        Animator fadeOutAnimator = mAnimatorBuilder.buildFadeOutAnimator(mImgMic, mLytTip, mLytContainer);
        ((AnimatorSet) mSlideAnimator).playTogether(fadeOutAnimator);
        mSlideAnimator.start();
    }

    public void setMillis(float millis) {
        this.mMillis = millis;
        float duration = millis / 1000f;

        long mins = (long) (duration / 60);
        long secs = (long) (duration % 60);
        long hours = mins / 60;
        if(hours > 0) {
            mins = mins % 60;
        }

        mTxtDuration.setText((hours > 0 ? hours + ":" : "") + mins + ":" + (secs < 10 ? "0" : "") + secs);
    }

    public void setAmplitude(float amplitude) {
        if(amplitude > mMaxAmplitude) {
            mMaxAmplitude = amplitude;
        }
        if(amplitude < mMinAmplitude) {
            mMinAmplitude = amplitude;
        }

        amplitude /= (mMaxAmplitude - mMinAmplitude);
        amplitude *= 0.09f;

        mImgMic.setScaleX(1f + amplitude);
        mImgMic.setScaleY(1f + amplitude);
    }

    public float getMillis() {
        return mMillis;
    }

    public void setName(String name) {
        mTxtRecordingFor.setText(String.format(getContext().getString(R.string.recording_for), name));
    }

    public void setContentPosition(Rect rect) {
        int factor = 0;

        // not necessary when using the overlay
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && getContext() instanceof Activity) {
            factor = mStatusBarHeight;
        }

        FrameLayout.LayoutParams newParams = new FrameLayout.LayoutParams(rect.width(), rect.height() + (mShadowSize*2));
        newParams.setMargins(0, rect.top - mShadowSize - factor, 0, 0);
        mLytContainer.setLayoutParams(newParams);
        mLytContainer.invalidate();

        FrameLayout.LayoutParams newMicParams = (FrameLayout.LayoutParams) mLytMicContainer.getLayoutParams();
        newMicParams.height = rect.height();
        newMicParams.setMargins(newMicParams.leftMargin, rect.top - factor, newMicParams.rightMargin, newMicParams.bottomMargin);
        mLytMicContainer.setLayoutParams(newMicParams);
        mLytMicContainer.invalidate();

        FrameLayout.LayoutParams newTipParams = (FrameLayout.LayoutParams) mLytTip.getLayoutParams();

        if((rect.bottom + newTipParams.height) <= mScreenSize.y) {
            newTipParams.setMargins(newTipParams.leftMargin, rect.bottom - factor, 0, 0);
            mLytTip.setBackgroundResource(R.drawable.img_popup_tip_top_right);
        } else {
            newTipParams.setMargins(newTipParams.leftMargin, rect.top - newTipParams.height - factor, 0, 0);
            mLytTip.setBackgroundResource(R.drawable.img_popup_tip_bottom_right);
        }

        mLytTip.setLayoutParams(newTipParams);
        mLytTip.invalidate();
    }
}
