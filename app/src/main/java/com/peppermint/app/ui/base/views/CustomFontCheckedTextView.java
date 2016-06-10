package com.peppermint.app.ui.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.CheckedTextView;

import com.peppermint.app.R;

import java.util.WeakHashMap;

/**
 * Created by Nuno Luz on 25-05-2016.
 *
 * {@link CheckedTextView} with the custom textFont attribute.
 */
public class CustomFontCheckedTextView extends CheckedTextView {

    private static WeakHashMap<String, Typeface> mTypefaceCache = new WeakHashMap<>();

    private static Typeface getTypeface(Context context, String fontPath) {
        if(mTypefaceCache.containsKey(fontPath)) {
            return mTypefaceCache.get(fontPath);
        }

        Typeface tf = Typeface.createFromAsset(context.getAssets(), fontPath);
        mTypefaceCache.put(fontPath, tf);

        return tf;
    }

    public CustomFontCheckedTextView(Context context) {
        super(context);
        init(null);
    }

    public CustomFontCheckedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CustomFontCheckedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomFontCheckedTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public void setTypeface(int typefaceStringResId) {
        String tfPath = getContext().getString(typefaceStringResId);
        if(tfPath != null) {
            setTypeface(getTypeface(getContext(), tfPath));
        }
    }

    public void init(AttributeSet attrs) {
        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                String tfPath = a.getString(R.styleable.PeppermintView_textFont);
                if(tfPath != null) {
                    setTypeface(getTypeface(getContext(), tfPath));
                }
            } finally {
                a.recycle();
            }
        }
    }
}
