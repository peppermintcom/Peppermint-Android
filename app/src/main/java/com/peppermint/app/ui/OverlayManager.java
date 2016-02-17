package com.peppermint.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peppermint.app.tracking.TrackerManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nuno Luz on 04-02-2016.
 */
public class OverlayManager {

    public interface OverlayVisibilityChangeListener {
        void onOverlayShown(Overlay overlay);
        void onOverlayHidden(Overlay overlay, boolean wasCancelled);
    }

    private class OverlayHideAnimatorListener extends AnimatorListenerAdapter {
        private Overlay mOverlay;
        private boolean mCancel = false;

        public OverlayHideAnimatorListener(Overlay overlay, boolean cancel) {
            this.mOverlay = overlay;
            this.mCancel = cancel;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mOverlay.getView().setVisibility(View.INVISIBLE);
            mOverlayHidding.remove(mOverlay.getId());
            if(mOverlay.isDisableAllTouch()) {
                enableDisableAllTouch(true);
            }
            if(mOverlay.isDisableAutoScreenRotation()) {
                //noinspection ResourceType
                mActivity.setRequestedOrientation(mOverlay.getRequestedOrientation());
            }

            boolean allHidden = true;
            for(Map.Entry<String, Overlay> entry : mOverlayMap.entrySet()) {
                if(entry.getValue().isVisible()) {
                    allHidden = false;
                }
            }
            if (allHidden) {
                //setFullscreen(false);
                mLytOverlayContainer.setVisibility(View.GONE);
            }

            if(mOverlay.getId() != null && mRootScreenId != null) {
                TrackerManager.getInstance(mActivity.getApplicationContext()).trackScreenView(mRootScreenId);
            }

            for(OverlayVisibilityChangeListener listener : mOverlayVisibilityChangeListenerList) {
                listener.onOverlayHidden(mOverlay, mCancel);
            }
        }
    }

    private String mRootScreenId;
    private Activity mActivity;
    private AnimatorBuilder mAnimatorBuilder;
    private Handler mDelayHandler = new Handler();

    private FrameLayout mLytOverlayContainer;
    private int mCachedScreenOrientation;

    private Map<String, Overlay> mOverlayMap = new HashMap<>();
    private Set<String> mOverlayHidding = new HashSet<>();

    private List<OverlayVisibilityChangeListener> mOverlayVisibilityChangeListenerList = new ArrayList<>();

    public OverlayManager(Activity activityWithOverlays, String rootScreenId) {
        this.mActivity = activityWithOverlays;
        this.mAnimatorBuilder = new AnimatorBuilder();
        this.mRootScreenId = rootScreenId;
    }

    public OverlayManager(Activity activityWithOverlays, String rootScreenId, int overlayContainerResId) {
        this(activityWithOverlays, rootScreenId);
        this.mLytOverlayContainer = (FrameLayout) activityWithOverlays.findViewById(overlayContainerResId);
    }

    /**
     * Create the overlay layout (inflated from the layout resource id)
     * @param layoutRes the layout resource id
     * @param id the id of the overlay
     * @return the root view containing the inflated layout
     */
    public Overlay createOverlay(int layoutRes, String id, boolean disableAllTouch, boolean disableAutoScreenRotation) {
        return createOverlay(new Overlay(id, layoutRes, disableAllTouch, disableAutoScreenRotation));
    }

    public Overlay createOverlay(Overlay overlay) {
        if(mOverlayMap.containsKey(overlay.getId())) {
            Log.d(overlay.getId(), "Overlay already exists! Ignoring createOverlay()...");
            Overlay oldOverlay = mOverlayMap.get(overlay.getId());
            oldOverlay.assimilateFrom(overlay);
            return oldOverlay;
        }

        overlay.setOverlayManager(this);
        mOverlayMap.put(overlay.getId(), overlay);

        View v = overlay.create(mActivity);
        v.setVisibility(View.INVISIBLE);

        mLytOverlayContainer.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return overlay;
    }

