package com.peppermint.app.ui.canvas;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * Listener of animation events triggered by a AnimatedLayer.
 */
public interface AnimatedLayerListener {
    void onAnimationStarted(AnimatedLayer animatedLayer);
    void onAnimationEnded(AnimatedLayer animatedLayer);
}
