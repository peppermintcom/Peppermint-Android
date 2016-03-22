package com.peppermint.app.ui.views.simple;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.peppermint.app.ui.KeyInterceptable;
import com.peppermint.app.ui.TouchInterceptable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 07-02-2016.
 */
public class TouchInterceptorView extends RelativeLayout implements TouchInterceptable, KeyInterceptable {

    private List<OnTouchListener> mTouchListeners = new ArrayList<>();
    private List<OnKeyListener> mKeyListeners = new ArrayList<>();

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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        for(OnTouchListener listener : mTouchListeners) {
            listener.onTouch(this, ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        for(OnKeyListener listener : mKeyListeners) {
            listener.onKey(this, event.getKeyCode(), event);
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void addTouchEventInterceptor(View.OnTouchListener listener) {
        mTouchListeners.add(listener);
    }

    @Override
    public boolean removeTouchEventInterceptor(View.OnTouchListener listener) {
        return mTouchListeners.remove(listener);
    }

    @Override
    public void addKeyEventInterceptor(OnKeyListener listener) {
        mKeyListeners.add(listener);
    }

    @Override
    public boolean removeKeyEventInterceptor(OnKeyListener listener) {
        return mKeyListeners.remove(listener);
    }

    @Override
    public void removeAllKeyEventInterceptors() {
        mKeyListeners.clear();
    }
}
