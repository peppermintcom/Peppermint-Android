package com.peppermint.app.ui.views.simple;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Created by Nuno Luz on 05-01-2016.
 *
 * ListView that allows a visibility listener.
 */
public class CustomVisibilityListView extends ListView {

    private OnVisibilityChangedListener mVisibilityListener;

    public CustomVisibilityListView(Context context) {
        super(context);
    }

    public CustomVisibilityListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVisibilityListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomVisibilityListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public interface OnVisibilityChangedListener {
        // Avoid "onVisibilityChanged" name because it's a View method
        public void visibilityChanged(int visibility);
    }

    public void setVisibilityListener(OnVisibilityChangedListener listener) {
        this.mVisibilityListener = listener;
    }

    public OnVisibilityChangedListener getVisibilityListener() {
        return this.mVisibilityListener;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if(mVisibilityListener != null) {
            mVisibilityListener.visibilityChanged(visibility);
        }
    }
}
