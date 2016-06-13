package com.peppermint.app.ui.canvas;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Region;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * A {@link TextureView} that allows animations to be executed through {@link AnimatedLayer}s
 * at the specified frame rate. It can also contain static {@link Layer}s.<br />
 * Drawing operations are performed on a background thread through double buffering
 * to improve performance.<br />
 * The contents of view can be scaled.<br />
 * The view supports a content explosion animation through {@link #startExplosion()}.
 */
public class AnimatedView extends TextureView {

    // explosion animation values
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_EXPLODING = 1;
    private static final int STATUS_EXPLODED = 2;

    private static final int SLICES_HORIZONTAL_AMOUNT = 8;
    private static final int SLICES_VERTICAL_AMOUNT = 8;

    private static final int SLICE_MAX_ROTATION = 90;
    private static final int SLICE_TRANSLATION_X_DP = 30;
    private static final int SLICE_TRANSLATION_Y_DP = 20;
    private static final int SLICE_TRANSLATION_VARIABLE_DP = 20;

    // other values
    private static final int SCALE_INTERPOLATION_DURATION = 1000;
    private static final int DEF_DESIRED_SIZE_DP = 150;

    /**
     * This thread draws each frame of the animation through double buffering.
     */
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

    public interface ExplosionListener {
        void onExplosionFinished();
    }

    /**
     * Represents a slice/fragment/piece in the explosion animation.
     */
    private class BitmapFragment {
        protected Bitmap bmp;
        protected Region.Op op;
        protected Path triangle;
        protected float posVector[] = new float[2];
        protected float transVector[] = new float[2];
        protected int rotVector[] = new int[3];

        private BitmapFragment(Bitmap bmp, float sourceX, float sourceY, Region.Op op, Path tri) {
            this.bmp = bmp;
            this.op = op;
            this.triangle = tri;

            this.posVector[0] = sourceX;
            this.posVector[1] = sourceY;
        }

        public void prepare(){
            final float translationX = Utils.dpToPx(getContext(), SLICE_TRANSLATION_X_DP + mRandomizer.nextInt(SLICE_TRANSLATION_VARIABLE_DP));
            final float translationY = Utils.dpToPx(getContext(), SLICE_TRANSLATION_Y_DP + mRandomizer.nextInt(SLICE_TRANSLATION_VARIABLE_DP));

            // initial translation vector (affected by the gravity vector later on)
            transVector[0] = (((posVector[0] + mSliceWidth) - (getLocalWidth() / 2)) / getLocalWidth()) * translationX;
            transVector[1] = (((posVector[1] + mSliceHeight) - (getLocalHeight() / 2)) / getLocalHeight()) * translationY;

            // rotation vector
            rotVector[0] = mRandomizer.nextInt(SLICE_MAX_ROTATION);
            rotVector[1] = mRandomizer.nextInt(SLICE_MAX_ROTATION);
            rotVector[2] = mRandomizer.nextInt(SLICE_MAX_ROTATION);
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
        public void onSurfaceTextureUpdated(SurfaceTexture surface) { /* nothing to do here */ }
    };

    private AnimatedViewListener mDrawingListener;

    private long mFrameInterval = 30;   // ~33 fps
    private List<Layer> mAnimatedLayerList = new ArrayList<>();
    private AnimationThread mAnimationThread;

    // view size
    private float mWidth, mHeight;
    private float mSliceWidth, mSliceHeight;

    // scaling variables
    private Interpolator mScaleInterpolator = new AccelerateDecelerateInterpolator();
    private float mScaleFactor = 1f, mScaleDiff = 0, mOldScaleFactor = 1f;
    private long mScaleFrames = 0;

    // explosion animation
    private boolean mExplosionEnabled = false;
    private Camera mCamera;
    private Bitmap mBackedBitmap, mExplosionBaseBitmap;
    private Canvas mBackedCanvas, mExplosionBaseCanvas;

    private int mExplosionStatus = STATUS_NORMAL;
    private long mExplosionStartTime;
    private int mExplosionDuration = 800;

    private int mSlicesHorizontalAmount, mSlicesVerticalAmount;
    private ArrayList<BitmapFragment> mExplosionFragments;
    private Interpolator mExplosionInterpolator;

    private Paint mAlphaBitmapPaint;
    private ExplosionListener mExplosionListener;

    private final float[] mGravityVector = { 0, 2f, 0 };
    private static Random mRandomizer;
    protected Context mContext;

    public AnimatedView(Context context) {
        super(context);
        init(context, null);
    }

    public AnimatedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AnimatedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AnimatedView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;

        if (mRandomizer == null) {
            mRandomizer = new Random(SystemClock.uptimeMillis());
        }

        mExplosionInterpolator = new DecelerateInterpolator();
        mCamera = new Camera();

        mAlphaBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mSlicesHorizontalAmount = SLICES_HORIZONTAL_AMOUNT;
        mSlicesVerticalAmount = SLICES_VERTICAL_AMOUNT;

        setSurfaceTextureListener(mSurfaceCallback);
        setOpaque(false);
    }

    public synchronized void startExplosion() {
        if(mExplosionBaseCanvas != null) {
            doDrawLayers(mExplosionBaseCanvas);

            mSliceWidth = (float) mExplosionBaseBitmap.getWidth() / mSlicesHorizontalAmount;
            mSliceHeight = (float) mExplosionBaseBitmap.getHeight() / mSlicesVerticalAmount;

            mBackedBitmap = Bitmap.createBitmap((int) mSliceWidth, (int) mSliceHeight, Bitmap.Config.ARGB_8888);
            mBackedCanvas = new Canvas(mBackedBitmap);

            mExplosionFragments = new ArrayList<>(mSlicesHorizontalAmount * mSlicesVerticalAmount);

            for (int i = 0; i < mSlicesHorizontalAmount; i++) {
                final int x = (int) (i * mSliceWidth);    // slice x pos

                for (int j = 0; j < mSlicesVerticalAmount; j++) {
                    final int y = (int) (j * mSliceHeight);       // slice y pos

                    final Bitmap part = Bitmap.createBitmap(mExplosionBaseBitmap, x, y, (int) mSliceWidth, (int) mSliceHeight);

                    // just use a rectangle/square for now
                    final Path path = new Path();
                    path.moveTo(0, 0);
                    /*for(float jj=0; jj<=1; jj+=.2){
                        path.lineTo(mRandomizer.nextInt(sliceW), sliceH * jj);
                    }*/
                    path.lineTo(mSliceWidth, 0);
                    path.lineTo(mSliceWidth, mSliceHeight);
                    path.lineTo(0, mSliceHeight);
                    path.close();

                    // two halves of the cut (difference and replace)
                    //BitmapFragment diffFragment = new BitmapFragment(part, x, y, w, h, Region.Op.DIFFERENCE, tri);
                    final BitmapFragment replaceFragment = new BitmapFragment(part, x, y, Region.Op.REPLACE, path);

                    //diffFragment.prepare();
                    replaceFragment.prepare();

                    //mExplosionFragments.add(diffFragment);
                    mExplosionFragments.add(replaceFragment);
                }
            }

            mExplosionStatus = STATUS_EXPLODING;
            mExplosionStartTime = -1;
        }
    }

    protected synchronized void resetExplosionCanvas() {
        // init explosion base bitmap (holds a print of the last frame before the explosion)
        // this frame is used to draw static content inside each explosion fragment
        if(mExplosionBaseBitmap != null) {
            mExplosionBaseBitmap.recycle();
        }

        if(mWidth > 0 && mHeight > 0) {
            mExplosionBaseBitmap = Bitmap.createBitmap((int) mWidth, (int) mHeight, Bitmap.Config.ARGB_8888);
            mExplosionBaseCanvas = new Canvas(mExplosionBaseBitmap);
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        final int wMode = MeasureSpec.getMode(widthMeasureSpec);
        final int hMode = MeasureSpec.getMode(heightMeasureSpec);

        if(wMode == MeasureSpec.UNSPECIFIED) {
            mWidth = Utils.dpToPx(getContext(), DEF_DESIRED_SIZE_DP);
        }
        if(hMode == MeasureSpec.UNSPECIFIED) {
            mHeight = Utils.dpToPx(getContext(), DEF_DESIRED_SIZE_DP);
        }

        if(mExplosionEnabled) {
            resetExplosionCanvas();
        }

        this.setMeasuredDimension((int) mWidth, (int) mHeight);
    }

    private synchronized void doDrawLayers(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor, canvas.getWidth() / 2f, canvas.getHeight() / 2f);

        for (Layer layer : mAnimatedLayerList) {
            layer.draw(AnimatedView.this, canvas);
        }

        canvas.restore();
    }

    protected synchronized void doDraw() {
        final Canvas canvas = lockCanvas();
        if (canvas != null) {
            if (mDrawingListener != null) {
                mDrawingListener.onPrepareToDraw(AnimatedView.this);
            }

            if (mScaleFrames > 0) {
                float factor = mScaleInterpolator.getInterpolation((float) mScaleFrames / (float) (SCALE_INTERPOLATION_DURATION / mFrameInterval));
                mScaleFactor = mOldScaleFactor + (mScaleDiff * factor);
                mScaleFrames--;
            }

            if(mExplosionStatus != STATUS_EXPLODED) {

                if(mExplosionStatus == STATUS_EXPLODING){

                    // init start time
                    if(mExplosionStartTime < 0) {
                        mExplosionStartTime = SystemClock.uptimeMillis();
                    }

                    // get elapsed time
                    long delayedTime = SystemClock.uptimeMillis() - mExplosionStartTime;
                    if(delayedTime > mExplosionDuration) {  // this avoids unexpected interpolator values at the end of the animation
                        delayedTime = mExplosionDuration;
                    }

                    // interpolation for explosion progress
                    final float interpolation = mExplosionInterpolator.getInterpolation((float) delayedTime / (float) mExplosionDuration);

                    // cleanup canvas
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    // iterate and draw explosion fragments
                    for (int i=0; i<mExplosionFragments.size(); i++){
                        final BitmapFragment part = mExplosionFragments.get(i);

                        // translation (slightly adjust the vector according to gravity)
                        // interpolation is not being used for translation
                        part.transVector[0] = ((part.transVector[0] - mGravityVector[0]) / 1.15f) + mGravityVector[0];
                        part.transVector[1] += mGravityVector[1];
                        part.posVector[0] = part.posVector[0] + Math.round(part.transVector[0]);
                        part.posVector[1] = part.posVector[1] + Math.round(part.transVector[1]);

                        // rotation (w/ interpolation)
                        int rotateX = Math.round(part.rotVector[0] * interpolation);
                        int rotateY = Math.round(part.rotVector[1] * interpolation);
                        int rotateZ = Math.round(part.rotVector[2] * interpolation);

                        // alpha (w/ interpolation)
                        mAlphaBitmapPaint.setAlpha(255 - Math.round(255f * interpolation));

                        // draw fragment onto backed canvas (this can be skipped for rectangular fragments)
                        // backed canvas allows clipping of the fragment according to part.triangle
                        mBackedCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        mBackedCanvas.save();
                        mBackedCanvas.clipPath(part.triangle, part.op);
                        mBackedCanvas.drawBitmap(part.bmp, 0, 0, null);
                        mBackedCanvas.restore();

                        // apply translation, rotation and alpha transforms + draw the fragment
                        canvas.save();
                        canvas.translate(part.posVector[0], part.posVector[1]);
                        mCamera.save();
                        mCamera.rotateX(rotateX);
                        mCamera.rotateY(rotateY);
                        mCamera.rotateZ(rotateZ);
                        mCamera.applyToCanvas(canvas);
                        mCamera.restore();
                        canvas.drawBitmap(mBackedBitmap, 0, 0, mAlphaBitmapPaint);
                        canvas.restore();
                    }

                    // check if the explosion has finished
                    if(delayedTime >= mExplosionDuration) {
                        mExplosionStatus = STATUS_EXPLODED;
                        if(mExplosionListener != null) {
                            mExplosionListener.onExplosionFinished();
                        }
                    }
                } else {
                    // status is normal so just draw normally
                    doDrawLayers(canvas);
                }
            } else {
                // explosion has finished so don't draw (empty canvas)
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }

            unlockCanvasAndPost(canvas);
        }
    }

    /**
     * Reset all animated layers and explosion status.
     */
    public synchronized void resetAnimations() {
        mExplosionStatus = STATUS_NORMAL;
        mExplosionStartTime = -1;

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

    public synchronized void setDrawingListener(AnimatedViewListener mDrawingListener) {
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

    public ExplosionListener getExplosionListener() {
        return mExplosionListener;
    }

    public void setExplosionListener(ExplosionListener mExplosionListener) {
        this.mExplosionListener = mExplosionListener;
    }

    public synchronized void setExplosionEnabled(boolean mExplosionEnabled) {
        this.mExplosionEnabled = mExplosionEnabled;
        if(mExplosionEnabled) {
            resetExplosionCanvas();
        }
    }

    public boolean isExplosionEnabled() {
        return mExplosionEnabled;
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            stopDrawingThread();
            super.onDetachedFromWindow();
        } catch (Throwable e) {
            TrackerManager.getInstance(getContext().getApplicationContext()).log("Error detaching view! Probable bug of Android 4", e);
        }
    }
}
