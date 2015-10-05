package com.peppermint.app.ui.animations;

import android.content.Context;
import android.graphics.Canvas;

import java.util.Collection;
import java.util.List;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * Represents a set (played sequentially or together) of CanvasAnimation.
 */
public class CanvasAnimationSet extends BaseCanvasAnimation implements CanvasAnimation, CanvasAnimationListener {

    private boolean mSequence = false;
    private CanvasAnimation[] mAnimations;
    private int mCurrentAnimationIndex = 0;

    public CanvasAnimationSet(Context context) {
        super(context, null, Long.MAX_VALUE);
    }

    @Override
    public void apply(Canvas canvas) {
        if(mSequence) {
            mAnimations[mCurrentAnimationIndex].apply(canvas);
        } else {
            for(CanvasAnimation animation : mAnimations) {
                animation.apply(canvas);
            }
        }
    }

    @Override
    protected void apply(Canvas canvas, double interpolatedElapsedTime) {
        // nothing to do here (overriden invocation at apply(Canvas))
    }

    public void play(CanvasAnimation animation) {
        mAnimations = new CanvasAnimation[1];
        mAnimations[0] = animation;
    }

    public void playSequentially(CanvasAnimation... animationList) {
        mAnimations = animationList;
        mSequence = true;
    }

    public void playSequentially(List<CanvasAnimation> animationList) {
        playSequentially(animationList.toArray(new CanvasAnimation[animationList.size()]));
    }

    public void playTogether(CanvasAnimation... animationList) {
        mAnimations = animationList;
        mSequence = false;
    }

    public void playTogether(Collection<CanvasAnimation> animationSet) {
        playTogether(animationSet.toArray(new CanvasAnimation[animationSet.size()]));
    }

    @Override
    public void onAnimationStarted(CanvasAnimation animation) {
    }

    @Override
    public void onAnimationApplied(CanvasAnimation animation) {
    }

    @Override
    public void onAnimationEnded(CanvasAnimation animation) {
        if(animation.equals(mAnimations[mCurrentAnimationIndex])) {
            mCurrentAnimationIndex++;
        }
    }
}
