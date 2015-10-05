package com.peppermint.app.ui.animations.old;

import android.graphics.Canvas;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * An animation that can be applied to a canvas.
 *
 */
public interface CanvasAnimation {
    void step(Canvas canvas);
}
