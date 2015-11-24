package com.peppermint.app.ui.canvas;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * A {@link TextureView} that allows animations to be executed through {@link AnimatedLayer}s
 * at the specified frame rate. It can also contain static {@link Layer}s.<br />
 * Drawing operations are performed on a background thread through double buffering
 * to improve performance.<br />
 * The contents of view can be scaled.
 */
public class AnimatedView extends TextureView {

    private static final String TAG = AnimatedView.class.getSimpleName();

    private static final int SCALE_INTERPOLATION_DURATION = 1000;
    private static final int DEF_DESIRED_SIZE_DP = 150;

    private class AnimationThread extends Thread {
        private boolean mRunning = true;

        @Override
        public void run() {
            while(mRunning) {
                doDraw();
                try {
                    sleep(mFrameInterval);
                } catch (InterruptedException e) {
                    // do nothing here
                }
            }
        }

        public boolean isRunning() {
            return this.mRunning;
        }

        public void setRunning(boolean running) {
            synchronized (AnimatedView.this) {
                this.mRunning = running;
                if (!running) {
                    interrupt();
                }
            }
        }
    }

    private TextureView.SurfaceTextureListener mSurfaceCallback = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // draw at least once on initialization
            doDraw();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            doDraw();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            stopDrawingThread();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private AnimatedViewListener mDrawingListener;

    private long mFrameInterval = 30;   // ~33 fps
    private List<Layer> mAnimatedLayerList = new ArrayList<>();
    private AnimationThread mAnimationThread;

    // view size
    private float mWidth, mHeight;

    // scaling variables
    private Interpolator mScaleInterpolator = new AccelerateDecelerateInterpolator();
    private float mScaleFactor = 1f, mScaleDiff = 0, mOldScaleFactor = 1f;
    private long mScaleFrames = 0;

    public AnimatedView(Context context) {
        super(context);
        init(null);
    }

    public AnimatedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AnimatedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AnimatedView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        setSurfaceTextureListener(mSurfaceCallback);
        //setZOrderOnTop(true);
        //setFormat(PixelFormat.TRANSPARENT);
        //setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setOpaque(false);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);

        if(wMode == MeasureSpec.UNSPECIFIED) {
            mWidth = Utils.dpToPx(getContext(), DEF_DESIRED_SIZE_DP);
        }
        if(hMode == MeasureSpec.UNSPECIFIED) {
            mHeight = Utils.dpToPx(getContext(), DEF_DESIRED_SIZE_DP);
        }

        this.setMeasuredDimension((int) mWidth, (int) mHeight);
    }

    protected synchronized void doDraw() {
        Canvas canvas = lockCanvas();
        if (canvas != null) {
            if (mDrawingListener != null) {
                mDrawingListener.onPrepareToDraw(AnimatedView.this);
            }

            if (mScaleFrames > 0) {
                float factor = mScaleInterpolator.getInterpolation((float) mScaleFrames / (float) (SCALE_INTERPOLATION_DURATION / mFrameInterval));
                mScaleFactor = mOldScaleFactor + (mScaleDiff * factor);
                mScaleFrames--;
            }

            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.save();
            canvas.scale(mScaleFactor, mScaleFactor, canvas.getWidth() / 2f, canvas.getHeight() / 2f);

            for(Layer layer : mAnimatedLayerList) {
                layer.draw(AnimatedView.this, canvas);
            }

            canvas.restore();

            unlockCanvasAndPost(canvas);
        }
    }

    /**
     * Reset all animated layers.
     */
    public synchronized void resetAnimations() {
        for(Layer layer : mAnimatedLayerList) {
            if(layer instanceof AnimatedLayer) {
                ((AnimatedLayer) layer).reset();
            }
        }
    }

    /**
     * Start animations for all animated layers. <br />
     * <strong>This doesn't start the drawing thread, which must be launched through
     * {@link #startDrawingThread()}</strong>
     */
    public synchronized void startAnimations() {
        for(Layer layer : mAnimatedLayerList) {
            if(layer instanceof AnimatedLayer) {
                ((AnimatedLayer) layer).start();
            }
        }
    }

    /**
     * Stop animations for all animated layers.
     */
    public synchronized void stopAnimations() {
        for(Layer layer : mAnimatedLayerList) {
            if(layer instanceof AnimatedLayer) {
                ((AnimatedLayer) layer).stop();
            }
        }
    }

    /**
     * Start the thread that draws all layers at the specified frame rate.
     */
    public void startDrawingThread() {
        if(mAnimationThread != null && mAnimationThread.isRunning()) {
            return;
        }

        mAnimationThread = new AnimationThread();
        mAnimationThread.start();
    }

    /**
     * Stop the thread that draws all layers.
     */
    public void stopDrawingThread() {
        if(mAnimationThread != null) {
            mAnimationThread.setRunning(false);
            mAnimationThread = null;
        }
    }

    public boolean isDrawingThreadRunning() {
        return mAnimationThread != null && mAnimationThread.isRunning();
    }

    public List<Layer> getLayers() {
        return mAnimatedLayerList;
    }

    public synchronized void setLayers(List<Layer> mAnimatedLayerList) {
        this.mAnimatedLayerList = mAnimatedLayerList;
    }

    public synchronized void addLayer(Layer layer) {
        this.mAnimatedLayerList.add(layer);
    }

    public synchronized boolean removeLayer(Layer layer) {
        return this.mAnimatedLayerList.remove(layer);
    }

    public synchronized void removeLayers() {
        this.mAnimatedLayerList.clear();
    }

    public AnimatedViewListener getDrawingListener() {
        return mDrawingListener;
    }

    public void setDrawingListener(AnimatedViewListener mDrawingListener) {
        this.mDrawingListener = mDrawingListener;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public synchronized void setScaleFactor(float mScaleFactor) {
        this.mOldScaleFactor = this.mScaleFactor;
        this.mScaleDiff = mScaleFactor - this.mScaleFactor;
        this.mScaleFrames = SCALE_INTERPOLATION_DURATION / mFrameInterval;
    }

    public float getLocalHeight() {
        return mHeight;
    }

    public float getLocalWidth() {
        return mWidth;
    }

    public long getFrameInterval() {
        return mFrameInterval;
    }

    public void setFrameInterval(long mFrameInterval) {
        this.mFrameInterval = mFrameInterval;
    }
}
