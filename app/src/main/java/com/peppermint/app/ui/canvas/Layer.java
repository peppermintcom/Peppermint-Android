package com.peppermint.app.ui.canvas;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

/**
 * Created by Nuno Luz on 06-10-2015.
 *
 * A drawing layer on an {@link AnimatedView}.
 */
public interface Layer {
    void draw(View view, Canvas canvas);

    Rect getBounds();
    void setBounds(Rect bounds);
}
