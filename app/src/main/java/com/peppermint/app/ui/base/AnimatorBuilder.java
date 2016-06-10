package com.peppermint.app.ui.base;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
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
     * Builds an animator that shortens the size of the target view to 50% and
     * enforces a displacement of 10dps to the right and bottom.
     *
     * @param v the target view
     * @return the animator
     */
    public Animator buildShortenTowardsBottomRightAnimator(View v) {
        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(new LinearInterpolator());

        ObjectAnimator scaleAnimator = new ObjectAnimator();
        scaleAnimator.setTarget(v);
        scaleAnimator.setDuration(1000);
        scaleAnimator.setValues(PropertyValuesHolder.ofFloat("scaleX", 1f, 0.5f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.5f));
        set.playTogether(
                scaleAnimator,
                ObjectAnimator.ofFloat(v, "translationX", 0, Utils.dpToPx(v.getContext(), 10)).setDuration(1000),
                ObjectAnimator.ofFloat(v, "translationY", 0, Utils.dpToPx(v.getContext(), 10)).setDuration(1000)
        );

        return set;
    }

    /**
     * Builds an animator that applies a photo effect to the target view:
     * <ol>
     *     <li>Slides in from the right and stops and the original position;</li>
     *     <li>Fades out and in (a white background_gradient will give a flash effect);</li>
     *     <li>Slides out from the original position to the left (only if slideOut is true).</li>
     * </ol>
     * @param v the target view
     * @param slideOut if the view slides out at the end
     * @return the animator
     */
    public Animator buildBillAnimator(View v, boolean slideOut) {
        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(new LinearInterpolator());

        List<Animator> animatorList = new ArrayList<>();
        ObjectAnimator a1 = ObjectAnimator.ofFloat(v.getParent(), "translationX", Utils.percentScreenWidthToPx(v.getContext(), 100), 0).setDuration(1000);
        ObjectAnimator a2 = ObjectAnimator.ofFloat(v, "alpha", 1, 0).setDuration(400);
        ObjectAnimator a3 = ObjectAnimator.ofFloat(v, "alpha", 0, 1).setDuration(100);
        animatorList.add(a1);
        animatorList.add(a2);
        animatorList.add(a3);

        if(slideOut) {
            ObjectAnimator a4 = ObjectAnimator.ofFloat(v.getParent(), "translationX", 0, Utils.percentScreenWidthToPx(v.getContext(), -100)).setDuration(1000);
            animatorList.add(a4);
        }

        set.playSequentially(animatorList);

        return set;
    }

    /**
     * Builds an animator that applies a wobble effect onto the target view using an AccelerateDecelerateInterpolator.
     *
     * @param v the target view
     * @return the animator
     */
    public Animator buildWobbleAnimator(View v) {
        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(new AccelerateDecelerateInterpolator());

        set.playSequentially(
            ObjectAnimator.ofFloat(v, "rotation", 0, 25).setDuration(100),
            ObjectAnimator.ofFloat(v, "rotation", 25, -15).setDuration(90),
            ObjectAnimator.ofFloat(v, "rotation", -15, 8).setDuration(80),
            ObjectAnimator.ofFloat(v, "rotation", 8, 0).setDuration(70)
        );

        return set;
    }

    /**
     * Builds an animator that applies a floating effect onto the target view. The animation is performed repeatedly until explicitly stopped.
     * @param v the target view
     * @return the animator
     */
    public Animator buildFloatAnimator(View v) {
        final AnimatorSet loopSet = new AnimatorSet();
        ObjectAnimator a2 = ObjectAnimator.ofFloat(v, "translationY", 3, Utils.dpToPx(v.getContext(), -3)).setDuration(700);
        ObjectAnimator a3 = ObjectAnimator.ofFloat(v, "translationY", -3, Utils.dpToPx(v.getContext(), 3)).setDuration(700);
        a2.setInterpolator(new LinearInterpolator());
        a3.setInterpolator(new LinearInterpolator());
        a2.setStartDelay(100);
        a3.setStartDelay(100);
        loopSet.playSequentially(a2, a3);

        AnimatorSet set = new AnimatorSet();
        ObjectAnimator a1 = ObjectAnimator.ofFloat(v, "translationY", 0, Utils.dpToPx(v.getContext(), 3)).setDuration(350);
        a1.setInterpolator(new DecelerateInterpolator());
        set.playSequentially(a1, loopSet);

        loopSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                loopSet.start();
            }
        });

        return set;
    }

    /**
     * Builds an animator that fades and scales in the target view simultaneously.
     * See {@link #buildFadeInAnimator(View...)} and {@link #buildScaleInAnimator(View)} for more information.
     * @param v the target view
     * @return the animator
     */
    public Animator buildFadeScaleInAnimator(View v) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(buildFadeInAnimator(v), buildScaleInAnimator(v));
        return set;
    }

    /**
     * Builds an animator that fades in and slides in the target view from the right (performed simultanously).
     * See {@link #buildFadeInAnimator(View...)} and {@link #buildSlideInRightAnimator(View)} for more information.
     * @param v the target view
     * @return the animator
     */
    public Animator buildFadeSlideInRightAnimator(View v) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(buildFadeInAnimator(v), buildSlideInRightAnimator(v));
        return set;
    }

    /**
     * Builds an animator that fades in and slides in the target view from the bottom (performed simultanously).
     * See {@link #buildFadeInAnimator(View...)} and {@link #buildSlideInRightAnimator(View)} for more information.
     * @param v the target view
     * @return the animator
     */
    public Animator buildFadeSlideInBottomAnimator(View v) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(buildFadeInAnimator(v), buildSlideInBottomAnimator(v));
        return set;
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
     * Builds an animator that scales in the target view from 0 to its original size.
     * @param v the target view
     * @return the animator
     */
    public Animator buildScaleInAnimator(View v) {
        ObjectAnimator scaleAnimator = new ObjectAnimator();
        scaleAnimator.setDuration(600);
        scaleAnimator.setTarget(v);
        scaleAnimator.setInterpolator(new LinearInterpolator());

        scaleAnimator.setValues(
                PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 0f, 1f));

        return scaleAnimator;
    }

    /**
     * Builds an animator that that slides in the target view from the top.
     * @param v the target view
     * @return the animator
     */
    public Animator buildSlideInTopAnimator(View v) {
        ObjectAnimator slideAnimator = ObjectAnimator.ofFloat(v, "translationY", -Utils.percentScreenHeightToPx(v.getContext(), 100), 0);
        slideAnimator.setDuration(800);
        slideAnimator.setInterpolator(new DecelerateInterpolator());
        return slideAnimator;
    }

    /**
     * Builds an animator that that slides in the target view from the bottom.
     * @param v the target view
     * @return the animator
     */
    public Animator buildSlideInBottomAnimator(View v) {
        ObjectAnimator slideAnimator = ObjectAnimator.ofFloat(v, "translationY", Utils.percentScreenHeightToPx(v.getContext(), 100), 0);
        slideAnimator.setDuration(800);
        slideAnimator.setInterpolator(new DecelerateInterpolator());
        return slideAnimator;
    }

    /**
     * Builds an animator that that slides out the target view to the bottom.
     * @param delay the delay in ms to start sliding
     * @param v the target view
     * @param originY the original translation value (typically 0, but may be another value if the target view is already translated)
     * @return the animator
     */
    public Animator buildSlideOutBottomAnimator(int delay, View v, float originY) {
        ObjectAnimator slideAnimator = ObjectAnimator.ofFloat(v, "translationY", originY, Utils.percentScreenHeightToPx(v.getContext(), 100));
        slideAnimator.setStartDelay(delay);
        slideAnimator.setDuration(800);
        slideAnimator.setInterpolator(new DecelerateInterpolator());
        return slideAnimator;
    }

    /**
     * Builds an animator that that slides in the target view from the right.
     * @param v the target view
     * @return the animator
     */
    public Animator buildSlideInRightAnimator(View v) {
        ObjectAnimator slideAnimator = ObjectAnimator.ofFloat(v, "translationX", Utils.percentScreenWidthToPx(v.getContext(), 100), 0);
        slideAnimator.setDuration(800);
        slideAnimator.setInterpolator(new DecelerateInterpolator());
        return slideAnimator;
    }

    /**
     * Builds an animator that that slides out the target view to the left.
     * @param delay the delay in ms to start sliding
     * @param v the target view
     * @param originX the original translation value (typically 0, but may be another value if the target view is already translated)
     * @return the animator
     */
    public Animator buildSlideOutLeftAnimator(int delay, View v, float originX) {
        ObjectAnimator slideAnimator = ObjectAnimator.ofFloat(v, "translationX", originX, Utils.percentScreenWidthToPx(v.getContext(), -100));
        slideAnimator.setStartDelay(delay);
        slideAnimator.setDuration(800);
        slideAnimator.setInterpolator(new DecelerateInterpolator());
        return slideAnimator;
    }

    /**
     * Builds an animator that slides out simultanously all the target views to the left.
     * See {@link #buildSlideOutLeftAnimator(int, View, float)} for more information.
     * @param delay the delay in ms to start sliding
     * @param incrementalDelay the delay to start sliding between target views (multiplied by the index of the current view)
     * @param views the set of target views
     * @return the animator
     */
    public Animator buildSlideOutLeftAnimator(int delay, int incrementalDelay, View... views) {
        AnimatorSet set = new AnimatorSet();
        List<Animator> animatorList = new ArrayList<>();

        for(int i=0; i < views.length; i++) {
            Animator a = buildSlideOutLeftAnimator(delay, views[i], 0);
            a.setStartDelay(delay + (i * incrementalDelay));
            animatorList.add(a);
        }

        set.playTogether(animatorList);
        return set;
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

    /**
     * Builds an animator that slides out simultaneously all target views to the right.
     * See {@link #buildSlideOutRightAnimator(int, View, float)} for more information.
     * @param delay the delay in ms to start sliding
     * @param incrementalDelay the delay to start sliding between target views (multiplied by the index of the current view)
     * @param views the set of target views
     * @return the animator
     */
    public Animator buildSlideOutRightAnimator(int delay, int incrementalDelay, View... views) {
        AnimatorSet set = new AnimatorSet();
        List<Animator> animatorList = new ArrayList<>();

        for(int i=0; i < views.length; i++) {
            Animator a = buildSlideOutRightAnimator(delay, views[i], 0);
            a.setStartDelay(delay + (i * incrementalDelay));
            animatorList.add(a);
        }

        set.playTogether(animatorList);
        return set;
    }

}
