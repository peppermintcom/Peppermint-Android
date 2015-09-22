package com.peppermint.app.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Custom recording view for Peppermint to show the progress of the recording.
 */
public class PeppermintRecordView extends View {

    private static final int DEF_DESIRED_SIZE_DP = 150;

    private static final int DEF_CORNER_RADIUS_DP = 50;
    private static final int DEF_PROGRESS_THICKNESS_DP = 10;

    private static final String DEF_PROGRESS_EMPTY_COLOR = "#ffffff";
    private static final String DEF_PROGRESS_FILL_COLOR = "#1f8479";
    private static final String DEF_BACKGROUND_COLOR1 = "#68c4a3";
    private static final String DEF_BACKGROUND_COLOR2 = "#2ebdb2";
    private static final String DEF_PRESSED_BACKGROUND_COLOR1 = "#000000";
    private static final String DEF_PRESSED_BACKGROUND_COLOR2 = "#000000";

    private float mHeight, mWidth;
    private float mCornerRadius;
    private float mCornerLength;

    private float mFullSideLength;
    private float mTotalLength;
    private float mCenterX, mCenterY;

    private float mThickness;
    private float mSeconds;
    private float mProgress;

    private float mHalfEyeSize;
    private int mProgressEmptyColor, mProgressFillColor, mBackgroundColor1, mBackgroundColor2, mPressedBackgroundColor1, mPressedBackgroundColor2;

    private Paint mProgressPaint, mEmptyProgressPaint, mPaintMask, mPaintBackground, mPaintBackgroundPressed;
    private boolean mFillBackground;

    public PeppermintRecordView(Context context) {
        super(context);
        init(null);
    }

