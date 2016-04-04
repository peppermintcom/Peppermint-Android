package com.peppermint.app.ui.base.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 26-11-2015.
 *
 * Creates and handles a {@link PopupWindow} that shows alerts/messages in the UI.
 */
public class PopupDialog extends Dialog implements View.OnTouchListener {

    private int mLayoutRes;

    public PopupDialog(Context context) {
        super(context, R.style.Peppermint_Dialog);
        init();
    }

    public PopupDialog(Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    public PopupDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init();
    }

    private void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mLayoutRes);

        ViewGroup root = (ViewGroup) findViewById(R.id.lytRoot);
        root.setClickable(true);
        root.setFocusable(true);
        root.setOnTouchListener(this);
    }

    public int getLayoutResource() {
        return mLayoutRes;
    }

    public void setLayoutResource(int mLayoutRes) {
        this.mLayoutRes = mLayoutRes;
    }

    public void show(View parent) {
        Rect outRect = new Rect();
        parent.getGlobalVisibleRect(outRect);

        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        layoutParams.y = outRect.bottom - Utils.dpToPx(getContext(), 3);

        int statusBarHeight = Utils.getStatusBarHeight(getContext());
        layoutParams.y -= statusBarHeight;

        getWindow().setAttributes(layoutParams);

        super.show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            dismiss();
        }
        return false;
    }

}
