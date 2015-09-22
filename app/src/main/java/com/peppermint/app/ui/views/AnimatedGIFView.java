package com.peppermint.app.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.peppermint.app.R;

import java.io.InputStream;

/**
 * Created by Nuno Luz on 21-09-2015.
 */
public class AnimatedGIFView extends View {

    private Movie mMovie;
    private InputStream mInputStream;
    private long mStart, mTime;
    private float mHeight, mWidth, mMovieWidth, mMovieHeight, mScaleFactor;
    private boolean mIsRunning = false;

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
    }

    private void init(AttributeSet attrs) {
        mInputStream = getContext().getResources().openRawResource(R.raw.record_view);
        mMovie = Movie.decodeStream(mInputStream);
        mMovie.setTime(0);
        mMovieWidth = mMovie.width();
        mMovieHeight = mMovie.height();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.scale(mScaleFactor, mScaleFactor);

        float whDelta = (mWidth - (mMovieWidth * mScaleFactor)) / 2f / mScaleFactor;
        float hwDelta = (mHeight - (mMovieHeight * mScaleFactor)) / 2f / mScaleFactor;

        long now = android.os.SystemClock.uptimeMillis();
        if(mStart == 0) {
            mStart = now;
        }
        mTime = (int)((mTime + now - mStart) % mMovie.duration()) ;
        mMovie.setTime((int) mTime);
        mMovie.draw(canvas, whDelta, hwDelta);

        if(mIsRunning) {
            this.invalidate();
        }

        mStart = now;
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
