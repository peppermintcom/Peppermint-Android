package com.peppermint.app.ui.recipients;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.sending.SenderEvent;
import com.peppermint.app.sending.mail.nativemail.IntentMailSenderTask;
import com.peppermint.app.sending.nativesms.IntentSMSSenderTask;
import com.peppermint.app.utils.AnimatorBuilder;

/**
 * Created by Nuno Luz on 17-09-2015.
 */
public class SenderControlLayout extends FrameLayout implements View.OnClickListener, SenderServiceManager.Listener {

    private static final int DURATION = 4000;

    private TextView mTxtStatus, mTxtTapToCancel;
    private ImageView mImgStatus;
    private SenderServiceManager mSenderServiceManager;
    private AnimatorBuilder mAnimatorBuilder;

    private Animator mShowAnimation, mHideAnimation;

    public SenderControlLayout(Context context) {
        super(context);
        init(null);
    }

    public SenderControlLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SenderControlLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SenderControlLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mAnimatorBuilder = new AnimatorBuilder();
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        layoutInflater.inflate(R.layout.v_sender_control_layout, this);

        mTxtStatus = (TextView) findViewById(R.id.txtStatus);
        mTxtTapToCancel = (TextView) findViewById(R.id.txtTapToCancel);
        mImgStatus = (ImageView) findViewById(R.id.imgStatus);

        setClickable(true);
        setOnClickListener(this);
        mImgStatus.setVisibility(GONE);
        setVisibility(GONE);
    }

    public void setTypeface(Typeface font) {
        mTxtStatus.setTypeface(font);
        mTxtTapToCancel.setTypeface(font);
    }

    public void setSenderManager(SenderServiceManager mSenderServiceManager) {
        this.mSenderServiceManager = mSenderServiceManager;
    }

    protected void hide(int delay) {
        if(getVisibility() == View.VISIBLE) {
            if(mHideAnimation != null && mHideAnimation.isStarted()) {
                mHideAnimation.cancel();
            }

            mHideAnimation = mAnimatorBuilder.buildSlideOutBottomAnimator(delay, this, 0);
            mHideAnimation.addListener(new Animator.AnimatorListener() {
                private boolean isCancelled = false;
                @Override
                public void onAnimationStart(Animator animation) { }
                @Override /* finally completely hide the view */
                public void onAnimationEnd(Animator animation) { if(!isCancelled) { setVisibility(View.GONE); } mHideAnimation = null; }
                @Override
                public void onAnimationCancel(Animator animation) { isCancelled = true; mHideAnimation = null; }
                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            mHideAnimation.start();
        }
    }

    protected void show() {
        if(getVisibility() == View.GONE) {
            if(mShowAnimation == null || !mShowAnimation.isRunning()) {
                mShowAnimation = mAnimatorBuilder.buildFadeSlideInBottomAnimator(this);
                mShowAnimation.start();
            }
        }
        setVisibility(View.VISIBLE);
    }

    protected void showAndHide() {
        if(getVisibility() != View.GONE) {
            hide(DURATION); // hide only after 4 secs.
        } else {
            if(mShowAnimation != null && mShowAnimation.isRunning()) {
                return;
            }

            mShowAnimation = mAnimatorBuilder.buildFadeSlideInBottomAnimator(this);
            mShowAnimation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) { }
                @Override
                public void onAnimationEnd(Animator animation) { hide(DURATION); mShowAnimation = null; }
                @Override
                public void onAnimationCancel(Animator animation) { mShowAnimation = null; }
                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            mShowAnimation.start();
            setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if(mSenderServiceManager.cancel()) {
            mTxtTapToCancel.setVisibility(View.GONE);
            mTxtStatus.setText(R.string.cancelling);
        }
    }

    private void onBoundSendService(SenderEvent event) {
        if(mSenderServiceManager.isSending()) {
            mTxtStatus.setText(getContext().getString(R.string.uploading));
            mTxtTapToCancel.setVisibility(View.VISIBLE);
            mImgStatus.setVisibility(View.GONE);
            showAndHide();
        }
    }

    @Override
    public void onSendFinished(SenderEvent event) {
        if(!mSenderServiceManager.isSending()) {
            // do not show message for IntentMailSender and IntentSMSSender
            if(event != null && (event.getSenderTask() instanceof IntentMailSenderTask || event.getSenderTask() instanceof IntentSMSSenderTask)) {
                hide(0);
                return;
            }
            mTxtStatus.setText(getContext().getString(R.string.sent));
            mTxtTapToCancel.setVisibility(View.GONE);
            mImgStatus.setVisibility(View.VISIBLE);
            showAndHide();
        }
    }

    @Override
    public void onBoundSendService() {
        onBoundSendService(null);
    }

    @Override
    public void onSendStarted(SenderEvent event) {
        onBoundSendService(event);
    }

    @Override
    public void onSendCancelled(SenderEvent event) {
        if(!mSenderServiceManager.isSending()) {
            mTxtStatus.setText(getContext().getString(R.string.cancelled));
            mTxtTapToCancel.setVisibility(View.GONE);
            mImgStatus.setVisibility(View.GONE);
            showAndHide();
        }
    }

    @Override
    public void onSendError(SenderEvent event) {
        if(!mSenderServiceManager.isSending()) {
            mTxtStatus.setText(getContext().getString(R.string.not_sent));
            mTxtTapToCancel.setVisibility(View.GONE);
            mImgStatus.setVisibility(View.GONE);
            showAndHide();
        }
    }

    @Override
    public void onSendProgress(SenderEvent event) { /* nothing to do here */ }

    @Override
    public void onSendQueued(SenderEvent event) {
        if(!mSenderServiceManager.isSending()) {
            mTxtStatus.setText(getContext().getString(R.string.queued));
            mTxtTapToCancel.setVisibility(View.GONE);
            mImgStatus.setVisibility(View.GONE);
            showAndHide();
        }
    }
}
