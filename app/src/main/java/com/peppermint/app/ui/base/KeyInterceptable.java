package com.peppermint.app.ui.base;

import android.view.View;

/**
 * Created by Nuno Luz on 04-03-2016.
 */
public interface KeyInterceptable {
    void addKeyEventInterceptor(View.OnKeyListener listener);
    boolean removeKeyEventInterceptor(View.OnKeyListener listener);
    void removeAllKeyEventInterceptors();
}
