package com.peppermint.app.ui.views.simple;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.EditText;

import com.peppermint.app.R;

import java.util.WeakHashMap;

/**
 * Created by Nuno Luz on 10-11-2015.
 * {@link EditText} with the custom textFont attribute.
 */
public class CustomFontEditText extends EditText {

    private static WeakHashMap<String, Typeface> mTypefaceCache = new WeakHashMap<>();

    private static Typeface getTypeface(Context context, String fontPath) {
        if(mTypefaceCache.containsKey(fontPath)) {
            return mTypefaceCache.get(fontPath);
        }

        Typeface tf = Typeface.createFromAsset(context.getAssets(), fontPath);
        mTypefaceCache.put(fontPath, tf);

        return tf;
    }

    public CustomFontEditText(Context context) {
        super(context);
        init(null);
    }

    public CustomFontEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CustomFontEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomFontEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                String tfPath = a.getString(R.styleable.PeppermintView_textFont);
                setTypeface(tfPath);
            } finally {
                a.recycle();
            }
        }
    }

    public void setTypeface(String tfPath) {
        if(tfPath != null) {
            setTypeface(getTypeface(getContext(), tfPath));
        }
    }
}
