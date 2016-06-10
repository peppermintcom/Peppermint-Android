package com.peppermint.app.ui.base;

import android.view.View;

/**
 * Created by Nuno Luz on 04-03-2016.
 */
public interface TouchInterceptable {
    void addTouchEventInterceptor(View.OnTouchListener listener);
    boolean removeTouchEventInterceptor(View.OnTouchListener listener);
}
