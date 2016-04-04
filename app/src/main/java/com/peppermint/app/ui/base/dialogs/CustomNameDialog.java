package com.peppermint.app.ui.base.dialogs;

import android.content.Context;
import android.os.Bundle;

import com.peppermint.app.R;
import com.peppermint.app.ui.base.views.CustomFontEditText;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 13-11-2015.
 *
 * Custom dialog that asks for text with two "Yes" and "No" buttons.<br />
 * Includes a {@link CustomFontEditText}
 */
public class CustomNameDialog extends CustomDialog {

    private static final String EDIT_TEXT1_KEY = CustomNameDialog.class.getCanonicalName() + "_EditText1";
    private static final String EDIT_TEXT2_KEY = CustomNameDialog.class.getCanonicalName() + "_EditText2";

    private CharSequence mText1, mText2;
    private CustomFontEditText mEditText1, mEditText2;

    public CustomNameDialog(Context context) {
        super(context);
        setLayout(R.layout.d_custom_name);
    }

    public CustomNameDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        setLayout(R.layout.d_custom_name);
    }

    public CustomNameDialog(Context context, int themeResId) {
        super(context, themeResId);
        setLayout(R.layout.d_custom_name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEditText1 = (CustomFontEditText) findViewById(R.id.text1);
        mEditText2 = (CustomFontEditText) findViewById(R.id.text2);

        if(mText1 != null) {
            mEditText1.setText(mText1);
        }

        if(mText2 != null) {
            mEditText2.setHint(mText2);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        if(bundle == null) {
            bundle = new Bundle();
        }
        bundle.putCharSequence(EDIT_TEXT1_KEY, getFirstName());
        bundle.putCharSequence(EDIT_TEXT2_KEY, getLastName());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            setFirstName(savedInstanceState.getCharSequence(EDIT_TEXT1_KEY, ""));
            setLastName(savedInstanceState.getCharSequence(EDIT_TEXT2_KEY, ""));
        }
    }

    public void setFullName(String fullName) {
        fullName = Utils.capitalizeFully(fullName);
        String[] names = fullName.split("\\s+");

        if(names.length > 1) {
            String lastName = names[names.length - 1];
            setLastName(lastName);

            String firstName = fullName.substring(0, fullName.length() - names[names.length - 1].length()).trim();
            setFirstName(firstName);
        } else {
            setFirstName(fullName);
        }
    }

    public String getFullName() {
        return Utils.capitalizeFully(getFirstName().toString().trim() + " " + getLastName().toString().trim());
    }

    public CharSequence getFirstName() {
        return mEditText1 == null ? mText1 : mEditText1.getText();
    }

    public CharSequence getLastName() {
        return mEditText2 == null ? mText2 : mEditText2.getText();
    }

    public void setFirstName(int mTextRes) {
        setFirstName(getContext().getString(mTextRes));
    }

    public void setFirstName(CharSequence mText) {
        this.mText1 = mText;
        if(mEditText1 != null) {
            mEditText1.setText(mText);
            mEditText1.setSelection(mText.length());
        }
    }

    public void setLastName(int mTextRes) {
        setLastName(getContext().getString(mTextRes));
    }

    public void setLastName(CharSequence mText) {
        this.mText2 = mText;
        if(mEditText2 != null) {
            mEditText2.setText(mText);
            mEditText2.setSelection(mText.length());
        }
    }

    public CustomFontEditText getFirstNameView() {
        return mEditText1;
    }

    public CustomFontEditText getLastNameView() {
        return mEditText2;
    }

}
