package com.peppermint.app.ui.base;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewPropertyAnimator;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.loading.LoadingView;

/**
 * Created by Nuno Luz on 06-04-2016.
 *
 * Controls the visibility of a loading view in an activity.
 */
public class LoadingController {

    protected Activity mActivity;
    protected int mViewResource, mContainerResource;
    protected boolean mLoading = false;

    public LoadingController(Activity mActivity, int mContainerResource, int mViewResource) {
        this.mActivity = mActivity;
        this.mContainerResource = mContainerResource;
        this.mViewResource = mViewResource;
    }

    public void setText(int textResourceId) {
        setText(mActivity.getString(textResourceId));
    }

    public void setText(CharSequence text) {
        getView().setProgressText(text == null ? null : text.toString());
    }

    public void setLoading(boolean value) {
        final View containerView = getContainerView();
        final LoadingView view = getView();

        if(value && !mLoading) {
            view.startAnimations();
            view.startDrawingThread();

            containerView.setVisibility(View.VISIBLE);
            containerView.animate().alpha(1f);

        } else if(!value && mLoading) {
            ViewPropertyAnimator animator = containerView.animate().alpha(0f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                animator.withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        containerView.setVisibility(View.GONE);
                    }
                });
            } else {
                containerView.setVisibility(View.GONE);
            }

            view.stopAnimations();
            view.stopDrawingThread();
        }

        mLoading = value;
    }

    public LoadingView getView() {
        return (LoadingView) mActivity.findViewById(R.id.loading);
    }

    public View getContainerView() {
        return mActivity.findViewById(R.id.fragmentProgressContainer);
    }
}