    public PeppermintRecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PeppermintRecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PeppermintRecordView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    protected void init(AttributeSet attrs) {
        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                mCornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));
                mCornerLength = (float) (mCornerRadius * 2f * Math.PI / 4f);

                mThickness = a.getDimensionPixelSize(R.styleable.PeppermintView_progressThickness, Utils.dpToPx(getContext(), DEF_PROGRESS_THICKNESS_DP));

                mProgressEmptyColor = a.getColor(R.styleable.PeppermintView_progressEmptyColor, Color.parseColor(DEF_PROGRESS_EMPTY_COLOR));
                mProgressFillColor = a.getColor(R.styleable.PeppermintView_progressFillColor, Color.parseColor(DEF_PROGRESS_FILL_COLOR));
                mBackgroundColor1 = a.getColor(R.styleable.PeppermintView_backgroundColor1, Color.parseColor(DEF_BACKGROUND_COLOR1));
                mBackgroundColor2 = a.getColor(R.styleable.PeppermintView_backgroundColor2, Color.parseColor(DEF_BACKGROUND_COLOR2));
                mPressedBackgroundColor1 = a.getColor(R.styleable.PeppermintView_pressedBackgroundColor1, Color.parseColor(DEF_PRESSED_BACKGROUND_COLOR1));
                mPressedBackgroundColor2 = a.getColor(R.styleable.PeppermintView_pressedBackgroundColor2, Color.parseColor(DEF_PRESSED_BACKGROUND_COLOR2));

                mSeconds = a.getFloat(R.styleable.PeppermintView_seconds, 0);

                mFillBackground = a.getBoolean(R.styleable.PeppermintView_fillBackground, false);

                mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mProgressPaint.setStyle(Paint.Style.FILL);
                mProgressPaint.setColor(mProgressFillColor);

                mEmptyProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mEmptyProgressPaint.setStyle(Paint.Style.FILL);
                mEmptyProgressPaint.setColor(mProgressEmptyColor);

                mPaintMask = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaintMask.setStyle(Paint.Style.FILL);
                mPaintMask.setColor(Color.WHITE);
                mPaintMask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

                mPaintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaintBackground.setStyle(Paint.Style.FILL);

                mPaintBackgroundPressed = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaintBackgroundPressed.setStyle(Paint.Style.FILL);
            } finally {
                a.recycle();
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        if(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            mHeight = Utils.dpToPx(getContext(), DEF_DESIRED_SIZE_DP);
        }
        if(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            mWidth = Utils.dpToPx(getContext(), DEF_DESIRED_SIZE_DP);
        }
        this.setMeasuredDimension((int) mWidth, (int) mHeight);

        mFullSideLength = (mHeight < mWidth ? mHeight : mWidth);
        mTotalLength = ((mFullSideLength - (2f * mCornerRadius)) * 4f) + (mCornerLength * 4f);

        mCenterX = mWidth / 2f;
        mCenterY = mHeight / 2f;

        mHalfEyeSize = mFullSideLength / 17f;

        calculateProgress();
        mPaintBackground.setShader(new LinearGradient(0, 0, mWidth, mHeight, mBackgroundColor1, mBackgroundColor2, Shader.TileMode.MIRROR));
        mPaintBackgroundPressed.setShader(new LinearGradient(0, 0, mWidth, mHeight, mPressedBackgroundColor1, mPressedBackgroundColor2, Shader.TileMode.MIRROR));

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        Path fullPath = getPath(mCenterX, mCenterY, mCornerRadius, mCornerLength, mFullSideLength, mTotalLength);
        Path progressPath = getPath(mCenterX, mCenterY, mCornerRadius, mCornerLength, mFullSideLength, mProgress);
        Path maskPath = getPath(mCenterX, mCenterY, mCornerRadius - mThickness, mCornerLength, mFullSideLength - (mThickness*2f), mTotalLength);

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        int min = (int) (mSeconds / 60f);
        if(min % 2 != 0) {
            canvas.drawPath(fullPath, mProgressPaint);
            canvas.drawPath(progressPath, mEmptyProgressPaint);
        } else {
            canvas.drawPath(fullPath, mEmptyProgressPaint);
            canvas.drawPath(progressPath, mProgressPaint);
        }

        if(isPressed()) {
            canvas.drawPath(maskPath, mPaintBackgroundPressed);
        } else {
            if (mFillBackground) {
                canvas.drawPath(maskPath, mPaintBackground);
            } else {
                canvas.drawPath(maskPath, mPaintMask);
            }
        }

        float angle = (mProgress / mTotalLength * 360f) + 45f;

        Drawable eye = Utils.getDrawable(getContext(), R.drawable.logo_eye);
        Drawable mouth = Utils.getDrawable(getContext(), R.drawable.logo_mouth);

        float xxRef =  (mCenterX - (mFullSideLength/6.5f));
        float yyRef =  (mCenterY - (mFullSideLength/5.5f));
        eye.setBounds((int) (xxRef - mHalfEyeSize), (int) (yyRef - mHalfEyeSize), (int) (xxRef + mHalfEyeSize), (int) (yyRef + mHalfEyeSize));
        canvas.save();
        canvas.rotate(angle, xxRef, yyRef);
        eye.draw(canvas);
        canvas.restore();

        xxRef = (mCenterX + (mFullSideLength/6.5f));
        eye.setBounds((int) (xxRef - mHalfEyeSize), (int) (yyRef - mHalfEyeSize), (int) (xxRef + mHalfEyeSize), (int) (yyRef + mHalfEyeSize));
        canvas.save();
        canvas.rotate(angle, xxRef, yyRef);
        eye.draw(canvas);
        canvas.restore();

        mouth.setBounds((int) (mCenterX - (mFullSideLength / 3.5f)), (int) (mCenterY - (mFullSideLength/15f)), (int) (mCenterX + (mFullSideLength / 3.5f)), (int) (mCenterY + (mFullSideLength/3.5f)));
        mouth.draw(canvas);
    }

    private static Path getPath(float centerX, float centerY, float cornerRadius, float cornerLength, float fullSideLength, float progress) {

        float halfFullSideLength = fullSideLength / 2f;
        float sideLength = fullSideLength - (cornerRadius * 2f);
        float sideHalfLength = sideLength / 2f;

        float xx, yy;
        boolean done = false;

        Path p = new Path();
        p.moveTo(centerX, centerY);
        yy = centerY - halfFullSideLength;
        p.lineTo(centerX, yy);

        // top right half of line
        {
            float tmpProgress = progress < sideHalfLength ? progress : sideHalfLength;
            progress -= tmpProgress;
            xx = centerX + tmpProgress;
            p.lineTo(xx, yy);
        }

        // top right corner
        if(progress > 0) {
            float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
            progress -= cornerLength;
            p.arcTo(new RectF(centerX + sideHalfLength - cornerRadius,      // left
                    yy,                                                     // top
                    centerX + sideHalfLength + cornerRadius,                // right
                    yy + (cornerRadius * 2f)), -90, tmpAngle);              // bottom
        } else {
            p.lineTo(xx, yy + cornerRadius);
            done = true;
        }

        // right line
        if(!done) {
            if (progress > 0) {
                float tmpProgress = progress < sideLength ? progress : sideLength;
                progress -= tmpProgress;
                xx = centerX + halfFullSideLength;
                yy += tmpProgress + cornerRadius;
                p.lineTo(xx, yy);
            } else {
                p.lineTo(centerX + sideHalfLength, yy + cornerRadius);
                done = true;
            }
        }

        // bottom right corner
        if(!done) {
            if (progress > 0) {
                float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
                progress -= cornerLength;
                p.arcTo(new RectF(centerX + sideHalfLength - cornerRadius,  // left
                        centerY - halfFullSideLength + sideLength,          // top
                        centerX + halfFullSideLength,                       // right
                        centerY + halfFullSideLength), 0, tmpAngle);        // bottom
            } else {
                p.lineTo(xx - cornerRadius, yy);
                done = true;
            }
        }

        // bottom line
        if(!done) {
            if (progress > 0) {
                float tmpProgress = progress < sideLength ? progress : sideLength;
                progress -= tmpProgress;
                xx = centerX - sideHalfLength + (sideLength - tmpProgress);
                yy = centerY + halfFullSideLength;
                p.lineTo(xx, yy);
            } else {
                p.lineTo(centerX + sideHalfLength, centerY + sideHalfLength);
                done = true;
            }
        }

        // bottom left corner
        if(!done) {
            if (progress > 0) {
                float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
                progress -= cornerLength;
                p.arcTo(new RectF(centerX - halfFullSideLength,             // left
                        centerY + sideHalfLength - cornerRadius,            // top
                        centerX - sideHalfLength + cornerRadius,            // right
                        centerY + halfFullSideLength), 90, tmpAngle);       // bottom
            } else {
                p.lineTo(xx, yy - cornerRadius);
                done = true;
            }
        }

        // left line
        if(!done) {
            if (progress > 0) {
                float tmpProgress = progress < sideLength ? progress : sideLength;
                progress -= tmpProgress;
                xx = centerX - halfFullSideLength;
                yy = centerY + sideHalfLength - tmpProgress;
                p.lineTo(xx, yy);
            } else {
                p.lineTo(centerX - sideHalfLength, centerY + sideHalfLength);
                done = true;
            }
        }

        // top left corner
        if(!done) {
            if (progress > 0) {
                float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
                progress -= cornerLength;
                p.arcTo(new RectF(centerX - halfFullSideLength,                     //left
                        centerY - halfFullSideLength,                               // top
                        centerX - sideHalfLength + cornerRadius,                    // right
                        centerY - sideHalfLength + cornerRadius), 180, tmpAngle);   // bottom
            } else {
                p.lineTo(xx + cornerRadius, yy);
                done = true;
            }
        }

        // top left half of line
        if(!done) {
            if (progress > 0) {
                xx = centerX - sideHalfLength + progress;
                yy = centerY - halfFullSideLength;
                p.lineTo(xx, yy);
                p.lineTo(xx, yy + cornerRadius);
            } else {
                p.lineTo(centerX - sideHalfLength, centerY - sideHalfLength);
            }
        }

        p.close();

        return p;
    }

    private static float getAngle(float progress, float cornerLength) {
        return 90f * progress / cornerLength;
    }

    private void calculateProgress() {
        this.mProgress = (this.mSeconds % 60f) / 60f * mTotalLength;
    }

    public float getSeconds() {
        return mSeconds;
    }

    public void setSeconds(float mSeconds) {
        this.mSeconds = mSeconds;
        calculateProgress();
        invalidate();
    }
}
