package com.peppermint.app.utils;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.peppermint.app.R;

/**
 * Created by Nuno Luz on 26-11-2015.
 *
 * Creates and handles a {@link PopupWindow} that shows alerts/messages in the UI.
 */
public class Popup {

    public static final long LENGTH_SHORT = 3000;
    public static final long LENGTH_LONG = 6000;

    private final Runnable mDismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            dismiss();
        }
    };
    private Handler mHandler;

    private Activity mActivity;
    private long mDuration = LENGTH_LONG;

    private TextView mTxtMessage;
    private PopupWindow mPopup;

    public Popup(Activity activity) {
        this(activity, R.layout.v_popup, R.drawable.img_popup);
    }

    public Popup(Activity activity, int layoutResId, int popupDrawableId) {
        this.mActivity = activity;
        this.mHandler = new Handler(activity.getMainLooper());

        // hold popup
        mPopup = new PopupWindow(mActivity);
        mPopup.setContentView(LayoutInflater.from(mActivity).inflate(layoutResId, null));

        mTxtMessage = (TextView) mPopup.getContentView().findViewById(R.id.text1);

        // noinspection deprecation
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        mPopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopup.setBackgroundDrawable(Utils.getDrawable(mActivity, popupDrawableId));
        mPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);

        // do not let the popup get in the way of user interaction
        mPopup.setFocusable(false);
        mPopup.setTouchable(false);
    }

    public void setMessage(int msgResId) {
        if(mTxtMessage != null) {
            mTxtMessage.setText(msgResId);
        }
    }

    public void setMessage(String msg) {
        if(mTxtMessage != null) {
            mTxtMessage.setText(msg);
        }
    }

    public void dismiss() {
        boolean isDestroyed = false;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isDestroyed = mActivity.isDestroyed();
        }

        if (mPopup.isShowing() && !isDestroyed) {
            mPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    public void show(View parent, int xDp, int yDp, boolean autoDismiss) {
        show(parent, null, xDp, yDp, autoDismiss);
    }

    public void show(View parent) {
        show(parent, null, 40, -1, true);
    }

    public void show(View parent, int msgResId) {
        show(parent, mActivity.getString(msgResId), 40, -1, true);
    }

    public void show(View parent, String msg, int xDp, int yDp, boolean autoDismiss) {
        Rect outRect = new Rect();
        parent.getGlobalVisibleRect(outRect);

        dismiss();
        if(msg != null) {
            setMessage(msg);
        }

        int xPx = xDp < 0 ? outRect.centerX() : Utils.dpToPx(mActivity, xDp);
        int yPx = yDp < 0 ? outRect.centerY() : Utils.dpToPx(mActivity, yDp);

        mPopup.showAtLocation(parent, Gravity.NO_GRAVITY, xPx, yPx);

        if(autoDismiss) {
            mHandler.postDelayed(mDismissPopupRunnable, mDuration);
        }
    }

}
