package com.peppermint.app.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

import java.io.InputStream;

/**
 * Created by Nuno Luz on 21-09-2015.
 *
 * A view that plays an animated GIF resource.
 */
public class AnimatedGIFView extends View {

    private Movie mMovie;
    private InputStream mInputStream;
    private long mStart, mTime;
    private float mHeight, mWidth, mMovieWidth, mMovieHeight, mScaleFactor;
    private float mPressedHeight, mPressedWidth, mPressedScaleFactor;
    private boolean mIsRunning = false;
    private BitmapDrawable mOnPressedDrawable;

    public AnimatedGIFView(Context context) {
        super(context);
        init(null);
    }

    public AnimatedGIFView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AnimatedGIFView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AnimatedGIFView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        this.setMeasuredDimension((int) mWidth, (int) mHeight);
        mScaleFactor = (mWidth-mMovieWidth) > (mHeight-mMovieHeight) ? (mHeight / (float) mMovie.height()) : (mWidth / (float) mMovie.width());

        if(mOnPressedDrawable != null) {
            mPressedHeight = mOnPressedDrawable.getBitmap().getHeight();
            mPressedWidth = mOnPressedDrawable.getBitmap().getWidth();
            mPressedScaleFactor = (mWidth-mPressedWidth) > (mHeight-mPressedHeight) ? (mHeight / mPressedHeight) : (mWidth / mPressedWidth);
            float whDelta = (mWidth - (mPressedWidth * mPressedScaleFactor)) / 2f;
            float hwDelta = (mHeight - (mPressedHeight * mPressedScaleFactor)) / 2f;
            mOnPressedDrawable.setBounds((int) whDelta, (int) hwDelta, (int) (mWidth-whDelta), (int) (mHeight-hwDelta));
        }
    }

    private void init(AttributeSet attrs) {
        //mInputStream = getContext().getResources().openRawResource(R.raw.record_view);
        mMovie = Movie.decodeStream(mInputStream);
        mMovie.setTime(0);
        mMovieWidth = mMovie.width();
        mMovieHeight = mMovie.height();

        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                mOnPressedDrawable = (BitmapDrawable) a.getDrawable(R.styleable.PeppermintView_drawablePressed);
            } finally {
                a.recycle();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mOnPressedDrawable != null && isPressed()) {
            mOnPressedDrawable.draw(canvas);
        } else {
            canvas.scale(mScaleFactor, mScaleFactor);

            float whDelta = (mWidth - (mMovieWidth * mScaleFactor)) / 2f / mScaleFactor;
            float hwDelta = (mHeight - (mMovieHeight * mScaleFactor)) / 2f / mScaleFactor;

            long now = android.os.SystemClock.uptimeMillis();
            if (mStart == 0) {
                mStart = now;
            }
            mTime = (int) ((mTime + now - mStart) % mMovie.duration());
            mMovie.setTime((int) mTime);
            mMovie.draw(canvas, whDelta, hwDelta);

            if (mIsRunning) {
                this.invalidate();
            }

            mStart = now;
        }
    }

    public void start() {
        mIsRunning = true;
        mStart = 0;
        invalidate();
    }

    public void stop() {
        mIsRunning = false;
    }

    public void reset() {
        mTime = 0;
    }
}
