package com.peppermint.app.ui.canvas.sound;

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
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.AnimatedLayerSet;
import com.peppermint.app.ui.canvas.AnimatedView;
import com.peppermint.app.ui.canvas.progress.ProgressBoxAnimatedLayer;
import com.peppermint.app.ui.canvas.progress.ProgressEyeAnimatedLayer;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Custom recording view for Peppermint to show the progress of the recording.
 */
public class RecordProgressBarsView extends AnimatedView {

    private static final float DEF_CYCLE_SECONDS = 15f;

    private static final int DEF_CORNER_RADIUS_DP = 50;
    private static final int DEF_PROGRESS_THICKNESS_DP = 10;
    private static final int DEF_EXPLOSION_PADDING_DP = 20;

    private static final String DEF_BORDER_COLOR = "#ffffff";
    private static final String DEF_BACKGROUND_COLOR1 = "#68c4a3";
    private static final String DEF_BACKGROUND_COLOR2 = "#2ebdb2";
    private static final String DEF_PRESSED_BACKGROUND_COLOR1 = "#000000";
    private static final String DEF_PRESSED_BACKGROUND_COLOR2 = "#000000";

    private float mCornerRadius, mProgressThickness;
    private int mBackgroundColor1, mBackgroundColor2, mPressedBackgroundColor1, mPressedBackgroundColor2, mBorderColor;
    private Paint mBackgroundPaint, mBackgroundPressedPaint, mBorderPaint, mBitmapPaint;
    private boolean mFillBackground;
    private float mContentWidth, mContentHeight;

    private LevelBarsAnimatedLayer mRecordBars;
    private ProgressEyeAnimatedLayer mLeftEye, mRightEye;
    private ProgressBoxAnimatedLayer mProgressBox;

    public RecordProgressBarsView(Context context) {
        super(context);
        init(null);
    }

