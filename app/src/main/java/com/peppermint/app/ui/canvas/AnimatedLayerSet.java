package com.peppermint.app.ui.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import java.util.Collection;
import java.util.List;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * Represents a set (played sequentially or together) of {@link AnimatedLayer}s.
 */
public class AnimatedLayerSet extends AnimatedLayerBase implements AnimatedLayer, AnimatedLayerListener {

    private boolean mSequence = false;
    private AnimatedLayer[] mAnimations;
    private int mCurrentAnimationIndex = 0;

    public AnimatedLayerSet(Context context) {
        super(context, null, Long.MAX_VALUE);
    }

    @Override
    public void draw(View view, Canvas canvas) {
        for(AnimatedLayer animation : mAnimations) {
            animation.draw(view, canvas);
        }
    }

    @Override
    public void start() {
        super.start();
        if(mSequence) {
            mAnimations[mCurrentAnimationIndex].addAnimationListener(this);
            mAnimations[mCurrentAnimationIndex].start();
        } else {
            for(AnimatedLayer animation : mAnimations) {
                animation.start();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if(mSequence) {
            mAnimations[mCurrentAnimationIndex].removeAnimationListener(this);
            mAnimations[mCurrentAnimationIndex].stop();
        } else {
            for(AnimatedLayer animation : mAnimations) {
                animation.stop();
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        for(AnimatedLayer animation : mAnimations) {
            animation.reset();
        }
    }

    @Override
    public boolean isRunning() {
        for(AnimatedLayer animation : mAnimations) {
            if(animation.isRunning()) {
                return true;
            }
        }
        return false;
    }

    public void play(AnimatedLayer animation) {
        mAnimations = new AnimatedLayer[1];
        mAnimations[0] = animation;
    }

    public void playSequentially(AnimatedLayer... animationList) {
        mAnimations = animationList;
        mSequence = true;
    }

    public void playSequentially(List<AnimatedLayer> animationList) {
        playSequentially(animationList.toArray(new AnimatedLayer[animationList.size()]));
    }

    public void playTogether(AnimatedLayer... animationList) {
        mAnimations = animationList;
        mSequence = false;
    }

    public void playTogether(Collection<AnimatedLayer> animationSet) {
        playTogether(animationSet.toArray(new AnimatedLayer[animationSet.size()]));
    }

    @Override
    public void onAnimationStarted(AnimatedLayer animatedLayer) {
    }

    @Override
    public void onAnimationEnded(AnimatedLayer animatedLayer) {
        if(animatedLayer.equals(mAnimations[mCurrentAnimationIndex])) {
            animatedLayer.removeAnimationListener(this);
            mCurrentAnimationIndex++;

            if(mCurrentAnimationIndex >= mAnimations.length && isLooping()) {
                mCurrentAnimationIndex = 0;
            }

            if(mCurrentAnimationIndex < mAnimations.length) {
                mAnimations[mCurrentAnimationIndex].addAnimationListener(this);
                mAnimations[mCurrentAnimationIndex].start();
            }
        }
    }
}
