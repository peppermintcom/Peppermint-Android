package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.peppermint.app.R;
import com.peppermint.app.dal.contact.ContactRaw;
import com.peppermint.app.ui.base.dialogs.CustomConfirmationDialog;
import com.peppermint.app.ui.base.views.CustomFontButton;
import com.peppermint.app.ui.base.views.CustomFontEditText;
import com.peppermint.app.ui.base.views.EditTextValidatorLayout;
import com.peppermint.app.utils.Utils;

import java.util.Set;

/**
 * Created by Nuno Luz on 03-05-2016.
 *
 * Custom dialog that asks for an email address to associate with an existing contact.
 */
public class AddEmailDialog extends CustomConfirmationDialog implements DialogInterface.OnShowListener {

    private static final String CONTACT_KEY = AddEmailDialog.class.getCanonicalName() + "_Contact";

    private ContactRaw mContact;

    private CustomFontEditText mTxtEmail;
    private CustomFontButton mBtnAdd;

    private EditTextValidatorLayout mEmailValidatorLayout;
    private EditTextValidatorLayout.Validator mEmailValidator = new EditTextValidatorLayout.Validator() {
        @Override
        public String getValidatorMessage(Set<Integer> indicesWithError, CharSequence[] text) {
            final String email = mTxtEmail.getText().toString().trim();

            if(!Utils.isValidEmail(email)) {
                indicesWithError.add(0);
                return getContext().getString(R.string.msg_insert_mail);
            }

            return null;
        }
    };
    private EditTextValidatorLayout.OnValidityChangeListener mValidityChangeListener = new EditTextValidatorLayout.OnValidityChangeListener() {
        @Override
        public void onValidityChange(boolean isValid) {
            if(isValid) {
                mBtnAdd.setEnabled(true);
            } else {
                mBtnAdd.setEnabled(false);
            }
        }
    };

    public AddEmailDialog(Context context) {
        super(context);
        setLayout(R.layout.d_add_email);
    }

    public AddEmailDialog(Context context, int themeResId) {
        super(context, themeResId);
        setLayout(R.layout.d_add_email);
    }

    public AddEmailDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        setLayout(R.layout.d_add_email);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setOnShowListener(this);

        setTitleText(R.string.sending_via_email);
        setPositiveButtonText(R.string.add_email);
        setNegativeButtonText(null);

        mEmailValidatorLayout = (EditTextValidatorLayout) findViewById(R.id.lytEmailValidator);
        mEmailValidatorLayout.setValidator(mEmailValidator);
        mEmailValidatorLayout.setOnValidityChangeListener(mValidityChangeListener);

        mBtnAdd = getPositiveButton();

        mTxtEmail = (CustomFontEditText) findViewById(R.id.txtEmail);

        setCancelable(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mEmailValidatorLayout.validate();
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        if(bundle == null) {
            bundle = new Bundle();
        }
        bundle.putSerializable(CONTACT_KEY, getContact());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            ContactRaw emailRecipient = (ContactRaw) savedInstanceState.getSerializable(CONTACT_KEY);
            setContact(emailRecipient);
        }
    }

    public ContactRaw getContact() {
        return mContact;
    }

    public void setContact(ContactRaw mContact) {
        this.mContact = mContact;
    }

    public String getEmail() {
        return mTxtEmail.getText().toString();
    }

    @Override
    public void onShow(DialogInterface dialog) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mTxtEmail, InputMethodManager.SHOW_FORCED);
    }
}
