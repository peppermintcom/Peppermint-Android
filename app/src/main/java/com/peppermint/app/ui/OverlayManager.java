package com.peppermint.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nuno Luz on 04-02-2016.
 */
public class OverlayManager {

    private class OverlayHideAnimatorListener extends AnimatorListenerAdapter {
        private OverlayWrapper ow;
        private boolean isCancel = false;

        public OverlayHideAnimatorListener(OverlayWrapper ow, boolean isCancel) {
            this.ow = ow;
            this.isCancel = isCancel;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            ow.view.setVisibility(View.INVISIBLE);
            mOverlayHidding.remove(ow.view.getTag());
            if(ow.disableAllTouch) {
                enableDisableAllTouch(true);
            }
            if(ow.disableAutoScreenRotation) {
                mActivity.setRequestedOrientation(ow.requestedOrientation);
            }
            if(isCancel && ow.onOverlayCancel != null) {
                ow.onOverlayCancel.run();
            }
            ow.onOverlayCancel = null;

            boolean allHidden = true;
            for(Map.Entry<String, OverlayWrapper> entry : mOverlayMap.entrySet()) {
                if(entry.getValue().view.getVisibility() == View.VISIBLE) {
                    allHidden = false;
                }
            }
            if (allHidden) {
                //setFullscreen(false);
                mLytOverlayContainer.setVisibility(View.GONE);
            }
        }
    }

    private class OverlayWrapper {
        View view; boolean disableAllTouch, disableAutoScreenRotation;
        int requestedOrientation;
        Runnable onOverlayCancel;
    }

    private Activity mActivity;
    private AnimatorBuilder mAnimatorBuilder;
    private Handler mDelayHandler = new Handler();

    private FrameLayout mLytOverlayContainer;
    private int mCachedScreenOrientation;

    private Map<String, OverlayWrapper> mOverlayMap = new HashMap<>();
    private Set<String> mOverlayHidding = new HashSet<>();

    public OverlayManager(Activity activityWithOverlays) {
        this.mActivity = activityWithOverlays;
        this.mAnimatorBuilder = new AnimatorBuilder();
    }

    public OverlayManager(Activity activityWithOverlays, int overlayContainerResId) {
        this(activityWithOverlays);
        this.mLytOverlayContainer = (FrameLayout) activityWithOverlays.findViewById(overlayContainerResId);
    }

    /**
     * Create the overlay layout (inflated from the layout resource id)
     * @param layoutRes the layout resource id
     * @param tag the tag of the overlay
     * @return the root view containing the inflated layout
     */
    public View createOverlay(int layoutRes, String tag, boolean disableAllTouch, boolean disableAutoScreenRotation) {
        if(mOverlayMap.containsKey(tag)) {
            return mOverlayMap.get(tag).view;
        }

        LayoutInflater layoutInflater = mActivity.getLayoutInflater();
        View overlayView =  layoutInflater.inflate(layoutRes, null);
        return createOverlay(overlayView, tag, disableAllTouch, disableAutoScreenRotation);
    }

