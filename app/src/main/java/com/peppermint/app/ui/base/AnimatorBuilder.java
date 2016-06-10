package com.peppermint.app.ui.base;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 18-06-2015.
 *
 * <p>
 *     Builds pre-defined animators that can be associated with a View.
 * </p>
 */
public class AnimatorBuilder {

    public AnimatorBuilder() {
    }

    /**
     * Builds an animator that simultaneously fades in all the target views.
     * @param views the set of target views
     * @return the animator
     */
    public Animator buildFadeInAnimator(long duration, View... views) {
        AnimatorSet set = new AnimatorSet();
        List<Animator> animatorList = new ArrayList<>();

        for(View v : views) {
            ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(v, "alpha", 0, 1);
            fadeAnimator.setDuration(duration);
            fadeAnimator.setInterpolator(new LinearInterpolator());
            animatorList.add(fadeAnimator);
        }

        set.playTogether(animatorList);
        return set;
    }

    public Animator buildFadeInAnimator(View... views) {
        return buildFadeInAnimator(600, views);
    }

    /**
     * Builds an animator that simultaneously fades out all the target views.
     * @param views the set of target views
     * @return the animator
     */
    public Animator buildFadeOutAnimator(long duration, View... views) {
        AnimatorSet set = new AnimatorSet();
        List<Animator> animatorList = new ArrayList<>();

        for(View v : views) {
            ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(v, "alpha", 1, 0);
            fadeAnimator.setDuration(duration);
            fadeAnimator.setInterpolator(new LinearInterpolator());
            animatorList.add(fadeAnimator);
        }

        set.playTogether(animatorList);
        return set;
    }

    public Animator buildFadeOutAnimator(View... views) {
        return buildFadeOutAnimator(600, views);
    }

    /**
     * Builds an animator that slides in a target view from the left.
     * @param v the target view
     * @return the animator
     */
    public Animator buildSlideInLeftAnimator(View v) {
        ObjectAnimator slideAnimator = ObjectAnimator.ofFloat(v, "translationX", Utils.percentScreenWidthToPx(v.getContext(), -100), 0);
        slideAnimator.setDuration(800);
        slideAnimator.setInterpolator(new DecelerateInterpolator());
        return slideAnimator;
    }

    /**
     * Builds an animator that slides out a target view to the right.
     * @param delay the delay in ms to start sliding
     * @param v the target view
     * @param originX the original translation value (typically 0, but may be another value if the target view is already translated)
     * @return the animator
     */
    public Animator buildSlideOutRightAnimator(int delay, View v, float originX) {
        ObjectAnimator slideAnimator = ObjectAnimator.ofFloat(v, "translationX", originX, Utils.percentScreenWidthToPx(v.getContext(), 100));
        slideAnimator.setStartDelay(delay);
        slideAnimator.setDuration(800);
        slideAnimator.setInterpolator(new DecelerateInterpolator());
        return slideAnimator;
    }

}
