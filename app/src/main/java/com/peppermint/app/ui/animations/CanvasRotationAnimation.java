package com.peppermint.app.ui.animations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * Given the specified FPS value, draws a sequence of bitmaps on a Canvas.
 */
public class CanvasRotationAnimation extends BaseCanvasAnimation {

    private Context mContext;

    public CanvasRotationAnimation(Context context, int framesPerSecond) {
        super(context);
        this.mContext = context;
        setFramesPerSecond(framesPerSecond);
        setDuration(getFrameInterval() * 360);
    }

    @Override
    public void apply(Canvas canvas, double interpolatedElapsedTime) {
        double angle = interpolatedElapsedTime / getFrameInterval();
        Rect bounds = getBounds();
        canvas.rotate((float) angle, bounds.left + getHalfBoundsWidth(), bounds.top + getHalfBoundsHeight());
    }
}
