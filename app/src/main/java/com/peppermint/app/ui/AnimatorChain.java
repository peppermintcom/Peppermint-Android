package com.peppermint.app.ui;

import android.animation.Animator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 18-02-2016.
 */
public class AnimatorChain implements Animator.AnimatorListener {

    private Animator.AnimatorListener mAnimatorListener;
    private int mCurrentStayIndex = 0;
    private int mCurrentAnimatorIndex = 0;
    private List<Animator> mAnimatorList = new ArrayList<>();

    public AnimatorChain(Animator... animators) {
        for(Animator animator : animators) {
            animator.addListener(this);
            mAnimatorList.add(animator);
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {
        if(mCurrentAnimatorIndex <= 0) {
            mAnimatorListener.onAnimationStart(null);
        }
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        playNext(true);
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        playNext(true);
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }

    private void playNext(boolean definitelyFinished) {
        if(!definitelyFinished && mAnimatorList.get(mCurrentAnimatorIndex).isStarted()) {
            return;
        }

        if(mCurrentAnimatorIndex >= (mAnimatorList.size() - 1)) {
            // finished
            mAnimatorListener.onAnimationEnd(null);
        }

        if(mCurrentAnimatorIndex >= mCurrentStayIndex) {
            return;
        }

        mCurrentAnimatorIndex++;

        if(mCurrentAnimatorIndex < mAnimatorList.size()) {
            // play
            mAnimatorList.get(mCurrentAnimatorIndex).start();
        }
    }

    public void allowNext(boolean doCancelCurrent) {
        mCurrentStayIndex++;

        if(doCancelCurrent && mAnimatorList.get(mCurrentAnimatorIndex).isStarted()) {
            mAnimatorList.get(mCurrentAnimatorIndex).cancel();
        } else {
            playNext(false);
        }
    }

    public void start() {
        if(mAnimatorList.size() <= 0) {
            return;
        }

        mAnimatorList.get(0).start();
    }

    public Animator.AnimatorListener getAnimatorListener() {
        return mAnimatorListener;
    }

    public void setAnimatorListener(Animator.AnimatorListener mAnimatorListener) {
        this.mAnimatorListener = mAnimatorListener;
    }
}
