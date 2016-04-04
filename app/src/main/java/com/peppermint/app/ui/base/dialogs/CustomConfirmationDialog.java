package com.peppermint.app.ui.base.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.ui.base.views.CustomFontButton;
import com.peppermint.app.ui.base.views.CustomFontTextView;

/**
 * Created by Nuno Luz on 13-11-2015.
 *
 * Custom dialog that asks for confirmation with two "Yes" and "No" buttons.
 * Includes a {@link CustomFontTextView}
 */
public class CustomConfirmationDialog extends CustomDialog {

    private static final String CHECK_KEY = CustomConfirmationDialog.class.getCanonicalName() + "_Check";

    private boolean mChecked;
    private CharSequence mMessageText, mCheckText;

    private CustomFontTextView mTxtMessage;
    private CustomFontButton mBtnCheck;

    private final View.OnClickListener mCheckClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mBtnCheck.setSelected(!mBtnCheck.isSelected());
        }
    };

    public CustomConfirmationDialog(Context context) {
        super(context);
        setLayout(R.layout.d_custom_confirmation);
    }

    public CustomConfirmationDialog(Context context, int themeResId) {
        super(context, themeResId);
        setLayout(R.layout.d_custom_confirmation);
    }

    public CustomConfirmationDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        setLayout(R.layout.d_custom_confirmation);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBtnCheck = (CustomFontButton) findViewById(R.id.check);
        mTxtMessage = (CustomFontTextView) findViewById(R.id.text);

        if(mMessageText != null) {
            mTxtMessage.setText(mMessageText);
        }

        mBtnCheck.setSelected(mChecked);
        mBtnCheck.setOnClickListener(mCheckClickListener);

        if(mCheckText != null) {
            mBtnCheck.setText(mCheckText);
        } else {
            mBtnCheck.setVisibility(View.GONE);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        if(bundle == null) {
            bundle = new Bundle();
        }
        bundle.putBoolean(CHECK_KEY, isChecked());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            boolean check = savedInstanceState.getBoolean(CHECK_KEY, false);
            setChecked(check);
        }
    }

    public CharSequence getMessageText() {
        return mMessageText;
    }

    public void setMessageText(int mMessageTextRes) {
        setMessageText(getContext().getString(mMessageTextRes));
    }

    public void setMessageText(CharSequence mMessageText) {
        this.mMessageText = mMessageText;
        if(mTxtMessage != null) {
            mTxtMessage.setText(mMessageText);
        }
    }

    public CharSequence getCheckText() {
        return mCheckText;
    }

    public void setCheckText(int mCheckTextRes) {
        setCheckText(getContext().getString(mCheckTextRes));
    }

    public void setCheckText(CharSequence mCheckText) {
        this.mCheckText = mCheckText;
        if(mBtnCheck != null) {
            if(mCheckText != null) {
                mBtnCheck.setText(mCheckText);
                mBtnCheck.setVisibility(View.VISIBLE);
            } else {
                mBtnCheck.setVisibility(View.GONE);
            }
        }
    }

    public boolean isChecked() {
        return mBtnCheck != null && mBtnCheck.isSelected();
    }

    public void setChecked(boolean val) {
        this.mChecked = val;
        if(mBtnCheck != null) {
            mBtnCheck.setSelected(val);
        }
    }

    public CustomFontTextView getMessageView() {
        return mTxtMessage;
    }

    public CustomFontButton getCheckButton() {
        return mBtnCheck;
    }
}
