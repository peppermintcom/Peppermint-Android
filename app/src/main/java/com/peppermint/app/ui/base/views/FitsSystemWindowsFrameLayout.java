package com.peppermint.app.ui.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

/**
 *
 * {@link FrameLayout} that is able to fit to system windows.
 *
 * As seen at https://gist.github.com/cbeyls/ab6903e103475bd4d51b
 */
public class FitsSystemWindowsFrameLayout extends FrameLayout {

    private Rect windowInsets = new Rect();
    private Rect tempInsets = new Rect();

    private WindowInsets newWindowInsets;

    public FitsSystemWindowsFrameLayout(Context context) {
        super(context);
    }

    public FitsSystemWindowsFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FitsSystemWindowsFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            newWindowInsets = insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        } else {
            newWindowInsets = new WindowInsets(insets);
        }

        return super.dispatchApplyWindowInsets(newWindowInsets);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            insets.set(insets.left, 0, insets.right, insets.bottom);
        }

        windowInsets.set(insets);

        super.fitSystemWindows(insets);
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && newWindowInsets != null) {
            super.dispatchApplyWindowInsets(new WindowInsets(newWindowInsets));
        }

        tempInsets.set(windowInsets);
        super.fitSystemWindows(tempInsets);
    }
}
