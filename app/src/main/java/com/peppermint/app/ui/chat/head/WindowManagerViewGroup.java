package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 19-03-2016.
 */
public class WindowManagerViewGroup {

    public interface OnInteractionListener {
        boolean onClick(int viewIndex, View view);
        boolean onDragStarted(int viewIndex, View view, float offsetX, float offsetY, MotionEvent event);
        boolean onDrag(int viewIndex, View view, float offsetX, float offsetY, MotionEvent event);
        boolean onDragFinished(int viewIndex, View view, float[] velocity, MotionEvent event);
    }

    protected Context mContext;
    private WindowManager mWindowManager;

    // ui
    private List<OnInteractionListener> mOnInteractionListeners = new ArrayList<>();
    private GestureDetector mClickDetector;

    // views
    protected boolean mVisible = false;
    protected List<View> mViews = new ArrayList<>();
    protected List<WindowManager.LayoutParams> mLayoutParams = new ArrayList<>();
    protected List<WindowManager.LayoutParams> mOriginalLayoutParams = new ArrayList<>();

     // UI
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        private float lastX, lastY;
        private int wait = 3;

        // custom velocity tracking code
        // Android's VelocityTracker implementation has severe issues,
        // returning inverse velocity vectors in some cases
        private List<Long> lastTimes = new ArrayList<>();
        private List<float[]> lastEvents = new ArrayList<>();

        private void pushMotionEvent(MotionEvent event) {
            // keep track of the 5 last motion events
            if(lastEvents.size() >= 5) {
                lastEvents.remove(0);
                lastTimes.remove(0);
            }
            lastEvents.add(new float[]{event.getRawX(), event.getRawY()});
            lastTimes.add(event.getEventTime());
        }

        private float[] getVelocity() {
            // use the first and last of the stored motion events to calculate the vel. vector
            int lastIndex = lastEvents.size() - 1;
            float durationSec = ((float) (lastTimes.get(lastIndex) - lastTimes.get(0)) / 1000f);
            float velX = (lastEvents.get(lastIndex)[0] - lastEvents.get(0)[0]) / durationSec;
            float velY = (lastEvents.get(lastIndex)[1] - lastEvents.get(0)[1]) / durationSec;
            return new float[]{velX, velY};
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(mOnInteractionListeners.size() == 0) {
                return false;
            }

            boolean localClick = mClickDetector.onTouchEvent(event);

            float touchX = event.getRawX();
            float touchY = event.getRawY();
            boolean ret = false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pushMotionEvent(event);

                    wait = 3;
                    lastX = touchX;
                    lastY = touchY;

                    ret = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if(!localClick) {
                        int listenerSize = mOnInteractionListeners.size();
                        boolean stopIteration = false;
                        for(int i=0; i<listenerSize && !stopIteration; i++) {
                            stopIteration = mOnInteractionListeners.get(i).onDragFinished(mViews.indexOf(v), v, getVelocity(), event);
                        }
                    } else {
                        int listenerSize = mOnInteractionListeners.size();
                        boolean stopIteration = false;
                        for(int i=0; i<listenerSize && !stopIteration; i++) {
                            stopIteration = mOnInteractionListeners.get(i).onClick(mViews.indexOf(v), v);
                        }
                    }
                    mClickDetector = new GestureDetector(mContext, mOnGestureListener);
                    ret = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    pushMotionEvent(event);

                    float offsetX = lastX - touchX;
                    float offsetY = lastY - touchY;

                    if(wait <= 0) {    // try to avoid movement on tap
                        if(wait == 0) {
                            int listenerSize = mOnInteractionListeners.size();
                            boolean stopIteration = false;
                            for(int i=0; i<listenerSize && !stopIteration; i++) {
                                stopIteration = mOnInteractionListeners.get(i).onDragStarted(mViews.indexOf(v), v, offsetX, offsetY, event);
                            }
                        } else {
                            int listenerSize = mOnInteractionListeners.size();
                            boolean stopIteration = false;
                            for(int i=0; i<listenerSize && !stopIteration; i++) {
                                stopIteration = mOnInteractionListeners.get(i).onDrag(mViews.indexOf(v), v, offsetX, offsetY, event);
                            }
                        }
                    }
                    wait--;

                    ret = true;
                    break;
            }

            lastX = touchX;
            lastY = touchY;

