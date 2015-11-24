package com.peppermint.app.ui.canvas.progress;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.AnimatedLayer;
import com.peppermint.app.ui.canvas.AnimatedLayerListener;
import com.peppermint.app.ui.canvas.AnimatedLayerSet;
import com.peppermint.app.ui.canvas.AnimatedView;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Custom recording view for Peppermint to show the progress of the recording.
 */
public class SplashView extends AnimatedView {

    private static final int DEF_CORNER_RADIUS_DP = 50;
    private static final String DEF_BACKGROUND_COLOR1 = "#68c4a3";
    private static final String DEF_BACKGROUND_COLOR2 = "#2ebdb2";

    private float mCornerRadius;
    private int mBackgroundColor1, mBackgroundColor2;
    private Paint mBackgroundPaint, mBitmapPaint;
    private boolean mFillBackground;

    private SplashMouthAnimatedLayer mMouth;
    private ProgressEyeAnimatedLayer mLeftEye, mRightEye;
    private ProgressBoxAnimatedLayer mProgressBox;
    private AnimatedLayerListener mBlinkListener = new AnimatedLayerListener() {
        @Override
        public void onAnimationStarted(AnimatedLayer animatedLayer) {
        }

        @Override
        public void onAnimationEnded(AnimatedLayer animatedLayer) {
            mMouth.reset();
            mMouth.start();
            mLeftEye.getBlinkAnimation().removeAnimationListener(this);
        }
    };

    public SplashView(Context context) {
        super(context);
        init(null);
    }

    public SplashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SplashView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SplashView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    protected void init(AttributeSet attrs) {
        mCornerRadius = Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP);
        mBackgroundColor1 = Color.parseColor(DEF_BACKGROUND_COLOR1);
        mBackgroundColor2 = Color.parseColor(DEF_BACKGROUND_COLOR2);

        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                mCornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));
                mBackgroundColor1 = a.getColor(R.styleable.PeppermintView_backgroundColor1, Color.parseColor(DEF_BACKGROUND_COLOR1));
                mBackgroundColor2 = a.getColor(R.styleable.PeppermintView_backgroundColor2, Color.parseColor(DEF_BACKGROUND_COLOR2));
                mFillBackground = a.getBoolean(R.styleable.PeppermintView_fillBackground, false);
            } finally {
                a.recycle();
            }
        }

        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setFilterBitmap(true);
        mBitmapPaint.setDither(true);

        mMouth = new SplashMouthAnimatedLayer(getContext(), 2000, mBitmapPaint);
        mLeftEye = new ProgressEyeAnimatedLayer(getContext(), 360, 1500, mBitmapPaint);
        mRightEye = new ProgressEyeAnimatedLayer(getContext(), 360, 1500, mBitmapPaint);
        mProgressBox = new ProgressBoxAnimatedLayer(getContext(), 1000, false, mCornerRadius, 0, null, null, mFillBackground ? mBackgroundPaint : null, null);
        mLeftEye.setElapsedTime(315);
        mRightEye.setElapsedTime(315);
        mLeftEye.getBlinkAnimation().setElapsedTime(750);
        mRightEye.getBlinkAnimation().setElapsedTime(750);

        AnimatedLayerSet set = new AnimatedLayerSet(getContext());
        set.playTogether(mProgressBox, mMouth, mLeftEye, mRightEye);
        addLayer(set);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // background gradients
        mBackgroundPaint.setShader(new LinearGradient(0, 0, getLocalWidth(), getLocalHeight(), mBackgroundColor1, mBackgroundColor2, Shader.TileMode.MIRROR));

        // box bounds
        Rect fullBounds = new Rect(0, 0, (int) getLocalWidth(), (int) getLocalHeight());
        mProgressBox.setBounds(fullBounds);

        // eye bounds
        final float eyeXFactor = 7f;
        final float eyeYFactor = 5.5f;
        float fullSideLength = (getLocalWidth() > getLocalHeight() ? getLocalHeight() : getLocalWidth());
        int eyeRadius = (int) (fullSideLength / 18f);
        int eyeCenterX = (int) (fullBounds.centerX() - (fullSideLength / eyeXFactor));
        int eyeCenterY = (int) (fullBounds.centerY() - (fullSideLength / eyeYFactor));
        Rect leftEyeBounds = new Rect(eyeCenterX - eyeRadius, eyeCenterY - eyeRadius, eyeCenterX + eyeRadius, eyeCenterY + eyeRadius);
        mLeftEye.setBounds(leftEyeBounds);

        Rect rightEyeBounds = new Rect(leftEyeBounds);
        rightEyeBounds.offset((int) (fullSideLength / (eyeXFactor / 2f)), 0);
        mRightEye.setBounds(rightEyeBounds);

        // mouth bounds
        final float mouthXFactor = 3.5f;
        final float mouthYFactor = 14f;
        Rect mouthBounds = new Rect((int) (fullBounds.centerX() - (fullSideLength / mouthXFactor)), (int) (fullBounds.centerY() - (fullSideLength / mouthYFactor)),
                (int) (fullBounds.centerX() + (fullSideLength / mouthXFactor)), (int) (fullBounds.centerY() + (fullSideLength / mouthXFactor)));
        mMouth.setBounds(mouthBounds);
    }

    public synchronized void blink() {
        mLeftEye.blink();
        mRightEye.blink();
    }

    public synchronized void blinkAndHalfOpenMouth() {
        mLeftEye.getBlinkAnimation().addAnimationListener(mBlinkListener);
        blink();
    }

    public synchronized void blinkLeftEye() {
        mLeftEye.blink();
    }

    public synchronized void blinkRightEye() {
        mRightEye.blink();
    }

    public SplashMouthAnimatedLayer getMouth() {
        return mMouth;
    }

    public ProgressEyeAnimatedLayer getLeftEye() {
        return mLeftEye;
    }

    public ProgressEyeAnimatedLayer getRightEye() {
        return mRightEye;
    }

    public ProgressBoxAnimatedLayer getProgressBox() {
        return mProgressBox;
    }
}
