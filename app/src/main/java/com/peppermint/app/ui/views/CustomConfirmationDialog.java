package com.peppermint.app.ui.views;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.peppermint.app.R;

/**
 * Created by Nuno Luz on 13-11-2015.
 *
 * Custom dialog that asks for confirmation with two "Yes" and "No" buttons.
 */
public class CustomConfirmationDialog extends Dialog {

    private static final String DO_NOT_SHOW_AGAIN_KEY = "CustomConfirmationDialog_doNotShowAgain";

    private Button mBtnYes, mBtnNo;
    private TextView mTxtTitle, mTxtText;
    private Button mChkDoNotShowAgain;

    private CharSequence mInitialTitle, mInitialText;
    private Typeface mInitialTitleTypeface, mInitialTextTypeface, mInitialButtonTypeface;
    private View.OnClickListener mInitialYesListener, mInitialNoListener;

    public CustomConfirmationDialog(Context context) {
        super(context, R.style.Peppermint_Dialog);
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.d_confirmation);

        mBtnYes = (Button) findViewById(R.id.btnYes);
        mBtnNo = (Button) findViewById(R.id.btnNo);

        mTxtTitle = (TextView) findViewById(R.id.txtTitle);
        mTxtText = (TextView) findViewById(R.id.txtText);
        mChkDoNotShowAgain = (Button) findViewById(R.id.chkDoNotShowAgain);
        mChkDoNotShowAgain.setSelected(false);
        mChkDoNotShowAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChkDoNotShowAgain.setSelected(!mChkDoNotShowAgain.isSelected());
            }
        });

        if(mInitialButtonTypeface != null) {
            setButtonTypeface(mInitialButtonTypeface);
        }
        if(mInitialNoListener != null) {
            setNoClickListener(mInitialNoListener);
        }
        if(mInitialYesListener != null) {
            setYesClickListener(mInitialYesListener);
        }

        if(mInitialTitle != null) {
            setTitle(mInitialTitle);
        }
        if(mInitialTitleTypeface != null) {
            setTitleTypeface(mInitialTitleTypeface);
        }

        if(mInitialText != null) {
            setText(mInitialText);
        }
        if(mInitialTextTypeface != null) {
            setTextTypeface(mInitialTextTypeface);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        if(bundle == null) {
            bundle = new Bundle();
        }
        bundle.putBoolean(DO_NOT_SHOW_AGAIN_KEY, isDoNotShowAgain());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            boolean doNotShow = savedInstanceState.getBoolean(DO_NOT_SHOW_AGAIN_KEY, false);
            setDoNotShowAgain(doNotShow);
        }
    }

    public void setButtonTypeface(Typeface typeface) {
        if(mBtnYes != null && mBtnNo != null) {
            mBtnYes.setTypeface(typeface);
            mBtnNo.setTypeface(typeface);
        } else {
            mInitialButtonTypeface = typeface;
        }
    }

    public CharSequence getTitle() {
        return mTxtTitle.getText();
    }

    public void setTitle(CharSequence text) {
        if(mTxtTitle != null) {
            mTxtTitle.setText(text);
        } else {
            mInitialTitle = text;
        }
    }

    public void setTitleTypeface(Typeface typeface) {
        if(mTxtTitle != null) {
            mTxtTitle.setTypeface(typeface);
        } else {
            mInitialTitleTypeface = typeface;
        }
    }

    public CharSequence getText() {
        return mTxtText.getText();
    }

    public void setText(CharSequence text) {
        if(mTxtText != null) {
            mTxtText.setText(text);
        } else {
            mInitialText = text;
        }
    }

    public void setTextTypeface(Typeface typeface) {
        if(mTxtText != null && mChkDoNotShowAgain != null) {
            mTxtText.setTypeface(typeface);
            mChkDoNotShowAgain.setTypeface(typeface);
        } else {
            mInitialTextTypeface = typeface;
        }
    }

    public void setYesClickListener(View.OnClickListener listener) {
        if(mBtnYes != null) {
            mBtnYes.setOnClickListener(listener);
        } else {
            mInitialYesListener = listener;
        }
    }

    public void setNoClickListener(View.OnClickListener listener) {
        if(mBtnNo != null) {
            mBtnNo.setOnClickListener(listener);
        } else {
            mInitialNoListener = listener;
        }
    }

    public boolean isDoNotShowAgain() {
        return mChkDoNotShowAgain != null && mChkDoNotShowAgain.isSelected();
    }

    public void setDoNotShowAgain(boolean val) {
        if(mChkDoNotShowAgain != null) {
            mChkDoNotShowAgain.setSelected(val);
        }
    }
}
