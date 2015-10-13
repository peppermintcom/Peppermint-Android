package com.peppermint.app.ui.canvas;

import android.graphics.Canvas;
import android.view.View;
import android.view.animation.Interpolator;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * An animation that can be applied to a Canvas.
 */
public interface AnimatedLayer extends Layer {

    void addAnimationListener(AnimatedLayerListener mListener);
    boolean removeAnimationListener(AnimatedLayerListener mListener);

    boolean isLooping();
    void setLooping(boolean looping);

    Interpolator getInterpolator();
    void setInterpolator(Interpolator interpolator);

    boolean addElapsedTime(double value);
    double getElapsedTime();
    boolean setElapsedTime(double elapsedTime);

    double getDuration();
    void setDuration(double duration);

    boolean isReversed();
    void setReversed(boolean reversed);

    void start();
    void stop();
    void reset();

    boolean isRunning();
}
