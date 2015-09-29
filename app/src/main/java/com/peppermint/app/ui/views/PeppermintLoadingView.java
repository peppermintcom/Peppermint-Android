package com.peppermint.app.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 15-09-2015.
 * Custom loading view for Peppermint.
 */
public class PeppermintLoadingView extends View {

    private static final int DEF_CORNER_RADIUS_DP = 50;
    private static final int DEF_AMPLITUDE_DP = 6;
    private static final int DEF_TEXT_SPACING_DP = 3;

    private static final String DEF_BACKGROUND_COLOR1 = "#ffffff";
    private static final String DEF_BACKGROUND_COLOR2 = "#68c4a3";

    private static final String DEF_PROGRESS_TEXT = "Loading\nContacts...";

    private float mHeight, mWidth;
    private float mCornerRadius;
    private float mCornerLength;

    private float mFullSideLength;
    private float mCenterX, mCenterY;

    private float mProgress = 0;
    private float mAmplitude;
    private String mProgressText;

    private float mHalfEyeSize;
    private float mTextSize;
    private float mTextSpacing;
    private int mBackgroundColor1, mBackgroundColor2;
    private Paint mBackground1Paint, mBackground2Paint, mTextPaint;

    private Typeface mFont;
    private float mLastTime;

    public PeppermintLoadingView(Context context) {
        super(context);
        init(null);
    }

    public PeppermintLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PeppermintLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PeppermintLoadingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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

                mBackgroundColor1 = a.getColor(R.styleable.PeppermintView_backgroundColor1, Color.parseColor(DEF_BACKGROUND_COLOR1));
                mBackgroundColor2 = a.getColor(R.styleable.PeppermintView_backgroundColor2, Color.parseColor(DEF_BACKGROUND_COLOR2));

                mBackground1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mBackground1Paint.setStyle(Paint.Style.FILL);
                mBackground1Paint.setColor(mBackgroundColor1);

                mBackground2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mBackground2Paint.setStyle(Paint.Style.FILL);
                mBackground2Paint.setColor(mBackgroundColor2);

                String textFont = a.getString(R.styleable.PeppermintView_textFont);
                if(textFont == null) {
                    textFont = "fonts/OpenSans-Semibold.ttf";
                }
                mFont = Typeface.createFromAsset(getContext().getAssets(), textFont);
                mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mTextPaint.setStyle(Paint.Style.FILL);
                mTextPaint.setTextAlign(Paint.Align.CENTER);
                mTextPaint.setTypeface(mFont);
                mTextPaint.setColor(mBackgroundColor1);