    public View createOverlay(View overlayView, String tag, boolean disableAllTouch, boolean disableAutoScreenRotation) {
        if(mOverlayMap.containsKey(tag)) {
            return mOverlayMap.get(tag).view;
        }

        overlayView.setTag(tag);
        overlayView.setVisibility(View.INVISIBLE);

        OverlayWrapper ow = new OverlayWrapper();
        ow.view = overlayView;
        ow.disableAllTouch = disableAllTouch;
        ow.disableAutoScreenRotation = disableAutoScreenRotation;

        mOverlayMap.put(tag, ow);

        mLytOverlayContainer.addView(overlayView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return overlayView;
    }

    /**
     * Sets the overlay associated with the tag to visible
     * @return true if the visibility of the overlay changed
     */
    public boolean showOverlay(String tag, boolean animated, Runnable onOverlayCancel) {
        if(!mOverlayMap.containsKey(tag)) {
            throw new RuntimeException("Overlay with tag " + tag + " does not exist!");
        }

        OverlayWrapper ow = mOverlayMap.get(tag);
        ow.onOverlayCancel = onOverlayCancel;
        if(ow.view.getVisibility() == View.VISIBLE) {
            return false;
        }

        // setFullscreen(true);
        mLytOverlayContainer.setVisibility(View.VISIBLE);
        ow.view.setVisibility(View.VISIBLE);

        if(ow.disableAllTouch) {
            enableDisableAllTouch(false);
        }

        if(ow.disableAutoScreenRotation) {
            ow.requestedOrientation = mActivity.getRequestedOrientation();
            //noinspection ResourceType
            mActivity.setRequestedOrientation(getActivityInfoOrientation());
        }

        if(animated) {
            Animator anim = mAnimatorBuilder.buildFadeInAnimator(ow.view);
            anim.setDuration(400);
            anim.start();
        }

        return true;
    }

    /**
     * Shows all overlays. See {@link #showOverlay(String, boolean, Runnable)} for more information.
     * @return true if at least one overlay was shown; false otherwise
     */
    public boolean showAllOverlays(boolean animated, Runnable onOverlayCancel) {
        boolean changed = false;

        for(String tag : mOverlayMap.keySet()) {
            if(showOverlay(tag, animated, onOverlayCancel)) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Sets the overlay associated with the tag to invisible
     * @return true if the visibility of the overlay changed
     */
    public boolean hideOverlay(String tag, long delay, final boolean animated) {
        return hideOverlay(tag, delay, animated, false);
    }

    protected boolean hideOverlay(String tag, long delay, final boolean animated, boolean isCancel) {
        if(!mOverlayMap.containsKey(tag)) {
            throw new RuntimeException("Overlay with tag " + tag + " does not exist!");
        }

        final OverlayWrapper ow = mOverlayMap.get(tag);
        if(ow.view.getVisibility() == View.INVISIBLE || mOverlayHidding.contains(tag)) {
            return false;
        }

        final OverlayHideAnimatorListener overlayListener = new OverlayHideAnimatorListener(ow, isCancel);
        mOverlayHidding.add(tag);

        mDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(animated) {
                    Animator anim = mAnimatorBuilder.buildFadeOutAnimator(ow.view);
                    anim.setDuration(400);
                    anim.addListener(overlayListener);
                    anim.start();
                } else {
                    overlayListener.onAnimationEnd(null);
                }
            }
        }, delay);

        return true;
    }

    /**
     * Hides all overlays. See {@link #hideOverlay(String, long, boolean)} for more information.
     * @param delay a delay before starting the hidding animation
     * @return true if at least one overlay was hidden; false otherwise
     */
    public boolean hideAllOverlays(long delay, boolean animated) {
        return hideAllOverlays(delay, animated, false);
    }

    public boolean hideAllOverlays(long delay, boolean animated, boolean isCancel) {
        boolean changed = false;

        for(String tag : mOverlayMap.keySet()) {
            if(hideOverlay(tag, delay, animated, isCancel)) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Destroys the overlay associated with the tag
     * @param tag the tag of the overlay
     */
    public void destroyOverlay(String tag) {
        if(!mOverlayMap.containsKey(tag)) {
            throw new RuntimeException("Overlay view with tag " + tag + " does not exist!");
        }

        View v = mOverlayMap.get(tag).view;
        mOverlayMap.remove(tag);
        mOverlayHidding.remove(tag);

        mLytOverlayContainer.removeView(v);
    }

    protected void lockOrientation() {
        mCachedScreenOrientation = mActivity.getRequestedOrientation();
        //noinspection ResourceType
        mActivity.setRequestedOrientation(getActivityInfoOrientation());
    }

    protected void unlockOrientation() {
        mActivity.setRequestedOrientation(mCachedScreenOrientation);
    }

    private int getActivityInfoOrientation() {
        // rotation depends on devices natural orientation (in tablets it's landscape; portrait on phones)
        // thus, 0 rotation is landscape on tablets and portrait on phones
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        if(mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
            return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }

        if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    }

    /**
     * Enables/disables touch capture events in the whole screen.
     * @param enabled true to enable; false to disable
     */
    public void enableDisableAllTouch(boolean enabled) {
        enableDisableTouch((ViewGroup) mActivity.getWindow().getDecorView(), enabled);
    }

    /**
     * Enables/disables touch capture in the specified ViewGroup
     * @param vg the viewgroup to be enabled/disabled
     * @param enabled true to enable; false to disable
     */
    protected void enableDisableTouch(ViewGroup vg, boolean enabled) {
        for (int i = 0; i < vg.getChildCount(); i++){
            View child = vg.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup){
                enableDisableTouch((ViewGroup) child, enabled);
            }
        }
    }

    /*private void setFullscreen(boolean fullscreen) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        } else {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
    }*/
}