            return ret;
        }
    };

    private GestureDetector.SimpleOnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) { return true; }
    };

    public WindowManagerViewGroup(Context mContext) {
        this.mContext = mContext;
        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        this.mClickDetector = new GestureDetector(mContext, mOnGestureListener);
    }

    public WindowManagerViewGroup(Context mContext, OnInteractionListener mOnInteractionListener) {
        this(mContext);
        if(mOnInteractionListener != null) {
            this.mOnInteractionListeners.add(mOnInteractionListener);
        }
    }

    // VIEWS
    public void addView(View v) {
        addView(v, new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT));
    }

    public void addView(View v, WindowManager.LayoutParams layoutParams) {
        v.setOnTouchListener(mOnTouchListener);
        mViews.add(v);
        mLayoutParams.add(layoutParams);
        mOriginalLayoutParams.add(new WindowManager.LayoutParams(
                layoutParams.width, layoutParams.height,
                layoutParams.type, layoutParams.flags, layoutParams.format));

        if(mVisible) {
            mWindowManager.addView(v, layoutParams);
        }
    }

    public void removeView(View v) {
        int i = mViews.indexOf(v);
        if(i < 0) {
            return;
        }

        if(mVisible) {
            mWindowManager.removeView(v);
        }

        mViews.get(i).setOnTouchListener(null);
        mViews.remove(i);
        mLayoutParams.remove(i);
        mOriginalLayoutParams.remove(i);
    }

    public void invalidate() {
        if(!mVisible) {
            return;
        }

        for(View v : mViews) {
            v.invalidate();
        }
    }

    public boolean show() {
        if(mVisible) {
            return false;
        }

        int size = mViews.size();
        for(int i=0; i<size; i++) {
            View v = mViews.get(i);
            if (v.getWindowToken() != null) {
                v.invalidate();
            } else {
                mWindowManager.addView(v, mLayoutParams.get(i));
            }
        }

        mVisible = true;
        return true;
    }

    public boolean hide() {
        if(!mVisible) {
            return false;
        }

        for(View v : mViews) {
            if (v.getWindowToken() != null) {
                mWindowManager.removeView(v);
            }
        }

        mVisible = false;
        return true;
    }

    public int getViewPositionX(int i) {
        if(i < 0 || i >= mLayoutParams.size()) {
            return 0;
        }
        return mLayoutParams.get(i).x;
    }

    public int getViewPositionY(int i) {
        if(i < 0 || i >= mLayoutParams.size()) {
            return 0;
        }
        return mLayoutParams.get(i).y;
    }

    public void setViewPositionX(int i, int x) {
        if(i < 0 || i >= mLayoutParams.size()) {
            return;
        }

        setViewPosition(i, x, mLayoutParams.get(i).y);
    }

    public void setViewPositionY(int i, int y) {
        if(i < 0 || i >= mLayoutParams.size()) {
            return;
        }

        setViewPosition(i, mLayoutParams.get(i).x, y);
    }

    public void setViewPosition(int i, int x, int y) {
        if(i < 0 || i >= mLayoutParams.size()) {
            return;
        }

        mLayoutParams.get(i).x = mOriginalLayoutParams.get(i).x = x;
        mLayoutParams.get(i).y = mOriginalLayoutParams.get(i).y = y;
        if (mVisible && mViews.get(i).getWindowToken() != null) {
            mWindowManager.updateViewLayout(mViews.get(i), mLayoutParams.get(i));
        }
    }

    public void setViewScale(int i, float w, float h) {
        if(i < 0 || i >= mLayoutParams.size()) {
            return;
        }

        mLayoutParams.get(i).width = (int) (mOriginalLayoutParams.get(i).width * w);
        mLayoutParams.get(i).height = (int) (mOriginalLayoutParams.get(i).height * h);

        int unitsToLeft = (mLayoutParams.get(i).width - mOriginalLayoutParams.get(i).width) / 2;
        mLayoutParams.get(i).x = mOriginalLayoutParams.get(i).x - unitsToLeft;

        int unitsToTop = (mLayoutParams.get(i).height - mOriginalLayoutParams.get(i).height) / 2;
        mLayoutParams.get(i).y = mOriginalLayoutParams.get(i).y - unitsToTop;

        if (mVisible && mViews.get(i).getWindowToken() != null) {
            mWindowManager.updateViewLayout(mViews.get(i), mLayoutParams.get(i));
        }
    }

    public boolean isVisible() {
        return mVisible;
    }

    public Context getContext() {
        return mContext;
    }

    public void addOnInteractionListener(OnInteractionListener onInteractionListener) {
        if(onInteractionListener == null) {
            return;
        }
        mOnInteractionListeners.add(0, onInteractionListener);
    }

    public boolean removeOnInteractionListener(OnInteractionListener onInteractionListener) {
        return mOnInteractionListeners.remove(onInteractionListener);
    }
}
