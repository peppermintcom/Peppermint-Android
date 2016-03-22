package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.peppermint.app.R;
import com.peppermint.app.ui.KeyInterceptable;
import com.peppermint.app.ui.views.simple.TouchInterceptorView;

/**
 * Created by Nuno Luz on 19-03-2016.
 */
public class BackgroundView extends WindowManagerViewGroup implements KeyInterceptable {

    private TouchInterceptorView mView;

    public BackgroundView(Context mContext) {
        super(mContext);

        mView = new TouchInterceptorView(mContext);
        mView.setBackgroundResource(R.color.black_50percent);
        mView.setVisibility(View.GONE);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        super.show();
        addView(mView, layoutParams);
    }

    public void requestLayout() {
        setViewPosition(0, 0, 0);
    }

    @Override
    public boolean show() {
        if(mViews.get(0).getVisibility() == View.GONE) {
            mViews.get(0).setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }

    @Override
    public boolean hide() {
        if(mViews.get(0).getVisibility() == View.VISIBLE) {
            mViews.get(0).setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    public void destroy() {
        mView.removeAllKeyEventInterceptors();
        super.hide();
    }

    @Override
    public void addKeyEventInterceptor(View.OnKeyListener listener) {
        mView.addKeyEventInterceptor(listener);
    }

    @Override
    public boolean removeKeyEventInterceptor(View.OnKeyListener listener) {
        return mView.removeKeyEventInterceptor(listener);
    }

    @Override
    public void removeAllKeyEventInterceptors() {
        mView.removeAllKeyEventInterceptors();
    }
}
