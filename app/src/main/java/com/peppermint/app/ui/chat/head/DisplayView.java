package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.ResourceUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 19-03-2016.
 *
 * Base class for handling overlay Views in a {@link WindowManager}.
 */
public class DisplayView<T extends View> implements Display.OnDisplaySizeObtainedListener {

    public interface OnInteractionListener<T extends View> {
        void onAtRest(DisplayView<T> windowManagerView);
        boolean onClick(DisplayView<T> windowManagerView);
        boolean onDragStarted(DisplayView<T> windowManagerView, float offsetX, float offsetY, MotionEvent event);
        boolean onDrag(DisplayView<T> windowManagerView, float offsetX, float offsetY, MotionEvent event);
        boolean onDragFinished(DisplayView<T> windowManagerView, float[] velocity, MotionEvent event);
    }

    public static final SpringConfig SPRING_CONFIG_STRENGTH_MEDIUM = SpringConfig.fromOrigamiTensionAndFriction(100, 10);

    protected Context mContext;
    protected Display mDisplay;
    protected WindowManager mWindowManager;

    // views
    protected boolean mTouchable = false;
    protected boolean mVisible = false;
    protected T mView;
    protected WindowManager.LayoutParams mViewLayoutParams, mViewOriginalLayoutParams;

    // rebound
    protected SpringSystem mSpringSystem;
    protected Spring mSpringX, mSpringY;
    private List<OnInteractionListener<T>> mOnInteractionListeners = new ArrayList<>();

    private SpringListener mSpringListener = new SimpleSpringListener() {
        public void onSpringUpdate(Spring spring) {
            if(spring == mSpringX) {
                setViewPosition((int) spring.getCurrentValue(), getViewPositionY(), true);
            } else {
                setViewPosition(getViewPositionX(), (int) spring.getCurrentValue(), true);
            }
        }

        @Override
        public void onSpringAtRest(Spring spring) {
            if(spring == mSpringX) {
                for(OnInteractionListener<T> onInteractionListener : mOnInteractionListeners) {
                    onInteractionListener.onAtRest(DisplayView.this);
                }
            }
        }
    };

    // gestures
    private CustomGestureDetector mCustomGestureDetector;
    private final CustomGestureDetector.OnGestureListener mOnGestureListener = new CustomGestureDetector.OnGestureListener() {
        @Override
        public boolean onClick(View view) {
            int listenerSize = mOnInteractionListeners.size();
            boolean stopIteration = false;
            for (int i = 0; i < listenerSize && !stopIteration; i++) {
                stopIteration = mOnInteractionListeners.get(i).onClick(DisplayView.this);
            }
            return false;
        }

        @Override
        public boolean onDragStarted(View view, float offsetX, float offsetY, MotionEvent event) {
            int listenerSize = mOnInteractionListeners.size();
            boolean stopIteration = false;
            for (int i = 0; i < listenerSize && !stopIteration; i++) {
                stopIteration = mOnInteractionListeners.get(i).onDragStarted(DisplayView.this, offsetX, offsetY, event);
            }
            return false;
        }

        @Override
        public boolean onDrag(View view, float offsetX, float offsetY, MotionEvent event) {
            int listenerSize = mOnInteractionListeners.size();
            boolean stopIteration = false;
            for (int i = 0; i < listenerSize && !stopIteration; i++) {
                stopIteration = mOnInteractionListeners.get(i).onDrag(DisplayView.this, offsetX, offsetY, event);
            }
            return false;
        }

        @Override
        public boolean onDragFinished(View view, float[] velocity, MotionEvent event) {
            int listenerSize = mOnInteractionListeners.size();
            boolean stopIteration = false;
            for (int i = 0; i < listenerSize && !stopIteration; i++) {
                stopIteration = mOnInteractionListeners.get(i).onDragFinished(DisplayView.this, velocity, event);
            }
            return false;
        }
    };

    public DisplayView(Context mContext, Display mDisplay) {
        this.mContext = mContext;
        this.mDisplay = mDisplay;
        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        this.mCustomGestureDetector = new CustomGestureDetector(mContext);
        this.mCustomGestureDetector.addOnGestureListener(mOnGestureListener);

        try {
            Field f = mWindowManager.getClass().getDeclaredField("mParentWindow");
            f.setAccessible(true);
            Window window = (Window) f.get(mWindowManager);
            if(window != null) {
                window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            }
        } catch (Exception e) {
            TrackerManager.getInstance(mContext).logException(e);
        }
    }

