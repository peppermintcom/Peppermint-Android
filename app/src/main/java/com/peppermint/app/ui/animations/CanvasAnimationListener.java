package com.peppermint.app.ui.animations;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * Listener of animation events triggered by a CanvasAnimation.
 */
public interface CanvasAnimationListener {
    void onAnimationStarted(CanvasAnimation animation);
    void onAnimationApplied(CanvasAnimation animation);
    void onAnimationEnded(CanvasAnimation animation);
}
