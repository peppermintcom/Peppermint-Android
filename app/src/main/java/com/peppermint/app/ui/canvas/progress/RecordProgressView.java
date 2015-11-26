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
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.AnimatedLayerSet;
import com.peppermint.app.ui.canvas.AnimatedView;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Custom recording view for Peppermint to show the progress of the recording.
 */
public class RecordProgressView extends AnimatedView {

    private static final float DEF_CYCLE_SECONDS = 30f;

    private static final int DEF_CORNER_RADIUS_DP = 50;
    private static final int DEF_PROGRESS_THICKNESS_DP = 10;
    private static final String DEF_BORDER_COLOR = "#ffffff";
    private static final String DEF_BACKGROUND_COLOR1 = "#68c4a3";
    private static final String DEF_BACKGROUND_COLOR2 = "#2ebdb2";
    private static final String DEF_PRESSED_BACKGROUND_COLOR1 = "#000000";
    private static final String DEF_PRESSED_BACKGROUND_COLOR2 = "#000000";
    private static final String DEF_PROGRESS_FILL_COLOR = "#1f8479";
    private static final String DEF_PROGRESS_EMPTY_COLOR = "#ffffff";

    private float mSeconds;

    private float mCornerRadius, mProgressThickness;
    private int mBackgroundColor1, mBackgroundColor2, mPressedBackgroundColor1, mPressedBackgroundColor2, mBorderColor;
    private int mProgressEmptyColor, mProgressFillColor;
    private Paint mBackgroundPaint, mBackgroundPressedPaint, mBorderPaint, mProgressPaint, mEmptyProgressPaint, mBitmapPaint;
    private boolean mFillBackground;

    private ProgressMouthAnimatedLayer mMouth;
    private ProgressEyeAnimatedLayer mLeftEye, mRightEye;
    private ProgressBoxAnimatedLayer mProgressBox;

    public RecordProgressView(Context context) {
        super(context);
        init(null);
    }

    public RecordProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RecordProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RecordProgressView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    protected void init(AttributeSet attrs) {
        mSeconds = 0;
        mCornerRadius = Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP);
        mProgressThickness = Utils.dpToPx(getContext(), DEF_PROGRESS_THICKNESS_DP);

        mProgressEmptyColor = Color.parseColor(DEF_PROGRESS_EMPTY_COLOR);
        mProgressFillColor = Color.parseColor(DEF_PROGRESS_FILL_COLOR);
        mBorderColor = Color.parseColor(DEF_BORDER_COLOR);
        mBackgroundColor1 = Color.parseColor(DEF_BACKGROUND_COLOR1);
        mBackgroundColor2 = Color.parseColor(DEF_BACKGROUND_COLOR2);
        mPressedBackgroundColor1 = Color.parseColor(DEF_PRESSED_BACKGROUND_COLOR1);
        mPressedBackgroundColor2 = Color.parseColor(DEF_PRESSED_BACKGROUND_COLOR2);

        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                mSeconds = a.getFloat(R.styleable.PeppermintView_seconds, 0);

                mCornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));
                mProgressThickness = a.getDimensionPixelSize(R.styleable.PeppermintView_borderWidth, Utils.dpToPx(getContext(), DEF_PROGRESS_THICKNESS_DP));

                mProgressEmptyColor = a.getColor(R.styleable.PeppermintView_progressEmptyColor, Color.parseColor(DEF_PROGRESS_EMPTY_COLOR));
                mProgressFillColor = a.getColor(R.styleable.PeppermintView_progressFillColor, Color.parseColor(DEF_PROGRESS_FILL_COLOR));
                mBorderColor = a.getColor(R.styleable.PeppermintView_borderColor, Color.parseColor(DEF_BORDER_COLOR));
                mBackgroundColor1 = a.getColor(R.styleable.PeppermintView_backgroundColor1, Color.parseColor(DEF_BACKGROUND_COLOR1));
                mBackgroundColor2 = a.getColor(R.styleable.PeppermintView_backgroundColor2, Color.parseColor(DEF_BACKGROUND_COLOR2));
                mPressedBackgroundColor1 = a.getColor(R.styleable.PeppermintView_pressedBackgroundColor1, Color.parseColor(DEF_PRESSED_BACKGROUND_COLOR1));
                mPressedBackgroundColor2 = a.getColor(R.styleable.PeppermintView_pressedBackgroundColor2, Color.parseColor(DEF_PRESSED_BACKGROUND_COLOR2));

                mFillBackground = a.getBoolean(R.styleable.PeppermintView_fillBackground, false);
            } finally {
                a.recycle();
            }
        }

        mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressPaint.setStyle(Paint.Style.FILL);
        mProgressPaint.setColor(mProgressFillColor);

        mEmptyProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mEmptyProgressPaint.setStyle(Paint.Style.FILL);
        mEmptyProgressPaint.setColor(mProgressEmptyColor);

        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mBackgroundPressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPressedPaint.setStyle(Paint.Style.FILL);

        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.FILL);
        mBorderPaint.setColor(mBorderColor);

        mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setFilterBitmap(true);
        mBitmapPaint.setDither(true);

        mMouth = new ProgressMouthAnimatedLayer(getContext(), (long) (DEF_CYCLE_SECONDS * 1000f), mBitmapPaint);
        mLeftEye = new ProgressEyeAnimatedLayer(getContext(), (long) (DEF_CYCLE_SECONDS * 1000f), 1000, mBitmapPaint);
        mRightEye = new ProgressEyeAnimatedLayer(getContext(), (long) (DEF_CYCLE_SECONDS * 1000f), 1000, mBitmapPaint);
        mProgressBox = new ProgressBoxAnimatedLayer(getContext(), (long) (DEF_CYCLE_SECONDS * 2000f), true, mCornerRadius, mProgressThickness, mProgressPaint, mEmptyProgressPaint, mFillBackground ? mBackgroundPaint : null, mBackgroundPressedPaint);

        AnimatedLayerSet set = new AnimatedLayerSet(getContext());
        set.playTogether(mProgressBox, mMouth, mLeftEye, mRightEye);
        addLayer(set);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // background gradients
        mBackgroundPaint.setShader(new LinearGradient(0, 0, getLocalWidth(), getLocalHeight(), mBackgroundColor1, mBackgroundColor2, Shader.TileMode.MIRROR));
        mBackgroundPressedPaint.setShader(new LinearGradient(0, 0, getLocalWidth(), getLocalHeight(), mPressedBackgroundColor1, mPressedBackgroundColor2, Shader.TileMode.MIRROR));

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

    public float getSeconds() {
        return mSeconds;
    }

    public synchronized void setSeconds(float mSeconds) {
        this.mSeconds = mSeconds;

        double elapsedTime = mSeconds * 1000f;
        mProgressBox.setElapsedTime(elapsedTime);
        mMouth.setElapsedTime(elapsedTime);
        mLeftEye.setElapsedTime(elapsedTime);
        mRightEye.setElapsedTime(elapsedTime);
        //invalidate();
    }

    public synchronized void blink() {
        mLeftEye.blink();
        mRightEye.blink();
    }

    public synchronized void blinkLeftEye() {
        mLeftEye.blink();
    }

    public synchronized void blinkRightEye() {
        mRightEye.blink();
    }

    public ProgressMouthAnimatedLayer getMouth() {
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
