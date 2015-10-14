package com.peppermint.app.ui.tutorial;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.AnimatedLayer;
import com.peppermint.app.ui.canvas.AnimatedLayerListener;
import com.peppermint.app.ui.canvas.progress.RecordProgressView;
import com.peppermint.app.utils.AnimatorBuilder;

public class T2RecordFragment extends TutorialActivity.TutorialFragment implements AnimatedLayerListener {

    private RecordProgressView mRecordView;
    private ImageView mImgSentCheck;
    private TextView mTxtSent;
    private AnimatorBuilder mAnimatorBuilder;
    private final Handler mHandler = new Handler();

    public T2RecordFragment() {
        mAnimatorBuilder = new AnimatorBuilder();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getActivity().getApplication();

        View v = inflater.inflate(R.layout.f_t2_record_layout, container, false);

        mImgSentCheck = (ImageView) v.findViewById(R.id.imgSentCheck);

        mTxtSent = (TextView) v.findViewById(R.id.txtSent);
        mTxtSent.setTypeface(app.getFontSemibold());

        TextView txtRecordAndSendToEmail = (TextView) v.findViewById(R.id.txtRecordAndSendToEmail);
        txtRecordAndSendToEmail.setTypeface(app.getFontSemibold());

        mRecordView = (RecordProgressView) v.findViewById(R.id.pmProgress);
        mRecordView.getProgressBox().addAnimationListener(this);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecordView.resetAnimations();
        mRecordView.getProgressBox().setLooping(false);
        mRecordView.getProgressBox().setDuration(5000);
        mRecordView.getProgressBox().setFirstPartOnly(true);
        mRecordView.getLeftEye().setLooping(false);
        mRecordView.getLeftEye().setDuration(5000);
        mRecordView.getRightEye().setLooping(false);
        mRecordView.getRightEye().setDuration(5000);
        mRecordView.getMouth().setLooping(false);
        mRecordView.getMouth().setDuration(5000);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecordView.startAnimations();
            }
        }, 1000);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mRecordView.isDrawingThreadRunning()) {
            mRecordView.startDrawingThread();
        }
    }

    @Override
    public void onStop() {
        mRecordView.stopDrawingThread();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAnimationStarted(AnimatedLayer animatedLayer) {
        // nothing to do here
    }

    @Override
    public void onAnimationEnded(AnimatedLayer animatedLayer) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecordView.getLeftEye().setElapsedTime(2500);
                mRecordView.getRightEye().setElapsedTime(2500);

                Interpolator interpolator = new DecelerateInterpolator();
                Animator imgSentAnimator = mAnimatorBuilder.buildFadeScaleInAnimator(mImgSentCheck);
                Animator txtSentAnimator = mAnimatorBuilder.buildSlideInRightAnimator(mTxtSent);
                txtSentAnimator.setInterpolator(interpolator);
                imgSentAnimator.setInterpolator(interpolator);
                AnimatorSet set = new AnimatorSet();
                set.playTogether(imgSentAnimator, txtSentAnimator);
                set.start();
                mImgSentCheck.setVisibility(View.VISIBLE);
                mTxtSent.setVisibility(View.VISIBLE);
            }
        }, 500);

        mRecordView.blink();
    }
}
