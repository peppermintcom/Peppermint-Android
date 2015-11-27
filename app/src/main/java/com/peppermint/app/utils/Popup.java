package com.peppermint.app.utils;

import android.app.Activity;
import android.graphics.Rect;
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
        this.mActivity = activity;
        this.mHandler = new Handler(activity.getMainLooper());

        // hold popup
        mPopup = new PopupWindow(mActivity);
        mPopup.setContentView(LayoutInflater.from(mActivity).inflate(R.layout.v_popup, null));

        mTxtMessage = (TextView) mPopup.getContentView().findViewById(R.id.text1);

        // noinspection deprecation
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        mPopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopup.setBackgroundDrawable(Utils.getDrawable(mActivity, R.drawable.img_popup));
        mPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);

        // do not let the popup get in the way of user interaction
        mPopup.setFocusable(false);
        mPopup.setTouchable(false);
    }

    public void setMessage(int msgResId) {
        mTxtMessage.setText(msgResId);
    }

    public void setMessage(String msg) {
        mTxtMessage.setText(msg);
    }

    public void dismiss() {
        if (mPopup.isShowing() && !mActivity.isDestroyed()) {
            mPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    public void show(View parent) {
        show(parent, null);
    }

    public void show(View parent, int msgResId) {
        show(parent, mActivity.getString(msgResId));
    }

    public void show(View parent, String msg) {
        Rect outRect = new Rect();
        parent.getGlobalVisibleRect(outRect);

        dismiss();
        if(msg != null) {
            setMessage(msg);
        }
        mPopup.showAtLocation(parent, Gravity.NO_GRAVITY, Utils.dpToPx(mActivity, 40), outRect.centerY());
        mHandler.postDelayed(mDismissPopupRunnable, mDuration);
    }

}
