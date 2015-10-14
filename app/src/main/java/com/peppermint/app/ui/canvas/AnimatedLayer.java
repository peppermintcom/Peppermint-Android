package com.peppermint.app.ui.canvas;

import android.view.animation.Interpolator;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * An animated {@link Layer}.
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

    /**
     * Starts the animation (starts elapsing time).
     */
    void start();

    /**
     * Stops the animation (stops elapsing time).
     */
    void stop();

    /**
     * Resets the animation (resets elapsed time to its original value).
     */
    void reset();

    boolean isRunning();
}
