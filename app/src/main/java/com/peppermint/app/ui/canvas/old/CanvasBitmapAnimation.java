package com.peppermint.app.ui.canvas.old;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 24-09-2015.
 * 
 * Given the specified FPS value, animates a sequence of bitmaps on a Canvas.
 */
public class CanvasBitmapAnimation {

    private Context mContext;
    private int[] mBitmapSequenceRes;
    private int mCurrentBitmap = 0;
    private boolean mReversed = false;
    private BitmapDrawable[] mDrawables;
    private int mFrameInterval;
    private long mLastTime;
    private Rect mBounds;
    private Paint mPaint;

    public CanvasBitmapAnimation(Context context, int framesPerSecond, Paint paint, int... bitmapSequenceRes) {
        this.mBitmapSequenceRes = bitmapSequenceRes;
        this.mContext = context;
        this.mPaint = paint;
        setFramesPerSecond(framesPerSecond);

        init();
    }

    private void init() {
        mDrawables = new BitmapDrawable[mBitmapSequenceRes.length];
        for(int i=0; i<mBitmapSequenceRes.length; i++) {
            mDrawables[i] = (BitmapDrawable) Utils.getDrawable(mContext, mBitmapSequenceRes[i]);
        }
    }

    public void step(Canvas canvas) {
        long now = android.os.SystemClock.uptimeMillis();
        if(mLastTime <= 0) {
            mLastTime = now;
        }

        canvas.drawBitmap(mDrawables[mCurrentBitmap].getBitmap(), null, mBounds, mPaint);
        //mDrawables[mCurrentBitmap].draw(canvas);

        if(now - mLastTime >= mFrameInterval) {
            if (mReversed) {
                setCurrentFrame(mCurrentBitmap - 1);
            } else {
                setCurrentFrame(mCurrentBitmap + 1);
            }
            mLastTime = now;
        }
    }

    public void setBounds(Rect boundsRect) {
        this.mBounds = boundsRect;
    }

    public void setCurrentFrame(int frame) {
        mCurrentBitmap = frame;
        if(mCurrentBitmap < 0) {
            mCurrentBitmap = 0;
            return;
        }
        if(mCurrentBitmap >= mDrawables.length) {
            mCurrentBitmap = mDrawables.length - 1;
        }
    }

    public boolean isReversed() {
        return mReversed;
    }

    public void reverse(boolean mReversed) {
        this.mReversed = mReversed;
    }

    public void setFramesPerSecond(int mFramesPerSecond) {
        this.mFrameInterval = (int) (1000f / (float) mFramesPerSecond);
    }

    public int getCurrentFrame() {
        return mCurrentBitmap;
    }
}
