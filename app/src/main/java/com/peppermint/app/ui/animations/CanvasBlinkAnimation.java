package com.peppermint.app.ui.animations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * Given the specified FPS value, draws a sequence of bitmaps on a Canvas.
 */
public class CanvasBlinkAnimation extends BaseCanvasAnimation {

    private Context mContext;
    private float mBlinkFrames;

    public CanvasBlinkAnimation(Context context, int framesPerSecond, int blinkFrames) {
        super(context);
        this.mContext = context;
        this.mBlinkFrames = blinkFrames;
        setFramesPerSecond(framesPerSecond);
        setDuration(getFrameInterval() * blinkFrames);
    }

    @Override
    public void apply(Canvas canvas, double interpolatedElapsedTime) {
        double currentFrame = interpolatedElapsedTime / getFrameInterval();
        Rect bounds = getBounds();

        float xx = bounds.left + getHalfBoundsWidth();
        float yy = bounds.top + getHalfBoundsHeight();
        float halfFrames = mBlinkFrames / 2f;

        if(currentFrame < halfFrames) {
            // scale up Y
            canvas.scale(1.0f, 1.0f-(mBlinkFrames/halfFrames), xx, yy);
        } else {
            // scale down Y
            canvas.scale(1.0f, 1.0f-((float)(mBlinkFrames-currentFrame)/halfFrames), xx, yy);
        }
    }
}
