package com.peppermint.app.ui.animations;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.animation.Interpolator;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * An animation that can be applied to a Canvas.
 */
public interface CanvasAnimation {
    boolean isLooping();
    void setLooping(boolean looping);

    float getFramesPerSecond();
    void setFramesPerSecond(float fps);

    Interpolator getInterpolator();
    void setInterpolator(Interpolator interpolator);

    double getElapsedTime();
    boolean setElapsedTime(double elapsedTime);

    double getDuration();
    void setDuration(double duration);

    boolean isReversed();
    void setReversed(boolean reversed);

    Rect getBounds();
    void setBounds(Rect bounds);

    void start();
    void stop();
    void reset();

    void apply(Canvas canvas);
}
