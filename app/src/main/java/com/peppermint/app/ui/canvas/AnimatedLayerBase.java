package com.peppermint.app.ui.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * Abstract implementation of an {@link AnimatedLayer}.
 */
public abstract class AnimatedLayerBase extends LayerBase implements AnimatedLayer {

    private List<AnimatedLayerListener> mListenerList = new ArrayList<>();
    private Interpolator mInterpolator;

    private double mStartTime = 0, mLastTime = 0, mElapsedTime = 0, mDuration;
    private boolean mReversed = false, mLooping = false;
    private boolean mRunning = false;

    public AnimatedLayerBase(Context context) {
        super(context);
    }

    public AnimatedLayerBase(Context context, Interpolator interpolator, long duration) {
        this(context);
        this.mInterpolator = interpolator;
        this.mDuration = duration;
    }

    @Override
    public void start() {
        this.mRunning = true;
        this.mLastTime = 0;
        triggerAnimationStarted();
    }

    @Override
    public void stop() {
        this.mRunning = false;
        triggerAnimationEnded();
    }

    @Override
    public void reset() {
        this.mStartTime = 0;
        this.mLastTime = 0;
        this.mElapsedTime = 0;
    }

    @Override
    public void draw(View view, Canvas canvas) {
        if(mRunning) {
            final long now = android.os.SystemClock.uptimeMillis();
            if (mStartTime <= 0) {
                mStartTime = now;
            }
            if (mLastTime <= 0) {
                mLastTime = now;
            }

            //setElapsedTime(isReversed() ? mDuration - (now - mStartTime) : now - mStartTime);
            addElapsedTime(now - mLastTime);

            mLastTime = now;
        }

        onDraw(view, canvas, getInterpolatedElapsedTime());
    }

    protected void onDraw(View view, Canvas canvas, double interpolatedElapsedTime) {
        // nothing to do here; this must be overriden
    }

    public boolean isRunning() {
        return mRunning;
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
        long elapsedTime = (long) getElapsedTime();
        if(mInterpolator != null) {
            elapsedTime = (long) (getElapsedTime() * getInterpolator().getInterpolation((float) (getElapsedTime() / getDuration())));
        }

        if(elapsedTime > getDuration()) {
            return (long) getDuration();
        }
        return elapsedTime;
    }

    @Override
    public double getElapsedTime() {
        return mElapsedTime;
    }

    @Override
    public boolean addElapsedTime(double value) {
        return setElapsedTime(mElapsedTime + ((isReversed() ? -1 : 1) * value));
    }

    @Override
    public boolean setElapsedTime(double elapsedTime) {
        mElapsedTime = elapsedTime;

        if (mElapsedTime > mDuration) {
            if(!mLooping) {
                mElapsedTime = mDuration;
                stop();
                return false;
            }

            mElapsedTime = mElapsedTime != mDuration ? mElapsedTime % mDuration : mDuration;
        } else if(mReversed && mElapsedTime < 0) {
            if(!mLooping) {
                this.mElapsedTime = 0;
                stop();
                return false;
            }

            final double rest = mElapsedTime % mDuration;
            mElapsedTime = mDuration + (rest != 0 ? rest : mDuration);
        }

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
    public boolean isReversed() {
        return mReversed;
    }

    @Override
    public void setReversed(boolean reversed) {
        this.mReversed = reversed;
    }

    @Override
    public boolean isLooping() {
        return mLooping;
    }

    @Override
    public void setLooping(boolean looping) {
        this.mLooping = looping;
    }

    public void addAnimationListener(AnimatedLayerListener mListener) {
        if(!mListenerList.contains(mListener)) {
            mListenerList.add(mListener);
        }
    }

    public boolean removeAnimationListener(AnimatedLayerListener mListener) {
        return mListenerList.contains(mListener) && mListenerList.remove(mListener);
    }

    protected void triggerAnimationStarted() {
        for(AnimatedLayerListener listener : mListenerList) {
            listener.onAnimationStarted(this);
        }
    }

    protected void triggerAnimationEnded() {
        for(AnimatedLayerListener listener : mListenerList) {
            listener.onAnimationEnded(this);
        }
    }

}
