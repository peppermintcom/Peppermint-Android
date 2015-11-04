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
import com.peppermint.app.sending.SendingEvent;
import com.peppermint.app.sending.nativemail.IntentMailSendingTask;
import com.peppermint.app.utils.AnimatorBuilder;

/**
 * Created by Nuno Luz on 17-09-2015.
 */
public class SenderControlLayout extends FrameLayout implements View.OnClickListener, SenderServiceManager.Listener {

    private TextView mTxtStatus, mTxtTapToCancel;
    private ImageView mImgStatus;
    private SenderServiceManager mSenderServiceManager;
    private AnimatorBuilder mAnimatorBuilder;

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
            Animator anim = mAnimatorBuilder.buildSlideOutBottomAnimator(delay, this, 0);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) { }
                @Override /* finally completely hide the view */
                public void onAnimationEnd(Animator animation) { setVisibility(View.GONE); }
                @Override
                public void onAnimationCancel(Animator animation) { }
                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            anim.start();
        }
    }

    protected void show() {
        if(getVisibility() == View.GONE) {
            Animator anim = mAnimatorBuilder.buildFadeSlideInBottomAnimator(this);
            anim.start();
        }
        setVisibility(View.VISIBLE);
    }

    protected void showAndHide() {
        if(getVisibility() != View.GONE) {
            hide(5000); // hide only after 5 secs.
        } else {
            Animator anim = mAnimatorBuilder.buildFadeSlideInBottomAnimator(this);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) { }
                @Override
                public void onAnimationEnd(Animator animation) { hide(5000); }
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            anim.start();
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

    private void onBoundSendService(SendingEvent event) {
        if(mSenderServiceManager.isSending()) {
            // do not show message for IntentMailSender
            if(event != null && event.getSendingTask() instanceof IntentMailSendingTask) {
                return;
            }

            mTxtStatus.setText(getContext().getString(R.string.uploading));
            mTxtTapToCancel.setVisibility(View.VISIBLE);
            mImgStatus.setVisibility(View.GONE);
            show();
        } else {
            hide(500);
        }
    }

    @Override
    public void onSendFinished(SendingEvent event) {
        if(!mSenderServiceManager.isSending()) {
            // do not show message for IntentMailSender
            if(event != null && event.getSendingTask() instanceof IntentMailSendingTask) {
                hide(0);
                return;
            }
            showAndHide();
            mTxtStatus.setText(getContext().getString(R.string.sent));
            mTxtTapToCancel.setVisibility(View.GONE);
            mImgStatus.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBoundSendService() {
        onBoundSendService(null);
    }

    @Override
    public void onSendStarted(SendingEvent event) {
        onBoundSendService(event);
    }

    @Override
    public void onSendCancelled(SendingEvent event) {
        onBoundSendService(event);
    }

    @Override
    public void onSendError(SendingEvent event) {
        onBoundSendService(event);
    }

    @Override
    public void onSendProgress(SendingEvent event) { /* nothing to do here */ }

    @Override
    public void onSendQueued(SendingEvent event) {
        onBoundSendService(event);
    }
}
