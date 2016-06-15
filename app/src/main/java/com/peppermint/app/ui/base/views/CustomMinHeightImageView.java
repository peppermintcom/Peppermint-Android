package com.peppermint.app.ui.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.peppermint.app.R;

/**
 * Created by Nuno Luz on 03-12-2015.
 *
 * ImageView that is hidden if the minHeight value is not satisfied.
 */
public class CustomMinHeightImageView extends ImageView {

    private int mMinVisibilityHeight = 0;

    public CustomMinHeightImageView(Context context) {
        super(context);
        init(context, null);
    }

    public CustomMinHeightImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomMinHeightImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomMinHeightImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if(attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
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
        int height = MeasureSpec.getSize(heightMeasureSpec);
        final int mode = MeasureSpec.getMode(heightMeasureSpec);

        final int drawableHeight = getDrawable() != null ? getDrawable().getIntrinsicHeight() : 0;

        int maxHeight = Integer.MAX_VALUE;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            maxHeight = getMaxHeight();
        }

        if(mode == MeasureSpec.AT_MOST) {
            height = Math.min(Math.min(drawableHeight, height), maxHeight);
        }

        if((mode == MeasureSpec.EXACTLY || mode == MeasureSpec.AT_MOST) && height < mMinVisibilityHeight) {
            if(getVisibility() == VISIBLE) {
                setVisibility(INVISIBLE);
            }
            setMeasuredDimension(0, 0);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (getVisibility() == INVISIBLE) {
                setVisibility(VISIBLE);
            }
        }
    }
}
