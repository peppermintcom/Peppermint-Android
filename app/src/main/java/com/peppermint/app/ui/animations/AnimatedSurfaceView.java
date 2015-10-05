package com.peppermint.app.ui.animations;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.peppermint.app.ui.animations.CanvasAnimationSet;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * A SurfaceView that allows CanvasAnimations on top of objects.
 * Drawing operations are performed on a background thread through double buffering to improve performance.
 */
public abstract class AnimatedSurfaceView extends SurfaceView {

    private class AnimationThread extends Thread {
        private boolean mRunning = true;

        @Override
        public void run() {
            long startTime = 0;
            while(mRunning) {
                long now = android.os.SystemClock.uptimeMillis();
                if(startTime <= 0) {
                    startTime = now;
                }

                boolean finished = !mAnimationSet.setElapsedTime(now - startTime);
                Canvas canvas = getHolder().lockCanvas();
                mAnimationSet.apply(canvas);
                getHolder().unlockCanvasAndPost(canvas);

                if(finished) {
                    mRunning = false;
                } else {
                    try {
                        sleep(mFrameInterval);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        public boolean isRunning() {
            return this.mRunning;
        }

        public void setRunning(boolean running) {
            this.mRunning = running;
            if(!running) {
                interrupt();
            }
        }
    }

    private long mFrameInterval = 30;   // ~33 fps
    private CanvasAnimationSet mAnimationSet;
    private AnimationThread mAnimationThread;

    public AnimatedSurfaceView(Context context) {
        super(context);
    }

    public AnimatedSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AnimatedSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public abstract void onAsyncDraw(Canvas canvas);

    public void startAnimations() {
        if(mAnimationThread != null) {
            stopAnimations();
        }
        mAnimationThread = new AnimationThread();
        mAnimationThread.start();
    }

    public void stopAnimations() {
        if(mAnimationThread != null) {
            mAnimationThread.setRunning(false);
            mAnimationThread = null;
        }
    }

    public void resetAnimations() {
        mAnimationSet.reset();
    }

    public CanvasAnimationSet getAnimationSet() {
        return mAnimationSet;
    }

    protected void setAnimationSet(CanvasAnimationSet mAnimationSet) {
        this.mAnimationSet = mAnimationSet;
    }
}
