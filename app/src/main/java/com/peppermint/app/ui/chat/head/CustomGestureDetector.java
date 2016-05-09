package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 05-05-2016.
 *
 * Enhanced detector for complex gestures.
 */
public class CustomGestureDetector implements View.OnTouchListener {

    /**
     * Listener of user-view interaction.
     */
    public interface OnGestureListener {
        boolean onClick(View view);
        boolean onDragStarted(View view, float offsetX, float offsetY, MotionEvent event);
        boolean onDrag(View view, float offsetX, float offsetY, MotionEvent event);
        boolean onDragFinished(View view, float[] velocity, MotionEvent event);
    }

    private final Context mContext;

    private float mLastX, mLastY;
    private boolean mDragging = false;

    // custom velocity tracking code
    // Android's VelocityTracker implementation has severe issues,
    // returning inverse velocity vectors in some cases
    private List<Long> lastTimes = new ArrayList<>();
    private List<float[]> lastEvents = new ArrayList<>();

    private List<OnGestureListener> mOnGestureListeners = new ArrayList<>();
    private GestureDetector mClickDetector, mDragDetector;

    public CustomGestureDetector(Context context) {
        this.mContext = context;
        this.mClickDetector = new GestureDetector(mContext, mOnTapGestureListener);
        this.mDragDetector = new GestureDetector(mContext, mOnScrollGestureListener);
    }

    private void pushMotionEvent(MotionEvent event) {
        // clear if direction changes
        int size = lastEvents.size();
        if(size > 1) {
            final float lastX = lastEvents.get(size - 1)[0];
            final float lastY = lastEvents.get(size - 1)[1];
            final float lastM1X = lastEvents.get(size - 2)[0];
            final float lastM1Y = lastEvents.get(size - 2)[1];

            if((lastX - lastM1X) * (event.getRawX() - lastX) < 0 || (lastY - lastM1Y) * (event.getRawY() - lastY) < 0) {
                while(lastEvents.size() > 1) {
                    lastEvents.remove(0);
                    lastTimes.remove(0);
                }
                size = 1;
            }
        }

        // keep track of the 5 last motion events
        if (size >= 5) {
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
        if (mOnGestureListeners.size() == 0) {
            return false;
        }

        boolean localClick = mClickDetector.onTouchEvent(event);
        boolean localDrag = mDragDetector.onTouchEvent(event);

        float touchX = event.getRawX();
        float touchY = event.getRawY();
        boolean ret = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pushMotionEvent(event);

                mLastX = touchX;
                mLastY = touchY;

                ret = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDragging && !localClick) {
                    int listenerSize = mOnGestureListeners.size();
                    boolean stopIteration = false;
                    for (int i = 0; i < listenerSize && !stopIteration; i++) {
                        stopIteration = mOnGestureListeners.get(i).onDragFinished(v, getVelocity(), event);
                    }
                } else {
                    int listenerSize = mOnGestureListeners.size();
                    boolean stopIteration = false;
                    for (int i = 0; i < listenerSize && !stopIteration; i++) {
                        stopIteration = mOnGestureListeners.get(i).onClick(v);
                    }
                }
                // re-create the gesture detectors to avoid remnants of past events messing up detected actions
                mClickDetector = new GestureDetector(mContext, mOnTapGestureListener);
                mDragDetector = new GestureDetector(mContext, mOnScrollGestureListener);
                mDragging = false;

                ret = true;
                break;
            case MotionEvent.ACTION_MOVE:
                pushMotionEvent(event);

                float offsetX = mLastX - touchX;
                float offsetY = mLastY - touchY;

                if (localDrag && !localClick) {    // try to avoid movement on tap
                    if (!mDragging) {
                        int listenerSize = mOnGestureListeners.size();
                        boolean stopIteration = false;
                        for (int i = 0; i < listenerSize && !stopIteration; i++) {
                            stopIteration = mOnGestureListeners.get(i).onDragStarted(v, offsetX, offsetY, event);
                        }
                        mDragging = true;
                    } else {
                        int listenerSize = mOnGestureListeners.size();
                        boolean stopIteration = false;
                        for (int i = 0; i < listenerSize && !stopIteration; i++) {
                            stopIteration = mOnGestureListeners.get(i).onDrag(v, offsetX, offsetY, event);
                        }
                    }
                }

                ret = true;
                break;
        }

        mLastX = touchX;
        mLastY = touchY;

        return ret;
    }

    public void resetDragOffset() {

    }

    private GestureDetector.SimpleOnGestureListener mOnTapGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }
    };

    private GestureDetector.SimpleOnGestureListener mOnScrollGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }
    };

    public void addOnGestureListener(OnGestureListener listener) {
        mOnGestureListeners.add(listener);
    }

    public void addOnGestureListener(int index, OnGestureListener listener) {
        mOnGestureListeners.add(index, listener);
    }

    public boolean removeOnGestureListener(OnGestureListener listener) {
        return mOnGestureListeners.remove(listener);
    }

    public int getOnGestureListenerAmount() {
        return mOnGestureListeners.size();
    }
}
