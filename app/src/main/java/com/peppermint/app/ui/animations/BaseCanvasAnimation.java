package com.peppermint.app.ui.animations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * Base abstract class for CanvasAnimations.
 */
public abstract class BaseCanvasAnimation implements CanvasAnimation {

    private float mFramesPerSecond = 25;
    private Interpolator mInterpolator;
    private double mStartTime = 0, mElapsedTime = 0, mDuration;
    private boolean mReversed = false, mLooping = false;
    private long mLoop = 0;
    private Rect mBounds;
    private Context mContext;
    private float mHalfBoundsWidth, mHalfBoundsHeight;
    private boolean mRunning = false;
    private List<CanvasAnimationListener> mListenerList = new ArrayList<>();

    public BaseCanvasAnimation(Context context) {
        this.mContext = context;
    }

    public BaseCanvasAnimation(Context context, Interpolator interpolator, long duration) {
        this(context);
        this.mInterpolator = interpolator;
        this.mDuration = duration;
    }

    protected long getFrameInterval() {
        return (long) (1000f / mFramesPerSecond);
    }

    @Override
    public float getFramesPerSecond() {
        return mFramesPerSecond;
    }

    @Override
    public void setFramesPerSecond(float fps) {
        this.mFramesPerSecond = fps;
    }

    @Override
    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    @Override
    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public long getInterpolatedElapsedTime() {
        return (long) (getElapsedTime() * getInterpolator().getInterpolation((float) (getElapsedTime() / getDuration())));
    }

    @Override
    public double getElapsedTime() {
        return mElapsedTime;
    }

    @Override
    public boolean setElapsedTime(double elapsedTime) {
        if(!mLooping && elapsedTime > mDuration) {
            this.mElapsedTime = mDuration;
            stop();
            return false;
        }
        this.mElapsedTime = elapsedTime % mDuration;
        this.mLoop = (long) (elapsedTime / mDuration);
        return true;
    }

    @Override
    public double getDuration() {
        return mDuration;
    }

    @Override
    public void setDuration(double duration) {
        this.mDuration = duration;
    }

    @Override
    public void reset() {
        mStartTime = 0;
        mLoop = 0;
        mElapsedTime = 0;
    }

    @Override
    public boolean isReversed() {
        return mReversed;
    }

    @Override
    public void setReversed(boolean reversed) {
        this.mReversed = reversed;
    }

    @Override
    public Rect getBounds() {
        return mBounds;
    }

    @Override
    public void setBounds(Rect mBounds) {
        this.mBounds = mBounds;
        this.mHalfBoundsHeight = mBounds.height() / 2f;
        this.mHalfBoundsWidth = mBounds.width() / 2f;
    }

    public float getHalfBoundsWidth() {
        return mHalfBoundsWidth;
    }

    public float getHalfBoundsHeight() {
        return mHalfBoundsHeight;
    }

    @Override
    public boolean isLooping() {
        return mLooping;
    }

    @Override
    public void setLooping(boolean looping) {
        this.mLooping = looping;
    }

    public long getLoop() {
        return mLoop;
    }

    public Context getContext() {
        return mContext;
    }

    public void addAnimationListener(CanvasAnimationListener mListener) {
        if(!mListenerList.contains(mListener)) {
            mListenerList.add(mListener);
        }
    }

    public boolean removeAnimationListener(CanvasAnimationListener mListener) {
        return mListenerList.contains(mListener) && mListenerList.remove(mListener);
    }

    protected void triggerAnimationStarted() {
        for(CanvasAnimationListener listener : mListenerList) {
            listener.onAnimationStarted(this);
        }
    }

    protected void triggerAnimationEnded() {
        for(CanvasAnimationListener listener : mListenerList) {
            listener.onAnimationEnded(this);
        }
    }

    protected void triggerAnimationApplied() {
        for(CanvasAnimationListener listener : mListenerList) {
            listener.onAnimationApplied(this);
        }
    }

    @Override
    public void start() {
        this.mRunning = true;
        this.mStartTime = mStartTime + mElapsedTime;
        triggerAnimationStarted();
    }

    @Override
    public void stop() {
        this.mRunning = false;
        triggerAnimationEnded();
    }

    @Override
    public void apply(Canvas canvas) {
        if(mRunning) {
            long now = android.os.SystemClock.uptimeMillis();
            if (mStartTime <= 0) {
                mStartTime = now;
            }
            setElapsedTime(now - mStartTime);
            triggerAnimationApplied();
        }
        apply(canvas, getInterpolatedElapsedTime());
    }

    protected abstract void apply(Canvas canvas, double interpolatedElapsedTime);
}
