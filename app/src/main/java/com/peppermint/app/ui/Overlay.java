package com.peppermint.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by Nuno Luz on 06-02-2016.
 */
public class Overlay {

    private int mViewResId;
    private View mView;
    private boolean mDisableAllTouch, mDisableAutoScreenRotation;
    private int mRequestedOrientation;

    private String mId;

    private OverlayManager mOverlayManager;
    private boolean mCreated = false;

    public Overlay() {
    }

    public Overlay(String mId, int mViewResId, boolean mDisableAllTouch, boolean mDisableAutoScreenRotation) {
        this();
        this.mId = mId;
        this.mViewResId = mViewResId;
        this.mDisableAllTouch = mDisableAllTouch;
        this.mDisableAutoScreenRotation = mDisableAutoScreenRotation;
    }

    protected View create(Context context) {
        if(mCreated) {
            return mView;
        }

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        mView = onCreateView(layoutInflater);
        mCreated = true;
        return mView;
    }

    protected void destroy() {
        if(mCreated) {
            onDestroy();
            mCreated = false;
        }
    }

    public boolean show(boolean animated) {
        return mOverlayManager.showOverlay(mId, animated);
    }

    public boolean hide(boolean animated, long delayMs, boolean cancel) {
        return mOverlayManager.hideOverlay(mId, delayMs, animated, cancel);
    }

    protected View onCreateView(LayoutInflater layoutInflater) {
        return layoutInflater.inflate(mViewResId, null);
    }

    public void onDestroy() { }

    public View getView() {
        return mView;
    }

    public void setView(View mView) {
        this.mView = mView;
    }

    public boolean isDisableAllTouch() {
        return mDisableAllTouch;
    }

    public void setDisableAllTouch(boolean mDisableAllTouch) {
        this.mDisableAllTouch = mDisableAllTouch;
    }

    public boolean isDisableAutoScreenRotation() {
        return mDisableAutoScreenRotation;
    }

    public void setDisableAutoScreenRotation(boolean mDisableAutoScreenRotation) {
        this.mDisableAutoScreenRotation = mDisableAutoScreenRotation;
    }

    public int getRequestedOrientation() {
        return mRequestedOrientation;
    }

    public void setRequestedOrientation(int mRequestedOrientation) {
        this.mRequestedOrientation = mRequestedOrientation;
    }

    public OverlayManager getOverlayManager() {
        return mOverlayManager;
    }

    public void setOverlayManager(OverlayManager mOverlayManager) {
        this.mOverlayManager = mOverlayManager;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public boolean isVisible() {
        return mView != null && mView.getVisibility() == View.VISIBLE;
    }

    public void assimilateFrom(Overlay overlay) {
    }

}
