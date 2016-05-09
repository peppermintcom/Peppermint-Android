package com.peppermint.app.ui.chat.head;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.peppermint.app.tracking.TrackerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 07-05-2016.
 *
 * Represents a display/drawing area on screen according to the {@link WindowManager}.
 */
public class Display {

    private static final String TAG = Display.class.getSimpleName();

    // default flags that define the display/drawable area
    public static final int DEFAULT_WINDOW_FLAGS = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
            WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR;

    // extra flags for the dummy view only
    private static final int DUMMY_WINDOW_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

    public interface OnDisplaySizeObtainedListener {
        void onDisplaySizeObtained(int prevDisplayWidth, int prevDisplayHeight, int displayWidth, int displayHeight);
    }

    protected final Context mContext;
    protected final WindowManager mWindowManager;
    protected final List<OnDisplaySizeObtainedListener> mOnDisplaySizeObtainedListeners = new ArrayList<>();

    private final View mDummyView;
    private final WindowManager.LayoutParams mDummyViewLayoutParams;

    private boolean mInitDone = false;
    private boolean mGettingSize = false;
    protected boolean mGotSize = false;
    protected int mDisplayWidth = 0, mDisplayHeight = 0;

    // broadcast to receive screen rotation events (to re-calculate width and height)
    private final BroadcastReceiver mRotationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getDisplaySizeAsync();
        }
    };
    private final IntentFilter mRotationIntentFilter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);

    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if(!mGettingSize) {
                return;
            }

            final int displayWidth = mDummyView.getMeasuredWidth();
            final int displayHeight = mDummyView.getMeasuredHeight();

            if(displayWidth > 0 && displayHeight > 0) {
                final int prevDisplayWidth = mDisplayWidth;
                final int prevDisplayHeight = mDisplayHeight;

                mDisplayWidth = displayWidth;
                mDisplayHeight = displayHeight;

                mGettingSize = false;
                mGotSize = true;

                Log.d(TAG, "Display Size Update: " + mDisplayWidth + "x" + mDisplayHeight);

                for(OnDisplaySizeObtainedListener onDisplaySizeObtainedListener : mOnDisplaySizeObtainedListeners) {
                    onDisplaySizeObtainedListener.onDisplaySizeObtained(prevDisplayWidth, prevDisplayHeight, displayWidth, displayHeight);
                }
            }
        }
    };

    public Display(final Context mContext) {
        this.mContext = mContext;
        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        // dummy view that allows us to calculate the effective drawable area according to the
        // supplied flags
        this.mDummyView = new FrameLayout(mContext);
        this.mDummyView.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
        this.mDummyViewLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                DUMMY_WINDOW_FLAGS | DEFAULT_WINDOW_FLAGS,
                PixelFormat.TRANSPARENT);
    }

    public void init() {
        if(mInitDone) {
            return;
        }

        /*// get full screen size as initial values
        final Point point = Utils.getScreenSize(mContext);
        mDisplayWidth = point.x;
        mDisplayHeight = point.y;*/

        mGettingSize = true;
        mWindowManager.addView(mDummyView, mDummyViewLayoutParams);

        mContext.registerReceiver(mRotationBroadcastReceiver, mRotationIntentFilter);

        mInitDone = true;
    }

    public void deinit() {
        if(!mInitDone) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mDummyView.getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
        }
        mContext.unregisterReceiver(mRotationBroadcastReceiver);

        try {
            mWindowManager.removeView(mDummyView);
        } catch (IllegalArgumentException e) {
            TrackerManager.getInstance(mContext).log(e.getMessage(), e);
        }

        mGettingSize = false;
        mGotSize = false;
        mInitDone = false;
    }

    public void getDisplaySizeAsync() {
        if(mGettingSize) {
            return;
        }
        mGettingSize = true;
        mDummyView.requestLayout();
    }

    public int getDisplayHeight() {
        return mDisplayHeight;
    }

    public int getDisplayWidth() {
        return mDisplayWidth;
    }

    public boolean isGotSize() {
        return mGotSize;
    }

    public void addOnDisplaySizeObtainedListener(OnDisplaySizeObtainedListener onDisplaySizeObtainedListener) {
        mOnDisplaySizeObtainedListeners.add(onDisplaySizeObtainedListener);
    }

    public boolean removeOnDisplaySizeObtainedListener(OnDisplaySizeObtainedListener onDisplaySizeObtainedListener) {
        return mOnDisplaySizeObtainedListeners.remove(onDisplaySizeObtainedListener);
    }

    /**
     * Dim the screen.
     * @param dimAmount amount from 0.0 (no dim) to 1.0 (dim black)
     */
    public void dim(float dimAmount) {
        if(mDummyViewLayoutParams == null) {
            return;
        }

        mDummyViewLayoutParams.dimAmount = dimAmount;
        if(dimAmount > 0) {
            mDummyViewLayoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        } else {
            mDummyViewLayoutParams.flags = DUMMY_WINDOW_FLAGS | DEFAULT_WINDOW_FLAGS;
        }

        if (mDummyView.getWindowToken() != null) {
            mWindowManager.updateViewLayout(mDummyView, mDummyViewLayoutParams);
        }
    }

    public float getDim() {
        if(mDummyViewLayoutParams == null) {
            return 0;
        }

        return mDummyViewLayoutParams.dimAmount;
    }
}
