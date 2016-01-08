package com.peppermint.app.ui.views.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.peppermint.app.R;
import com.peppermint.app.ui.views.simple.CustomFontButton;
import com.peppermint.app.ui.views.simple.CustomFontTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 13-11-2015.
 *
 * Custom dialog that contains an inflated layout.<br />
 * Has a title bar and supports three buttons (negative, neutral and positive).<br />
 */
public class CustomDialog extends Dialog implements View.OnClickListener {

    private int mLayoutRes;
    private View mLayoutView;
    private FrameLayout mContainer;

    private CharSequence mTitleText;
    private CharSequence mPositiveButtonText, mNegativeButtonText, mNeutralButtonText;
    private View.OnClickListener mPositiveButtonListener, mNegativeButtonListener, mNeutralButtonListener;

    private CustomFontButton mButton1, mButton2, mButton3;
    private CustomFontTextView mTxtTitle;

    public CustomDialog(Context context) {
        super(context, R.style.Peppermint_Dialog);
        setCancelable(false);
        init();
    }

    public CustomDialog(Context context, int themeResId) {
        super(context, themeResId);
        setCancelable(false);
        init();
    }

    public CustomDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init();
    }

    private void init() {
        mPositiveButtonText = getContext().getString(R.string.yes);
        mNegativeButtonText = getContext().getString(R.string.no);
    }

    private void refreshButtonBackgrounds() {
        List<Button> visibleButtons = new ArrayList<>();

        if(mButton1 == null || mButton2 == null || mButton3 == null) {
            return;
        }

        if(mButton1.getVisibility() == View.VISIBLE) {
            visibleButtons.add(mButton1);
        }
        if(mButton2.getVisibility() == View.VISIBLE) {
            visibleButtons.add(mButton2);
        }
        if(mButton3.getVisibility() == View.VISIBLE) {
            visibleButtons.add(mButton3);
        }

        if(visibleButtons.size() == 1) {
            visibleButtons.get(0).setBackgroundResource(R.drawable.background_dialog_btn);
        } else if(visibleButtons.size() == 2) {
            visibleButtons.get(0).setBackgroundResource(R.drawable.background_dialog_btn_left);
            visibleButtons.get(1).setBackgroundResource(R.drawable.background_dialog_btn_right);
        } else if(visibleButtons.size() == 3) {
            visibleButtons.get(0).setBackgroundResource(R.drawable.background_dialog_btn_left);
            visibleButtons.get(1).setBackgroundResource(R.drawable.background_dialog_btn_center);
            visibleButtons.get(2).setBackgroundResource(R.drawable.background_dialog_btn_right);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.d_custom);

        mButton1 = (CustomFontButton) findViewById(R.id.button1);
        mButton2 = (CustomFontButton) findViewById(R.id.button2);
        mButton3 = (CustomFontButton) findViewById(R.id.button3);

        mTxtTitle = (CustomFontTextView) findViewById(R.id.title);
        mContainer = (FrameLayout) findViewById(R.id.container);

        if(mLayoutView == null && mLayoutRes > 0) {
            View v = LayoutInflater.from(getContext()).inflate(mLayoutRes, null);
            mContainer.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            mContainer.addView(mLayoutView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        mButton1.setOnClickListener(this);
        mButton2.setOnClickListener(this);
        mButton3.setOnClickListener(this);

        if(mPositiveButtonText != null) {
            mButton3.setText(mPositiveButtonText);
        } else {
            mButton3.setVisibility(View.GONE);
        }

        if(mNeutralButtonText != null) {
            mButton2.setText(mNeutralButtonText);
        } else {
            mButton2.setVisibility(View.GONE);
        }

        if(mNegativeButtonText != null) {
            mButton1.setText(mNegativeButtonText);
        } else {
            mButton1.setVisibility(View.GONE);
        }

        refreshButtonBackgrounds();

        if(mTitleText != null) {
            mTxtTitle.setText(mTitleText);
        }
    }

    public CharSequence getTitleText() {
        return mTitleText;
    }

    public void setTitleText(int mTitleTextRes) {
        setTitleText(getContext().getString(mTitleTextRes));
    }

    public void setTitleText(CharSequence mTitleText) {
        this.mTitleText = mTitleText;
        if(mTxtTitle != null) {
            mTxtTitle.setText(mTitleText);
        }
    }

    public CharSequence getPositiveButtonText() {
        return mPositiveButtonText;
    }

    public void setPositiveButtonText(int mPositiveButtonTextRes) {
        setPositiveButtonText(getContext().getString(mPositiveButtonTextRes));
    }

    public void setPositiveButtonText(CharSequence mPositiveButtonText) {
        this.mPositiveButtonText = mPositiveButtonText;
        if(mButton3 != null) {
            if(mPositiveButtonText != null) {
                mButton3.setText(mPositiveButtonText);
                mButton3.setVisibility(View.VISIBLE);
            } else {
                mButton3.setVisibility(View.GONE);
            }
            refreshButtonBackgrounds();
        }
    }

    public CharSequence getNegativeButtonText() {
        return mNegativeButtonText;
    }

    public void setNegativeButtonText(int mNegativeButtonTextRes) {
        setNegativeButtonText(getContext().getString(mNegativeButtonTextRes));
    }

    public void setNegativeButtonText(CharSequence mNegativeButtonText) {
        this.mNegativeButtonText = mNegativeButtonText;
        if(mButton1 != null) {
            if(mNegativeButtonText != null) {
                mButton1.setText(mNegativeButtonText);
                mButton1.setVisibility(View.VISIBLE);
            } else {
                mButton1.setVisibility(View.GONE);
            }
            refreshButtonBackgrounds();
        }
    }

    public CharSequence getNeutralButtonText() {
        return mNeutralButtonText;
    }

    public void setNeutralButtonText(int mNeutralButtonTextRes) {
        setNeutralButtonText(getContext().getString(mNeutralButtonTextRes));
    }

    public void setNeutralButtonText(CharSequence mNeutralButtonText) {
        this.mNeutralButtonText = mNeutralButtonText;
        if(mButton2 != null) {
            if(mNeutralButtonText != null) {
                mButton2.setText(mNeutralButtonText);
                mButton2.setVisibility(View.VISIBLE);
            } else {
                mButton2.setVisibility(View.GONE);
            }
            refreshButtonBackgrounds();
        }
    }

    public View.OnClickListener getPositiveButtonListener() {
        return mPositiveButtonListener;
    }

    public void setPositiveButtonListener(View.OnClickListener mPositiveButtonListener) {
        this.mPositiveButtonListener = mPositiveButtonListener;
    }

    public View.OnClickListener getNegativeButtonListener() {
        return mNegativeButtonListener;
    }

    public void setNegativeButtonListener(View.OnClickListener mNegativeButtonListener) {
        this.mNegativeButtonListener = mNegativeButtonListener;
    }

    public View.OnClickListener getNeutralButtonListener() {
        return mNeutralButtonListener;
    }

    public void setNeutralButtonListener(View.OnClickListener mNeutralButtonListener) {
        this.mNeutralButtonListener = mNeutralButtonListener;
    }

    public CustomFontButton getNegativeButton() {
        return mButton1;
    }

    public CustomFontButton getNeutralButton() {
        return mButton2;
    }

    public CustomFontButton getPositiveButton() {
        return mButton3;
    }

    public CustomFontTextView getTitleView() {
        return mTxtTitle;
    }

    public int getLayout() {
        return mLayoutRes;
    }

    public void setLayout(int mLayoutRes) {
        this.mLayoutRes = mLayoutRes;
    }

    public View getLayoutView() {
        return mLayoutView;
    }

    public void setLayoutView(View mLayoutView) {
        this.mLayoutView = mLayoutView;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button1) {
            if(mNegativeButtonListener != null) {
                mNegativeButtonListener.onClick(v);
            }
            //cancel();
        } else if(v.getId() == R.id.button2) {
            if(mNeutralButtonListener != null) {
                mNeutralButtonListener.onClick(v);
            }
        } else if(v.getId() == R.id.button3) {
            if(mPositiveButtonListener != null) {
                mPositiveButtonListener.onClick(v);
            }
            //dismiss();
        }
    }
}
