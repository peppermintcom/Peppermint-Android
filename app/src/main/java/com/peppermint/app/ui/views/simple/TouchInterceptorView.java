package com.peppermint.app.ui.views.simple;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by Nuno Luz on 07-02-2016.
 */
public class TouchInterceptorView extends FrameLayout {

    private View mRelayView;

    public TouchInterceptorView(Context context) {
        super(context);
        init();
    }

    public TouchInterceptorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TouchInterceptorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TouchInterceptorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
    }
/*
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(mRelayView == null) {
            return false;
        }
        Log.d("Interceptor", "" + ev);
        mRelayView.dispatchTouchEvent(MotionEvent.obtain(ev.getDownTime(), ev.getEventTime(), ev.getAction(), ev.getX(), ev.getY(), ev.getPressure(), ev.getSize(), ev.getMetaState(), ev.getXPrecision(), ev.getYPrecision(), ev.getDeviceId(), ev.getEdgeFlags()));
        return true;
    }*/

    public View getRelayView() {
        return mRelayView;
    }

    public void setRelayView(View view) {
        this.mRelayView = view;
    }
}