    public RecordProgressBarsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RecordProgressBarsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RecordProgressBarsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    protected void init(AttributeSet attrs) {
        try {
            mCornerRadius = Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP);
            mProgressThickness = Utils.dpToPx(getContext(), DEF_PROGRESS_THICKNESS_DP);

            mBorderColor = Color.parseColor(DEF_BORDER_COLOR);
            mBackgroundColor1 = Color.parseColor(DEF_BACKGROUND_COLOR1);
            mBackgroundColor2 = Color.parseColor(DEF_BACKGROUND_COLOR2);
            mPressedBackgroundColor1 = Color.parseColor(DEF_PRESSED_BACKGROUND_COLOR1);
            mPressedBackgroundColor2 = Color.parseColor(DEF_PRESSED_BACKGROUND_COLOR2);

            if (attrs != null) {
                TypedArray a = getContext().getTheme().obtainStyledAttributes(
                        attrs,
                        R.styleable.PeppermintView,
                        0, 0);

                try {
                    mCornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));
                    mProgressThickness = a.getDimensionPixelSize(R.styleable.PeppermintView_borderWidth, Utils.dpToPx(getContext(), DEF_PROGRESS_THICKNESS_DP));

                    mContentHeight = a.getDimensionPixelSize(R.styleable.PeppermintView_contentHeight, 0);
                    mContentWidth = a.getDimensionPixelSize(R.styleable.PeppermintView_contentWidth, 0);

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

            mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBackgroundPaint.setStyle(Paint.Style.FILL);

            mBackgroundPressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBackgroundPressedPaint.setStyle(Paint.Style.FILL);

            mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBorderPaint.setStyle(Paint.Style.FILL);
            mBorderPaint.setColor(mBorderColor);

            mBitmapPaint = new Paint();
            mBitmapPaint.setAntiAlias(true);
            mBitmapPaint.setFilterBitmap(true);
            mBitmapPaint.setDither(true);

            mRecordBars = new LevelBarsAnimatedLayer(getContext());
            mLeftEye = new ProgressEyeAnimatedLayer(getContext(), (long) (DEF_CYCLE_SECONDS * 1000f), 1000, mBitmapPaint);
            mRightEye = new ProgressEyeAnimatedLayer(getContext(), (long) (DEF_CYCLE_SECONDS * 1000f), 1000, mBitmapPaint);
            mProgressBox = new ProgressBoxAnimatedLayer(getContext(), (long) (DEF_CYCLE_SECONDS * 2000f), true, mCornerRadius, mProgressThickness, mBorderPaint, mBorderPaint, mFillBackground ? mBackgroundPaint : null, mBackgroundPressedPaint);

            mLeftEye.setElapsedTime(7500);
            mRightEye.setElapsedTime(7500);

            AnimatedLayerSet set = new AnimatedLayerSet(getContext());
            set.playTogether(mProgressBox, mRecordBars, mLeftEye, mRightEye);
            addLayer(set);
        }catch (Throwable e) {
            Log.e("TAG", "ERROR", e);
        }
    }

    @Override
    public void startAnimations() {
        mRecordBars.start();
    }

    @Override
    public void stopAnimations() {
        mRecordBars.stop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // explosion padding or content size
        float localHeight = getLocalHeight() - Utils.dpToPx(getContext(), DEF_EXPLOSION_PADDING_DP);
        float localWidth = getLocalHeight() - Utils.dpToPx(getContext(), DEF_EXPLOSION_PADDING_DP);

        if(mContentHeight != 0 && mContentWidth != 0) {
            localHeight = mContentHeight;
            localWidth = mContentWidth;
        }

        float paddingHeight = (getLocalHeight() - localHeight) / 2f;
        float paddingWidth = (getLocalWidth() - localWidth) / 2f;

        // background gradients
        mBackgroundPaint.setShader(new LinearGradient(0, 0, localWidth, localHeight, mBackgroundColor1, mBackgroundColor2, Shader.TileMode.MIRROR));
        mBackgroundPressedPaint.setShader(new LinearGradient(0, 0, localWidth, localHeight, mPressedBackgroundColor1, mPressedBackgroundColor2, Shader.TileMode.MIRROR));

        // box bounds
        Rect fullBounds = new Rect((int) paddingWidth, (int) paddingHeight,  Math.round(getLocalWidth() - paddingWidth), Math.round(getLocalHeight() - paddingHeight));
        mProgressBox.setBounds(fullBounds);

        // eye bounds
        final float eyeXFactor = 7f;
        final float eyeYFactor = 5.5f;
        float fullSideLength = (localWidth > localHeight ? localHeight : localWidth);
        int eyeRadius = (int) (fullSideLength / 18f);
        int eyeCenterX = (int) (fullBounds.centerX() - (fullSideLength / eyeXFactor));
        int eyeCenterY = (int) (fullBounds.centerY() - (fullSideLength / eyeYFactor));
        Rect leftEyeBounds = new Rect(eyeCenterX - eyeRadius, eyeCenterY - eyeRadius, eyeCenterX + eyeRadius, eyeCenterY + eyeRadius);
        mLeftEye.setBounds(leftEyeBounds);

        Rect rightEyeBounds = new Rect(leftEyeBounds);
        rightEyeBounds.offset((int) (fullSideLength / (eyeXFactor / 2f)), 0);
        mRightEye.setBounds(rightEyeBounds);

        // mouth bounds1
        /*final float mouthXFactor = 3.5f;
        final float mouthYFactor = 14f;
        Rect mouthBounds = new Rect((int) (fullBounds.centerX() - (fullSideLength / mouthXFactor)), (int) (fullBounds.centerY() - (fullSideLength / mouthYFactor)),
                (int) (fullBounds.centerX() + (fullSideLength / mouthXFactor)), (int) (fullBounds.centerY() + (fullSideLength / mouthXFactor)));*/
        float margin = fullBounds.width() / 5f;
        Rect barsBounds = new Rect((int) (fullBounds.left + mProgressThickness + margin), (int) (fullBounds.top + mProgressThickness + (fullBounds.height()/2.5f)),
                (int) (fullBounds.right - mProgressThickness - margin), (int) (fullBounds.bottom - mProgressThickness - (fullBounds.height() / 10f)));
        mRecordBars.setBounds(barsBounds);
    }

    public void blink() {
        mLeftEye.blink();
        mRightEye.blink();
    }

    public void blinkLeftEye() {
        mLeftEye.blink();
    }

    public void blinkRightEye() {
        mRightEye.blink();
    }

    public LevelBarsAnimatedLayer getBars() {
        return mRecordBars;
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