    /**
     * Sets the overlay associated with the tag to visible
     * @return true if the visibility of the overlay changed
     */
    public boolean showOverlay(String id, boolean animated) {
        if(!mOverlayMap.containsKey(id)) {
            throw new RuntimeException("Overlay " + id + " does not exist!");
        }

        Overlay overlay = mOverlayMap.get(id);
        if(overlay.isVisible()) {
            return false;
        }

        mLytOverlayContainer.setVisibility(View.VISIBLE);
        overlay.getView().setVisibility(View.VISIBLE);

        if(overlay.isDisableAllTouch()) {
            enableDisableAllTouch(false);
        }

        if(overlay.isDisableAutoScreenRotation()) {
            overlay.setRequestedOrientation(mActivity.getRequestedOrientation());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            } else {
                //noinspection ResourceType
                mActivity.setRequestedOrientation(getActivityInfoOrientation() | ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            }
        }

        if(animated) {
            Animator anim = mAnimatorBuilder.buildFadeInAnimator(overlay.getView());
            anim.setDuration(400);
            anim.start();
        }

        if(overlay.getId() != null) {
            TrackerManager.getInstance(mActivity.getApplicationContext()).trackScreenView(overlay.getId());
        }

        for(OverlayVisibilityChangeListener listener : mOverlayVisibilityChangeListenerList) {
            listener.onOverlayShown(overlay);
        }

        return true;
    }

    /**
     * Shows all overlays. See {@link #showOverlay(String, boolean)} for more information.
     * @return true if at least one overlay was shown; false otherwise
     */
    public boolean showAllOverlays(boolean animated) {
        boolean changed = false;

        for(String id : mOverlayMap.keySet()) {
            if(showOverlay(id, animated)) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Sets the overlay associated with the tag to invisible
     * @return true if the visibility of the overlay changed
     */
    public boolean hideOverlay(String id, long delay, final boolean animated) {
        return hideOverlay(id, delay, animated, false);
    }

    protected boolean hideOverlay(String id, long delay, final boolean animated, boolean isCancel) {
        if(!mOverlayMap.containsKey(id)) {
            throw new RuntimeException("Overlay " + id + " does not exist!");
        }

        final Overlay overlay = mOverlayMap.get(id);
        if(!overlay.isVisible() || mOverlayHidding.contains(overlay.getId())) {
            return false;
        }

        final OverlayHideAnimatorListener overlayListener = new OverlayHideAnimatorListener(overlay, isCancel);
        mOverlayHidding.add(overlay.getId());

        mDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(animated) {
                    Animator anim = mAnimatorBuilder.buildFadeOutAnimator(overlay.getView());
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

        for(String id : mOverlayMap.keySet()) {
            if(hideOverlay(id, delay, animated, isCancel)) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Destroys the overlay associated with the tag
     * @param id the tag of the overlay
     */
    public void destroyOverlay(String id) {
        if(!mOverlayMap.containsKey(id)) {
            throw new RuntimeException("Overlay " + id + " does not exist!");
        }

        View v = mOverlayMap.get(id).getView();
        Overlay overlay = mOverlayMap.remove(id);
        mOverlayHidding.remove(id);

        mLytOverlayContainer.removeView(v);

        overlay.destroy();
    }

    public void destroyAllOverlays() {
        for(String id : mOverlayMap.keySet()) {
            destroyOverlay(id);
        }
    }

    protected void lockOrientation() {
        mCachedScreenOrientation = mActivity.getRequestedOrientation();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        } else {
            //noinspection ResourceType
            mActivity.setRequestedOrientation(getActivityInfoOrientation() | ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
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

    public void addOverlayVisibilityChangeListener(OverlayVisibilityChangeListener listener) {
        mOverlayVisibilityChangeListenerList.add(listener);
    }

    public boolean removeOverlayVisibilityChangeListener(OverlayVisibilityChangeListener listener) {
        return mOverlayVisibilityChangeListenerList.remove(listener);
    }

    public String getRootScreenId() {
        return mRootScreenId;
    }

    public void setRootScreenId(String mRootScreenId) {
        this.mRootScreenId = mRootScreenId;
    }
}
