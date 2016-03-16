package com.peppermint.app.ui.canvas;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * RecordServiceListener of animation events triggered by a {@link AnimatedLayer}.
 */
public interface AnimatedLayerListener {
    void onAnimationStarted(AnimatedLayer animatedLayer);
    void onAnimationEnded(AnimatedLayer animatedLayer);
}
