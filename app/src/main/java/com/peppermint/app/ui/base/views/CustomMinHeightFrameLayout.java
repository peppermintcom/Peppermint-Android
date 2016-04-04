package com.peppermint.app.ui.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.peppermint.app.R;

/**
 * Created by Nuno Luz on 03-12-2015.
 *
 * FrameLayout that is hidden if the minHeight value is not satisfied.
 */
public class CustomMinHeightFrameLayout extends FrameLayout {

    private int mMinVisibilityHeight = 0;

    public CustomMinHeightFrameLayout(Context context) {
        super(context);
        init(context, null);
    }

    public CustomMinHeightFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomMinHeightFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomMinHeightFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                mMinVisibilityHeight = a.getDimensionPixelSize(R.styleable.PeppermintView_minVisibilityHeight, 0);
            } finally {
                a.recycle();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = MeasureSpec.getSize(heightMeasureSpec);

        if(height < mMinVisibilityHeight && getVisibility() == VISIBLE) {
            setVisibility(INVISIBLE);
        } else if(height >= mMinVisibilityHeight && getVisibility() == INVISIBLE) {
            setVisibility(VISIBLE);
        }
    }
}
