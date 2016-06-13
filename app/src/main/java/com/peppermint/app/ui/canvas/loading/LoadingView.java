package com.peppermint.app.ui.canvas.loading;

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

    private float mProgress = 0;
    private String mProgressText;

    private float mTextSize;
    private Paint mTextPaint;

    private TextLayer mTextLayer;
    private BitmapLayer mLeftEye, mRightEye;
    private LoadingBoxAnimatedLayer mProgressBox;
    private Rect mFullBounds = new Rect();
    private Rect mLeftEyeBounds = new Rect(), mRightEyeBounds = new Rect();
    private Rect mTextBounds = new Rect();

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
        int cornerRadius = Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP);

        int backgroundColor1 = Color.parseColor(DEF_BACKGROUND_COLOR1);
        int backgroundColor2 = Color.parseColor(DEF_BACKGROUND_COLOR2);
        int amplitude = Utils.dpToPx(getContext(), DEF_AMPLITUDE_DP);
        int textSpacing = Utils.dpToPx(getContext(), DEF_TEXT_SPACING_DP);
        mProgressText = DEF_PROGRESS_TEXT;

        String textFont = getContext().getString(R.string.font_semibold);

        if(attrs != null) {
            final TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                cornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));

                backgroundColor1 = a.getColor(R.styleable.PeppermintView_backgroundColor1, Color.parseColor(DEF_BACKGROUND_COLOR1));
                backgroundColor2 = a.getColor(R.styleable.PeppermintView_backgroundColor2, Color.parseColor(DEF_BACKGROUND_COLOR2));

                final String aTextFont = a.getString(R.styleable.PeppermintView_textFont);
                if(aTextFont != null) {
                    textFont = aTextFont;
                }

                amplitude = a.getDimensionPixelSize(R.styleable.PeppermintView_amplitude, Utils.dpToPx(getContext(), DEF_AMPLITUDE_DP));
                textSpacing = a.getDimensionPixelSize(R.styleable.PeppermintView_textSpacing, Utils.dpToPx(getContext(), DEF_TEXT_SPACING_DP));
                mProgressText = a.getString(R.styleable.PeppermintView_text);
                if(mProgressText == null) {
                    mProgressText = DEF_PROGRESS_TEXT;
                }
            } finally {
                a.recycle();
            }
        }

        final Paint background1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        background1Paint.setStyle(Paint.Style.FILL);
        background1Paint.setColor(backgroundColor1);

        final Paint background2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        background2Paint.setStyle(Paint.Style.FILL);
        background2Paint.setColor(backgroundColor2);

        final Paint bitmapPaint = new Paint();
        bitmapPaint.setColorFilter(new PorterDuffColorFilter(background2Paint.getColor(), PorterDuff.Mode.MULTIPLY));
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);

        final Typeface font = Typeface.createFromAsset(getContext().getAssets(), textFont);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(font);
        mTextPaint.setColor(backgroundColor1);
        mTextPaint.setTextSize(mTextSize);

        mTextLayer = new TextLayer(getContext(), mTextSize, textSpacing, mTextPaint);
        mTextLayer.setText(mProgressText);
        mLeftEye = new BitmapLayer(getContext(), R.drawable.img_logo_eye, bitmapPaint);
        mRightEye = new BitmapLayer(getContext(), R.drawable.img_logo_eye, bitmapPaint);
        mProgressBox = new LoadingBoxAnimatedLayer(getContext(), 360 * 33, true, cornerRadius, amplitude, background2Paint, background1Paint, background1Paint, null);
        mProgressBox.setProgressType(LoadingBoxAnimatedLayer.PROGRESS_WAVE);

        addLayer(mProgressBox);
        addLayer(mLeftEye);
        addLayer(mRightEye);
        addLayer(mTextLayer);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final float smallerSide = getLocalHeight() > getLocalWidth() ? getLocalWidth() : getLocalHeight();

        mTextSize = smallerSide / 10f;
        mTextPaint.setTextSize(mTextSize);
        mTextLayer.setTextSize(mTextSize);

        // box bounds
        mFullBounds.set(0, 0, (int) getLocalWidth(), (int) getLocalHeight());
        mProgressBox.setBounds(mFullBounds);

        // eye bounds
        final float eyeXFactor = 7f;
        final float eyeYFactor = 5.5f;
        final int eyeRadius = (int) (smallerSide / 18f);
        final int eyeCenterX = (int) (mFullBounds.centerX() - (smallerSide / eyeXFactor));
        final int eyeCenterY = (int) (mFullBounds.centerY() - (smallerSide / eyeYFactor));
        mLeftEyeBounds.set(eyeCenterX - eyeRadius, eyeCenterY - eyeRadius, eyeCenterX + eyeRadius, eyeCenterY + eyeRadius);
        mLeftEye.setBounds(mLeftEyeBounds);

        mRightEyeBounds.set(mLeftEyeBounds);
        mRightEyeBounds.offset((int) (smallerSide / (eyeXFactor / 2f)), 0);
        mRightEye.setBounds(mRightEyeBounds);

        // text bounds
        final int textY = (int) ((getLocalHeight() / 2f) + (smallerSide / 4f));
        mTextBounds.set(0, (int) (textY - mTextSize), (int) getLocalWidth(), (int) (textY + mTextSize));
        mTextLayer.setBounds(mTextBounds);
    }

    public LoadingBoxAnimatedLayer getProgressBox() {
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
