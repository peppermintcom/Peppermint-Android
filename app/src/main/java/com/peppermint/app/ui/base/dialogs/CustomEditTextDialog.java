package com.peppermint.app.ui.base.dialogs;

import android.content.Context;
import android.os.Bundle;

import com.peppermint.app.R;
import com.peppermint.app.ui.base.views.CustomFontEditText;

/**
 * Created by Nuno Luz on 13-11-2015.
 *
 * Custom dialog that asks for text with two "Yes" and "No" buttons.<br />
 * Includes a {@link CustomFontEditText}
 */
public class CustomEditTextDialog extends CustomDialog {

    private static final String EDIT_TEXT_KEY = CustomEditTextDialog.class.getCanonicalName() + "_EditText";
    private static final String EDIT_HINT_KEY = CustomEditTextDialog.class.getCanonicalName() + "_EditHint";

    private CharSequence mText, mHintText;

    private CustomFontEditText mEditText;

    public CustomEditTextDialog(Context context) {
        super(context);
        setLayout(R.layout.d_custom_edittext);
    }

    public CustomEditTextDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        setLayout(R.layout.d_custom_edittext);
    }

    public CustomEditTextDialog(Context context, int themeResId) {
        super(context, themeResId);
        setLayout(R.layout.d_custom_edittext);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEditText = (CustomFontEditText) findViewById(R.id.text);

        if(mText != null) {
            mEditText.setText(mText);
        }

        if(mHintText != null) {
            mEditText.setHint(mHintText);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        if(bundle == null) {
            bundle = new Bundle();
        }
        bundle.putCharSequence(EDIT_TEXT_KEY, getText());
        bundle.putCharSequence(EDIT_HINT_KEY, getHintText());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            setText(savedInstanceState.getCharSequence(EDIT_TEXT_KEY, ""));
            setHintText(savedInstanceState.getCharSequence(EDIT_HINT_KEY, ""));
        }
    }

    public CharSequence getText() {
        return mEditText == null ? mText : mEditText.getText();
    }

    public void setText(int mTextRes) {
        setText(getContext().getString(mTextRes));
    }

    public void setText(CharSequence mText) {
        this.mText = mText;
        if(mEditText != null) {
            mEditText.setText(mText);
        }
    }

    public CharSequence getHintText() {
        return mEditText == null ? mHintText : mEditText.getHint();
    }

    public void setHintText(int mHintTextRes) {
        setHintText(getContext().getString(mHintTextRes));
    }

    public void setHintText(CharSequence mHintText) {
        this.mHintText = mHintText;
        if(mEditText != null) {
            mEditText.setHint(mText);
        }
    }

    public CustomFontEditText getEditTextView() {
        return mEditText;
    }
}
