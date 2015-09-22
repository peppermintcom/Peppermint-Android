package com.peppermint.app.ui.tutorial;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.peppermint.app.MainActivity;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;

import java.util.ArrayList;
import java.util.List;

public class TutorialActivity extends Activity implements FragmentManager.OnBackStackChangedListener, View.OnTouchListener {

    public static class TutorialFragment extends Fragment {
        protected TutorialActivity mActivity;
        public TutorialFragment() {
            super();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            this.mActivity = (TutorialActivity) activity;
        }
    }

    private float x1, x2;
    private static final int MIN_DISTANCE = 150;

    private List<Class<?>> mFragmentClassList = new ArrayList<>();
    private int[] mPaginatorRes;
    private TutorialFragment mCurrentFragment;
    private ImageView mImgPaginator;

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

        TextView txtPeppermint = (TextView) findViewById(R.id.txtPeppermint);
        txtPeppermint.setTypeface(((PeppermintApp) getApplication()).getFontSemibold());

        mImgPaginator = (ImageView) findViewById(R.id.imgPaginator);

        getFragmentManager().addOnBackStackChangedListener(this);

        if (savedInstanceState != null) {
            mCurrentFragment = (TutorialFragment) getFragmentManager().findFragmentById(R.id.container);
            return; // avoids duplicate fragments
        }

        View lytRoot = findViewById(R.id.lytRoot);
        lytRoot.setOnTouchListener(this);

        // show intro screen
        mCurrentFragment = getFragmentInstance(0);
        mImgPaginator.setImageResource(mPaginatorRes[0]);
        getFragmentManager().beginTransaction().add(R.id.container, mCurrentFragment, mCurrentFragment.getClass().getName()).commit();
    }

    public void nextFragment() {
        int newPosition = mFragmentClassList.indexOf(mCurrentFragment.getClass()) + 1;

        if(newPosition >= mFragmentClassList.size()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
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
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    // left to right swipe action
                    if (x2 > x1) {
                        previousFragment();
                    }
                    // right to left swipe action
                    else {
                        nextFragment();
                    }
                } else {
                    // tap
                    nextFragment();
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}
