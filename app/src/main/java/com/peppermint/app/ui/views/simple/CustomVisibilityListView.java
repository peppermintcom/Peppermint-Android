package com.peppermint.app.ui.views.simple;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

/**
 * Created by Nuno Luz on 05-01-2016.
 *
 * ListView that allows a visibility listener.
 */
public class CustomVisibilityListView extends ListView implements View.OnLayoutChangeListener {

    private OnVisibilityChangedListener mVisibilityListener;
    private CanScrollListener mCanScrollListener;

    public CustomVisibilityListView(Context context) {
        super(context);
        addOnLayoutChangeListener(this);
    }

    public CustomVisibilityListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        addOnLayoutChangeListener(this);
    }

    public CustomVisibilityListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addOnLayoutChangeListener(this);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomVisibilityListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        addOnLayoutChangeListener(this);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if(mCanScrollListener != null) {
            mCanScrollListener.canScrollChanged(canScroll(), getVisibility());
        }
    }

    public interface CanScrollListener {
        void canScrollChanged(boolean canScroll, int visibility);
    }

    public interface OnVisibilityChangedListener {
        // Avoid "onVisibilityChanged" name because it's a View method
        void visibilityChanged(int visibility);
    }

    public void setVisibilityListener(OnVisibilityChangedListener listener) {
        this.mVisibilityListener = listener;
    }

    public OnVisibilityChangedListener getVisibilityListener() {
        return this.mVisibilityListener;
    }

    public CanScrollListener getCanScrollListener() {
        return mCanScrollListener;
    }

    public void setCanScrollListener(CanScrollListener mCanScrollListener) {
        this.mCanScrollListener = mCanScrollListener;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if(mVisibilityListener != null) {
            mVisibilityListener.visibilityChanged(visibility);
        }
        if(mCanScrollListener != null) {
            mCanScrollListener.canScrollChanged(canScroll(), visibility);
        }
    }

    public boolean canScroll() {
        int childHeight = 0;
        final int childCount = getChildCount();

        for(int i=0; i<childCount; i++) {
            childHeight += getChildAt(i).getHeight();
        }

        /*Log.d("TAG", "$$$ H=" + (childHeight + getPaddingTop() + getPaddingBottom()) + " / " + getHeight());*/
        return getHeight() < (childHeight + getPaddingTop() + getPaddingBottom());
    }
}
