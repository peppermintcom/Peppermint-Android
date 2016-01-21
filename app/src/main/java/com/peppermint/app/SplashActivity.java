package com.peppermint.app;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.peppermint.app.ui.canvas.AnimatedLayer;
import com.peppermint.app.ui.canvas.AnimatedLayerListener;
import com.peppermint.app.ui.canvas.progress.SplashView;
import com.peppermint.app.utils.AnimatorBuilder;

public class SplashActivity extends Activity {

    private SplashView mImgLogo;
    private TextView mTxtName;
    private AnimatorBuilder mAnimatorBuilder;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_splash_layout);

        PeppermintApp app = (PeppermintApp) getApplication();
        mAnimatorBuilder = new AnimatorBuilder();

        mImgLogo = (SplashView) findViewById(R.id.imgLogo);

        mTxtName = (TextView) findViewById(R.id.txtName);
        mTxtName.setTypeface(app.getFontSemibold());

        mImgLogo.getLeftEye().getBlinkAnimation().addAnimationListener(new AnimatedLayerListener() {
            @Override
            public void onAnimationStarted(AnimatedLayer animatedLayer) {

            }

            @Override
            public void onAnimationEnded(AnimatedLayer animatedLayer) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Animator animator = mAnimatorBuilder.buildSlideInBottomAnimator(mTxtName);
                        animator.setInterpolator(new DecelerateInterpolator());
                        animator.setDuration(2000);
                        animator.start();
                        mTxtName.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setBackgroundDrawableResource(R.color.background0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.slide_in_right_activity, R.anim.slide_out_left_activity);
            }
        }, 6000);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mImgLogo.blinkAndHalfOpenMouth();
            }
        }, 1000);
    }
}
