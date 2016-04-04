package com.peppermint.app.ui.chat.recorder;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.peppermint.app.R;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.ui.base.dialogs.CustomConfirmationDialog;
import com.peppermint.app.ui.base.views.CustomFontTextView;

/**
 * Created by Nuno Luz on 13-11-2015.
 *
 * Custom dialog that asks for confirmation with two "Yes" and "No" buttons.
 * Includes a {@link CustomFontTextView}
 */
public class SMSConfirmationDialog extends CustomConfirmationDialog implements View.OnClickListener {

    private static final String EMAIL_RECIPIENT_KEY = SMSConfirmationDialog.class.getCanonicalName() + "_EmailRecipient";

    private ContactRaw mEmailRecipient;

    private CustomFontTextView mTxtEmailVia, mTxtEmailValue;

    private View.OnClickListener mEmailClickListener;

    public SMSConfirmationDialog(Context context) {
        super(context);
        setLayout(R.layout.d_sms_confirmation);
    }

    public SMSConfirmationDialog(Context context, int themeResId) {
        super(context, themeResId);
        setLayout(R.layout.d_sms_confirmation);
    }

    public SMSConfirmationDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        setLayout(R.layout.d_sms_confirmation);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout lytEmail = (LinearLayout) findViewById(R.id.lytEmail);
        lytEmail.setOnClickListener(this);

        mTxtEmailVia = (CustomFontTextView) findViewById(R.id.txtEmailVia);
        mTxtEmailValue = (CustomFontTextView) findViewById(R.id.txtEmailValue);

        setEmailRecipient(mEmailRecipient);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if(v.getId() == R.id.lytEmail && mEmailClickListener != null) {
            mEmailClickListener.onClick(v);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        if(bundle == null) {
            bundle = new Bundle();
        }
        bundle.putSerializable(EMAIL_RECIPIENT_KEY, getEmailRecipient());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            ContactRaw emailRecipient = (ContactRaw) savedInstanceState.getSerializable(EMAIL_RECIPIENT_KEY);
            setEmailRecipient(emailRecipient);
        }
    }

    public ContactRaw getEmailRecipient() {
        return mEmailRecipient;
    }

    public void setEmailRecipient(ContactRaw mEmailRecipient) {
        if(mTxtEmailValue != null) {
            if(mEmailRecipient == null) {
                mTxtEmailValue.setText("");
                mTxtEmailVia.setText(R.string.must_add_an_email_address_first);
                mTxtEmailValue.setVisibility(View.GONE);
            } else {
                mTxtEmailValue.setText(mEmailRecipient.getEmail().getVia());
                mTxtEmailVia.setText(R.string.via);
                mTxtEmailValue.setVisibility(View.VISIBLE);
            }
        }
        this.mEmailRecipient = mEmailRecipient;
    }

    public View.OnClickListener getEmailClickListener() {
        return mEmailClickListener;
    }

    public void setEmailClickListener(View.OnClickListener mEmailClickListener) {
        this.mEmailClickListener = mEmailClickListener;
    }
}