                mAmplitude = a.getDimensionPixelSize(R.styleable.PeppermintView_amplitude, Utils.dpToPx(getContext(), DEF_AMPLITUDE_DP));
                mTextSpacing = a.getDimensionPixelSize(R.styleable.PeppermintView_textSpacing, Utils.dpToPx(getContext(), DEF_TEXT_SPACING_DP));
                mProgressText = a.getString(R.styleable.PeppermintView_text);
                if(mProgressText == null) {
                    mProgressText = DEF_PROGRESS_TEXT;
                }
            } finally {
                a.recycle();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        this.setMeasuredDimension((int) mWidth, (int) mHeight);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mFullSideLength = (mHeight < mWidth ? mHeight : mWidth);

        mCenterX = mWidth / 2f;
        mCenterY = mHeight / 2f;

        mHalfEyeSize = mFullSideLength / 17f;
        mTextSize = mFullSideLength / 10f;

        mTextPaint.setTextSize(mTextSize);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        float now = (float) android.os.SystemClock.uptimeMillis() / 100f;
        if(mLastTime <= 0) {
            mLastTime = now;
        }
        setProgress(getProgress() + (0.2f * (now - mLastTime)));

        Path fullPath = getRoundRectPath(mCenterX, mCenterY, mCornerRadius, mCornerLength, mFullSideLength);
        Path progressPath = getSinWavePath(mCenterX, mCenterY, mCornerRadius, mCornerLength, mFullSideLength, mAmplitude, mProgress);

        canvas.drawPath(fullPath, mBackground1Paint);
        canvas.drawPath(progressPath, mBackground2Paint);

        Drawable eye = Utils.getDrawable(getContext(), R.drawable.img_logo_eye);
        eye.setColorFilter(mBackground2Paint.getColor(), PorterDuff.Mode.MULTIPLY);

        float xxRef =  (mCenterX - (mFullSideLength/6.5f));
        float yyRef =  (mCenterY - (mFullSideLength/5.5f));
        eye.setBounds((int) (xxRef - mHalfEyeSize), (int) (yyRef - mHalfEyeSize), (int) (xxRef + mHalfEyeSize), (int) (yyRef + mHalfEyeSize));
        eye.draw(canvas);

        xxRef = (mCenterX + (mFullSideLength/6.5f));
        eye.setBounds((int) (xxRef - mHalfEyeSize), (int) (yyRef - mHalfEyeSize), (int) (xxRef + mHalfEyeSize), (int) (yyRef + mHalfEyeSize));
        eye.draw(canvas);

        String[] split = mProgressText.split("\n");
        float offsetY = - (((mTextSize/2f)+(mTextSpacing/2f)) * (float) (split.length - 1));
        for(int i=0; i<split.length; i++) {
            canvas.drawText(split[i], mCenterX, offsetY + mCenterY + (mFullSideLength / 4f) + ((mTextSize + mTextSpacing) * (float) i), mTextPaint);
        }

        mLastTime = now;

        if(getVisibility() == VISIBLE) {
            invalidate();
        }
    }

    private static Path getSinWavePath(float centerX, float centerY, float cornerRadius, float cornerLength, float fullSideLength, float amplitude, float progress) {

        float halfFullSideLength = fullSideLength / 2f;
        float sideLength = fullSideLength - (cornerRadius * 2f);
        float sideHalfLength = sideLength / 2f;

        float xx, yy;

        Path p = new Path();
        p.moveTo(centerX + halfFullSideLength, centerY);
        yy = centerY + sideHalfLength;
        p.lineTo(centerX + halfFullSideLength, yy);

        // bottom right corner
        p.arcTo(new RectF(centerX + sideHalfLength - cornerRadius,  // left
                centerY - halfFullSideLength + sideLength,          // top
                centerX + halfFullSideLength,                       // right
                centerY + halfFullSideLength), 0, 90f);        // bottom

        // bottom line
        xx = centerX - sideHalfLength;
        yy = centerY + halfFullSideLength;
        p.lineTo(xx, yy);

        // bottom left corner
        p.arcTo(new RectF(centerX - halfFullSideLength,             // left
                centerY + sideHalfLength - cornerRadius,            // top
                centerX - sideHalfLength + cornerRadius,            // right
                centerY + halfFullSideLength), 90, 90f);       // bottom

        // left line
        xx = centerX - halfFullSideLength;
        yy = centerY;
        p.lineTo(xx, yy);

        // sin
        float divider = fullSideLength / 10f;
        float unitX = fullSideLength / divider;
        float unitAngle = (float) ((2f*Math.PI) / divider);
        float angle = unitAngle;
        for(float i=0; i<divider; i++) {
            yy = (float) (centerY - (Math.sin(progress + angle) * amplitude));
            p.lineTo(xx + (i * unitX), yy);
            angle += unitAngle;
        }

        p.lineTo(centerX + halfFullSideLength, yy);
        p.close();

        return p;
    }

    private static Path getRoundRectPath(float centerX, float centerY, float cornerRadius, float cornerLength, float fullSideLength) {

        float halfFullSideLength = fullSideLength / 2f;
        float sideLength = fullSideLength - (cornerRadius * 2f);
        float sideHalfLength = sideLength / 2f;

        float xx, yy;

        Path p = new Path();
        yy = centerY - halfFullSideLength;
        p.moveTo(centerX, yy);

        // top right half of line
        xx = centerX + sideHalfLength;
        p.lineTo(xx, yy);

        // top right corner
        p.arcTo(new RectF(centerX + sideHalfLength - cornerRadius,      // left
                yy,                                                     // top
                centerX + sideHalfLength + cornerRadius,                // right
                yy + (cornerRadius * 2f)), -90f, 90f);              // bottom

        // right line
        xx = centerX + halfFullSideLength;
        yy += sideLength + cornerRadius;
        p.lineTo(xx, yy);

        // bottom right corner
        p.arcTo(new RectF(centerX + sideHalfLength - cornerRadius,  // left
                centerY - halfFullSideLength + sideLength,          // top
                centerX + halfFullSideLength,                       // right
                centerY + halfFullSideLength), 0, 90f);        // bottom

        // bottom line
        xx = centerX - sideHalfLength;
        yy = centerY + halfFullSideLength;
        p.lineTo(xx, yy);

        // bottom left corner
        p.arcTo(new RectF(centerX - halfFullSideLength,             // left
                centerY + sideHalfLength - cornerRadius,            // top
                centerX - sideHalfLength + cornerRadius,            // right
                centerY + halfFullSideLength), 90, 90f);       // bottom

        // left line
        xx = centerX - halfFullSideLength;
        yy = centerY + sideHalfLength - sideLength;
        p.lineTo(xx, yy);

        // top left corner
        p.arcTo(new RectF(centerX - halfFullSideLength,                     //left
                centerY - halfFullSideLength,                               // top
                centerX - sideHalfLength + cornerRadius,                    // right
                centerY - sideHalfLength + cornerRadius), 180f, 90f);   // bottom

        // top left half of line
        p.close();

        return p;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float mProgress) {
        this.mProgress = mProgress % (float) (Math.PI * 2f);
    }

    public String getProgressText() {
        return mProgressText;
    }

    public void setProgressText(String mProgressText) {
        this.mProgressText = mProgressText;
    }
}
