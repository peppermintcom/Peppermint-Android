package com.peppermint.app.ui.canvas.progress;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.AnimatedView;
import com.peppermint.app.ui.canvas.BitmapLayer;
import com.peppermint.app.ui.canvas.TextLayer;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Custom recording view for Peppermint to show the progress of the recording.
 */
public class LoadingView extends AnimatedView {

    private static final int DEF_CORNER_RADIUS_DP = 50;
    private static final int DEF_AMPLITUDE_DP = 6;
    private static final int DEF_TEXT_SPACING_DP = 3;

    private static final String DEF_BACKGROUND_COLOR1 = "#ffffff";
    private static final String DEF_BACKGROUND_COLOR2 = "#68c4a3";

    private static final String DEF_PROGRESS_TEXT = "Loading\nContacts...";

    private float mCornerRadius;

    private float mProgress = 0;
    private float mAmplitude;
    private String mProgressText;

    private float mTextSize;
    private float mTextSpacing;
    private int mBackgroundColor1, mBackgroundColor2;
    private Paint mBackground1Paint, mBackground2Paint, mTextPaint, mBitmapPaint;

    private Typeface mFont;

    private TextLayer mTextLayer;
    private BitmapLayer mLeftEye, mRightEye;
    private ProgressBoxAnimatedLayer mProgressBox;

    public LoadingView(Context context) {
        super(context);
        init(null);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    protected void init(AttributeSet attrs) {
        mCornerRadius = Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP);

        mBackgroundColor1 = Color.parseColor(DEF_BACKGROUND_COLOR1);
        mBackgroundColor2 = Color.parseColor(DEF_BACKGROUND_COLOR2);
        mAmplitude = Utils.dpToPx(getContext(), DEF_AMPLITUDE_DP);
        mTextSpacing = Utils.dpToPx(getContext(), DEF_TEXT_SPACING_DP);
        mProgressText = DEF_PROGRESS_TEXT;

        String textFont = "fonts/OpenSans-Semibold.ttf";

        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                mCornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));

                mBackgroundColor1 = a.getColor(R.styleable.PeppermintView_backgroundColor1, Color.parseColor(DEF_BACKGROUND_COLOR1));
                mBackgroundColor2 = a.getColor(R.styleable.PeppermintView_backgroundColor2, Color.parseColor(DEF_BACKGROUND_COLOR2));

                String aTextFont = a.getString(R.styleable.PeppermintView_textFont);
                if(aTextFont != null) {
                    textFont = aTextFont;
                }

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

        mBackground1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackground1Paint.setStyle(Paint.Style.FILL);
        mBackground1Paint.setColor(mBackgroundColor1);

        mBackground2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackground2Paint.setStyle(Paint.Style.FILL);
        mBackground2Paint.setColor(mBackgroundColor2);

        mBitmapPaint = new Paint();
        mBitmapPaint.setColorFilter(new PorterDuffColorFilter(mBackground2Paint.getColor(), PorterDuff.Mode.MULTIPLY));
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setFilterBitmap(true);
        mBitmapPaint.setDither(true);

        mFont = Typeface.createFromAsset(getContext().getAssets(), textFont);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(mFont);
        mTextPaint.setColor(mBackgroundColor1);
        mTextPaint.setTextSize(mTextSize);

        mTextLayer = new TextLayer(getContext(), mTextSize, mTextSpacing, mTextPaint);
        mTextLayer.setText(mProgressText);
        mLeftEye = new BitmapLayer(getContext(), R.drawable.img_logo_eye, mBitmapPaint);
        mRightEye = new BitmapLayer(getContext(), R.drawable.img_logo_eye, mBitmapPaint);
        mProgressBox = new ProgressBoxAnimatedLayer(getContext(), 360 * 33, true, mCornerRadius, mAmplitude, mBackground2Paint, mBackground1Paint, mBackground1Paint, null);
        mProgressBox.setProgressType(ProgressBoxAnimatedLayer.PROGRESS_WAVE);

        addLayer(mProgressBox);
        addLayer(mLeftEye);
        addLayer(mRightEye);
        addLayer(mTextLayer);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        float smallerSide = getLocalHeight() > getLocalWidth() ? getLocalWidth() : getLocalHeight();

        mTextSize = smallerSide / 10f;
        mTextPaint.setTextSize(mTextSize);
        mTextLayer.setTextSize(mTextSize);

        // box bounds
        Rect fullBounds = new Rect(0, 0, (int) getLocalWidth(), (int) getLocalHeight());
        mProgressBox.setBounds(fullBounds);

        // eye bounds
        final float eyeXFactor = 7f;
        final float eyeYFactor = 5.5f;
        int eyeRadius = (int) (smallerSide / 18f);
        int eyeCenterX = (int) (fullBounds.centerX() - (smallerSide / eyeXFactor));
        int eyeCenterY = (int) (fullBounds.centerY() - (smallerSide / eyeYFactor));
        Rect leftEyeBounds = new Rect(eyeCenterX - eyeRadius, eyeCenterY - eyeRadius, eyeCenterX + eyeRadius, eyeCenterY + eyeRadius);
        mLeftEye.setBounds(leftEyeBounds);

        Rect rightEyeBounds = new Rect(leftEyeBounds);
        rightEyeBounds.offset((int) (smallerSide / (eyeXFactor / 2f)), 0);
        mRightEye.setBounds(rightEyeBounds);

        // text bounds
        int textY = (int) ((getLocalHeight() / 2f) + (smallerSide / 4f));
        mTextLayer.setBounds(new Rect(0, (int) (textY - mTextSize), (int) getLocalWidth(), (int) (textY + mTextSize)));
    }

    public ProgressBoxAnimatedLayer getProgressBox() {
        return mProgressBox;
    }

    public float getProgress() {
        return mProgress;
    }

    public synchronized void setProgress(float mProgress) {
        this.mProgress = mProgress % (float) (Math.PI * 2f);
    }

    public String getProgressText() {
        return mProgressText;
    }

    public synchronized void setProgressText(String mProgressText) {
        this.mProgressText = mProgressText;
    }
}