    public DisplayView(Context mContext, Display mDisplay, SpringSystem mSpringSystem) {
        this(mContext, mDisplay);

        this.mSpringSystem = mSpringSystem;

        if(mSpringSystem != null) {
            mSpringX = mSpringSystem.createSpring();
            mSpringY = mSpringSystem.createSpring();
            mSpringX.setSpringConfig(SPRING_CONFIG_STRENGTH_MEDIUM);
            mSpringY.setSpringConfig(SPRING_CONFIG_STRENGTH_MEDIUM);
            mSpringX.addListener(mSpringListener);
            mSpringY.addListener(mSpringListener);
        }
    }

    public void init() {
        if(mDisplay.isGotSize()) {
            onDisplaySizeObtained(0, 0, mDisplay.getDisplayWidth(), mDisplay.getDisplayHeight());
        }
        mDisplay.addOnDisplaySizeObtainedListener(this);
    }

    public void deinit() {
        mDisplay.removeOnDisplaySizeObtainedListener(this);
        hide();
    }

    // VIEWS
    public void setView(T v, int width, int height, int extraLayoutFlags) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | Display.DEFAULT_WINDOW_FLAGS | extraLayoutFlags,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        setView(v, layoutParams);
    }

    public void setView(T v, WindowManager.LayoutParams layoutParams) {
        if(mView != null && mVisible) {
            try {
                mWindowManager.removeView(mView);
            } catch (IllegalArgumentException e) {
                TrackerManager.getInstance(mContext).log(e.getMessage(), e);
            }
        }

        mView = v;
        mViewLayoutParams = layoutParams;
        mViewOriginalLayoutParams = new WindowManager.LayoutParams();
        mViewOriginalLayoutParams.copyFrom(mViewLayoutParams);

        ResourceUtils.disableChangeAnimations(mContext, mViewLayoutParams);
        ResourceUtils.disableChangeAnimations(mContext, mViewOriginalLayoutParams);

        if (mVisible) {
            mWindowManager.addView(mView, mViewLayoutParams);
        }
    }

    public void onDisplaySizeObtained(int prevDisplayWidth, int prevDisplayHeight, int displayWidth, int displayHeight) {
        if(mView == null) {
            return;
        }

        // adjust proportionally
        final int viewWidth = mView.getMeasuredWidth();
        final int viewHeight = mView.getMeasuredHeight();

        if(prevDisplayHeight > 0 && prevDisplayWidth > 0 && viewWidth > 0 && viewHeight > 0) {
            final float refPosX = mSpringX == null || mSpringX.isAtRest() ? getViewPositionX() : (float) mSpringX.getEndValue();
            final float refPosY = mSpringY == null || mSpringY.isAtRest() ? getViewPositionY() : (float) mSpringY.getEndValue();

            final float prevMaxX = prevDisplayWidth - viewWidth;
            final float prevMaxY = prevDisplayHeight - viewHeight;

            final float newMaxX = displayWidth - viewWidth;
            final float newMaxY = displayHeight - viewHeight;

            final float newPosX = prevMaxX > 0 ? refPosX / prevMaxX * newMaxX : 0;
            final float newPosY = prevMaxY > 0 ? refPosY / prevMaxY * newMaxY : 0;

            if (mSpringX == null || mSpringX.isAtRest()) {
                mViewLayoutParams.x = mViewOriginalLayoutParams.x = (int) newPosX;
            } else {
                mSpringX.setEndValue(newPosX);
            }

            if (mSpringY == null || mSpringY.isAtRest()) {
                mViewLayoutParams.y = mViewOriginalLayoutParams.y = (int) newPosY;
            } else {
                mSpringY.setEndValue(newPosY);
            }
        }

        setViewPosition(mViewLayoutParams.x, mViewLayoutParams.y, true);
    }

    public T getView() {
        return mView;
    }

    public void invalidate() {
        if(mView != null) {
            mView.invalidate();
        }
    }

    public boolean show() {
        if (mVisible) {
            return false;
        }

        if (mView.getWindowToken() != null) {
            mView.invalidate();
        } else {
            mWindowManager.addView(mView, mViewLayoutParams);
        }

        mVisible = true;

        return true;
    }

    public boolean hide() {
        if (!mVisible) {
            return false;
        }

        try {
            mWindowManager.removeView(mView);
        } catch (IllegalArgumentException e) {
            TrackerManager.getInstance(mContext).log(e.getMessage());
        }

        mVisible = false;
        return true;
    }

    public int getViewPositionX() {
        return mViewLayoutParams.x;
    }

    public int getViewPositionY() {
        return mViewLayoutParams.y;
    }

    public void setViewPositionX(int x) {
        setViewPosition(x, mViewLayoutParams.y);
    }

    public void setViewPositionY(int y) {
        setViewPosition(mViewLayoutParams.x, y);
    }

    protected boolean setViewPositionNoUpdateUI(int x, int y, boolean isRebounding) {
        boolean needUpdateUI = true;

        if(!isRebounding) {
            if(mSpringSystem != null) {
                if(!mSpringX.isAtRest() || !mSpringY.isAtRest()) {
                    mSpringX.setCurrentValue(x, true);
                    mSpringY.setCurrentValue(y, true);
                    needUpdateUI = false;
                }
            }
        }

        mViewLayoutParams.x = mViewOriginalLayoutParams.x = x;
        mViewLayoutParams.y = mViewOriginalLayoutParams.y = y;

        return needUpdateUI;
    }

    public void setViewPosition(int x, int y) {
        setViewPosition(x, y, false);
    }

    protected void setViewPosition(int x, int y, boolean isRebounding) {
        if(setViewPositionNoUpdateUI(x, y, isRebounding)) {
            if (mVisible && mView.getWindowToken() != null) {
                mWindowManager.updateViewLayout(mView, mViewLayoutParams);
            }
        }
    }

    public void doRebound(int x, int y, float velX, float velY, boolean startAtRest) {
        if(mSpringSystem == null) {
            throw new IllegalStateException("The SpringSystem is null!");
        }
        mSpringX.setVelocity(velX);
        mSpringY.setVelocity(velY);

        doRebound(x, y, startAtRest);
    }

    public void doRebound(int x, int y, boolean startAtRest) {
        if(mSpringSystem == null) {
            throw new IllegalStateException("The SpringSystem is null!");
        }

        if(startAtRest || (mSpringX.isAtRest() && mSpringY.isAtRest())) {
            mSpringX.setCurrentValue(getViewPositionX(), true);
            mSpringY.setCurrentValue(getViewPositionY(), true);
        }

        final int maxX = mDisplay.getDisplayWidth() - mView.getMeasuredWidth();
        final int maxY = mDisplay.getDisplayHeight() - mView.getMeasuredHeight();

        mSpringX.setEndValue(x > maxX ? maxX : x);
        mSpringY.setEndValue(y > maxY ? maxY : y);
    }

    public void setViewScale(float w, float h) {
        mViewLayoutParams.width = (int) (mViewOriginalLayoutParams.width * w);
        mViewLayoutParams.height = (int) (mViewOriginalLayoutParams.height * h);

        final int unitsToLeft = (mViewLayoutParams.width - mViewOriginalLayoutParams.width) / 2;
        mViewLayoutParams.x = mViewOriginalLayoutParams.x - unitsToLeft;

        final int unitsToTop = (mViewLayoutParams.height - mViewOriginalLayoutParams.height) / 2;
        mViewLayoutParams.y = mViewOriginalLayoutParams.y - unitsToTop;

        if (mVisible && mView.getWindowToken() != null) {
            mWindowManager.updateViewLayout(mView, mViewLayoutParams);
        }
    }

    public void setViewAlpha(float alpha) {
        mViewLayoutParams.alpha = alpha;

        if (mVisible && mView.getWindowToken() != null) {
            mWindowManager.updateViewLayout(mView, mViewLayoutParams);
        }
    }

    public boolean removeOnInteractionListener(OnInteractionListener<T> onInteractionListener) {
        return mOnInteractionListeners.remove(onInteractionListener);
    }

    public void addOnInteractionListener(OnInteractionListener<T> onInteractionListener) {
        this.mOnInteractionListeners.add(0, onInteractionListener);
    }

    public boolean isTouchable() {
        return mTouchable;
    }

    public void setTouchable(boolean mTouchable) {
        this.mTouchable = mTouchable;
        mView.setOnTouchListener(mTouchable ? mCustomGestureDetector : null);
    }

    public boolean isVisible() {
        return mVisible;
    }

    public Context getContext() {
        return mContext;
    }

    public WindowManager.LayoutParams getViewLayoutParams() {
        return mViewLayoutParams;
    }

    public WindowManager.LayoutParams getViewOriginalLayoutParams() {
        return mViewOriginalLayoutParams;
    }

    public Display getDisplay() {
        return mDisplay;
    }

    public boolean isReboundAtRest() {
        return mSpringX.isAtRest() && mSpringY.isAtRest();
    }
}
