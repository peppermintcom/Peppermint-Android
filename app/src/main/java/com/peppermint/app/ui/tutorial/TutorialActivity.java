package com.peppermint.app.ui.tutorial;

import android.animation.Animator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.utils.AnimatorBuilder;

import java.util.ArrayList;
import java.util.List;

public class TutorialActivity extends Activity implements FragmentManager.OnBackStackChangedListener, View.OnTouchListener, View.OnClickListener {

    public static class TutorialFragment extends Fragment {
        protected TutorialActivity mActivity;
        public TutorialFragment() {
            super();
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            this.mActivity = (TutorialActivity) activity;
        }
    }

    // swipe-related
    private float x1, x2, y1, y2;
    private static final int MIN_DISTANCE = 150;        // min swipe distance

    private List<Class<?>> mFragmentClassList = new ArrayList<>();
    private int[] mPaginatorRes;
    private TutorialFragment mCurrentFragment;
    private ImageView mImgPaginator;
    private Button mBtnContinue;

    private AnimatorBuilder mAnimatorBuilder;
    private Animator mContinueAnimator;

    public TutorialActivity() {
        mFragmentClassList.add(T1PickRecipientFragment.class);
        mFragmentClassList.add(T2RecordFragment.class);
        mFragmentClassList.add(T3ReceiveFragment.class);

        mPaginatorRes = new int[]{R.drawable.img_paginator1, R.drawable.img_paginator2, R.drawable.img_paginator3};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_tutorial_layout);

        mAnimatorBuilder = new AnimatorBuilder();

        TextView txtPeppermint = (TextView) findViewById(R.id.txtPeppermint);
        txtPeppermint.setTypeface(((PeppermintApp) getApplication()).getFontSemibold());

        mImgPaginator = (ImageView) findViewById(R.id.imgPaginator);
        mBtnContinue = (Button) findViewById(R.id.btnContinue);
        mBtnContinue.setVisibility(View.INVISIBLE);
        mBtnContinue.setTypeface(((PeppermintApp) getApplication()).getFontSemibold());
        mBtnContinue.setOnClickListener(this);

        getFragmentManager().addOnBackStackChangedListener(this);

        View lytRoot = findViewById(R.id.lytRoot);
        lytRoot.setOnTouchListener(this);

        if (savedInstanceState != null) {
            mCurrentFragment = (TutorialFragment) getFragmentManager().findFragmentById(R.id.container);
            return; // avoids duplicate fragments
        }

        // show intro screen
        mCurrentFragment = getFragmentInstance(0);
        mImgPaginator.setImageResource(mPaginatorRes[0]);
        getFragmentManager().beginTransaction().add(R.id.container, mCurrentFragment, mCurrentFragment.getClass().getName()).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mContinueAnimator != null && mContinueAnimator.isRunning()) {
            mContinueAnimator.cancel();
        }
    }

    public void nextFragment() {
        int newPosition = mFragmentClassList.indexOf(mCurrentFragment.getClass()) + 1;

        if(newPosition >= mFragmentClassList.size()) {
            finish();
            return;
        }

        try {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            mCurrentFragment = getFragmentInstance(newPosition);
            ft.setCustomAnimations(R.anim.slide_in_right_fragment, R.anim.slide_out_left_fragment, R.anim.slide_in_left_fragment, R.anim.slide_out_right_fragment);
            ft.replace(R.id.container, mCurrentFragment, mCurrentFragment.getClass().getName());
            ft.addToBackStack(mCurrentFragment.getClass().getName());
            ft.commit();
            mImgPaginator.setImageResource(mPaginatorRes[newPosition]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void previousFragment() {
        int currentPos = mFragmentClassList.indexOf(mCurrentFragment.getClass());
        if(currentPos > 0) {
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left_activity, R.anim.slide_out_right_activity);
    }

    @Override
    public void onBackStackChanged() {
        mCurrentFragment = (TutorialFragment) getFragmentManager().findFragmentById(R.id.container);
        int currentPos = mFragmentClassList.indexOf(mCurrentFragment.getClass());
        // set final continue button visibility
        if(currentPos >= (mFragmentClassList.size()-1)) {
            if(mContinueAnimator != null && mContinueAnimator.isRunning()) {
                mContinueAnimator.cancel();
            }
            mBtnContinue.setVisibility(View.VISIBLE);
            mContinueAnimator = mAnimatorBuilder.buildFadeInAnimator(mBtnContinue);
            mContinueAnimator.start();
        } else if(mBtnContinue.getVisibility() == View.VISIBLE) {
            if(mContinueAnimator != null && mContinueAnimator.isRunning()) {
                mContinueAnimator.cancel();
            }
            mContinueAnimator = mAnimatorBuilder.buildFadeOutAnimator(mBtnContinue);
            mContinueAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) { }
                @Override
                public void onAnimationEnd(Animator animation) { mBtnContinue.setVisibility(View.INVISIBLE); }
                @Override
                public void onAnimationCancel(Animator animation) { mBtnContinue.setVisibility(View.INVISIBLE); }
                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            mContinueAnimator.start();
        }

        mImgPaginator.setImageResource(mPaginatorRes[currentPos]);
    }

    private TutorialFragment getFragmentInstance(int pos) {
        try {
            return (TutorialFragment) mFragmentClassList.get(pos).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate fragment!", e);
        }
    }

    @Override
    public void onClick(View v) {
        nextFragment();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                y2 = event.getY();
                float deltaX = x2 - x1;
                float deltaY = y2 - y1;

                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    // left to right swipe action
                    if (x2 > x1) {
                        previousFragment();
                    }
                    // right to left swipe action
                    else {
                        nextFragment();
                    }
                } else if(Math.abs(deltaY) < MIN_DISTANCE) {
                    // tap
                    nextFragment();
                }
                break;
        }

        return super.onTouchEvent(event);
    }
}
