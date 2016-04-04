package com.peppermint.app.ui.base.views;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;

import com.peppermint.app.R;
import com.peppermint.app.ui.AnimatorBuilder;
import com.peppermint.app.utils.Utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Nuno Luz on 10-11-2015.
 * {@link EditText} with animated validation warnings.
 */
public class CustomValidatedEditText extends FrameLayout implements TextWatcher {

    public interface OnValidityChangeListener {
        void onValidityChange(boolean isValid);
    }

    public interface Validator {
        String getValidatorMessage(CharSequence text);
    }

    public static class ValidityChecker {
        private Set<CustomValidatedEditText> mSet;

        public ValidityChecker() {
            mSet = new HashSet<>();
        }

        public ValidityChecker(CustomValidatedEditText... editTexts) {
            mSet = new HashSet<>();
            Collections.addAll(mSet, editTexts);
        }

        public boolean areValid() {
            for(CustomValidatedEditText editText : mSet) {
                if(!editText.isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final String TAG = CustomValidatedEditText.class.getSimpleName();
    private static final String VALIDATOR_MESSAGE_STATE_KEY = TAG + "_ValidatorMessage";
    private static final String EDIT_TEXT_STATE_KEY = TAG + "_EditText";
    private static final String VALIDATOR_VIEW_STATE_KEY = TAG + "_ValidatorView";
    private static final String SUPER_STATE_KEY = TAG + "_Super";

    private CustomValidatedEditText mLinkedEditText;

    private CustomFontEditText mEditText;
    private CustomFontTextView mValidatorTextView;
    private Validator mValidator;
    private OnValidityChangeListener mOnValidityChangeListener;
    private String mValidatorMessage;
    private int mValidBackgroundResource = -1;
    private int mInvalidBackgroundResource = -1;

    private AnimatorBuilder mAnimatorBuilder;
    private Animator mValidatorTextViewShowAnimator;
    private Animator.AnimatorListener mShowAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            mValidatorTextView.setVisibility(VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animation) {

        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mValidatorTextView.setVisibility(GONE);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    public CustomValidatedEditText(Context context) {
        super(context);
        init(context, null, -1, -1);
    }

    public CustomValidatedEditText(Context context, AttributeSet attrs) {
        super(context);
        init(context, attrs, -1, -1);
    }

    public CustomValidatedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context);
        init(context, attrs, defStyleAttr, -1);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomValidatedEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mAnimatorBuilder = new AnimatorBuilder();
        setSaveEnabled(true);

        if(defStyleRes >= 0) {
            mEditText = new CustomFontEditText(context, attrs, defStyleAttr, defStyleRes);
        } else if(defStyleAttr >= 0) {
            mEditText = new CustomFontEditText(context, attrs, defStyleAttr);
        } else if(attrs != null) {
            mEditText = new CustomFontEditText(context, attrs);
        } else {
            mEditText = new CustomFontEditText(context);
        }

        setId(mEditText.getId());
        mEditText.setId(NO_ID);

        mEditText.addTextChangedListener(this);
        mValidatorTextView = new CustomFontTextView(context);

        mValidatorTextView.setVisibility(GONE);
        mValidatorTextView.setTypeface(CustomFontEditText.getTypeface(context, context.getString(R.string.font_regular)));
        mValidatorTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        mValidatorTextView.setTextColor(Utils.getColor(context, R.color.white));
        mValidatorTextView.setBackgroundResource(R.color.orange_text);
        mValidatorTextView.setGravity(Gravity.BOTTOM);

        final int dp3 = Utils.dpToPx(context, 3);
        final int dp5 = Utils.dpToPx(context, 5);
        mValidatorTextView.setPadding(dp5, dp3, dp5, dp3);

        /*final int dp14 = Utils.dpToPx(getContext(), 14);*/
        FrameLayout.LayoutParams editTextParams = new FrameLayout.LayoutParams(context, attrs);
        /*editTextParams.bottomMargin += dp14;
        editTextParams.topMargin += dp14;*/

        addView(mEditText, editTextParams);
        addView(mValidatorTextView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState());
        bundle.putString(VALIDATOR_MESSAGE_STATE_KEY, mValidatorMessage);
        bundle.putParcelable(VALIDATOR_VIEW_STATE_KEY, mValidatorTextView.onSaveInstanceState());
        bundle.putParcelable(EDIT_TEXT_STATE_KEY, mEditText.onSaveInstanceState());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state != null) {
            Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable(SUPER_STATE_KEY));
            mValidatorMessage = bundle.getString(VALIDATOR_MESSAGE_STATE_KEY);
            mEditText.onRestoreInstanceState(bundle.getParcelable(EDIT_TEXT_STATE_KEY));
            mValidatorTextView.onRestoreInstanceState(bundle.getParcelable(VALIDATOR_VIEW_STATE_KEY));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;

        if(params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).setMargins(0, 0, 0, 0);
        } else if(params instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) params).setMargins(0, 0, 0, 0);
        } else if(params instanceof RelativeLayout.LayoutParams) {
            ((RelativeLayout.LayoutParams) params).setMargins(0, 0, 0, 0);
        } else if(params instanceof GridLayout.LayoutParams) {
            ((GridLayout.LayoutParams) params).setMargins(0, 0, 0, 0);
        } else if(params instanceof TableLayout.LayoutParams) {
            ((TableLayout.LayoutParams) params).setMargins(0, 0, 0, 0);
        }
    }

    public CustomValidatedEditText getLinkedEditText() {
        return mLinkedEditText;
    }

    public void setLinkedEditText(CustomValidatedEditText mLinkedEditText) {
        if(this.mLinkedEditText == mLinkedEditText) {
            return;
        }
        this.mLinkedEditText = mLinkedEditText;
        this.mLinkedEditText.setLinkedEditText(this);
    }

    public int getValidBackgroundResource() {
        return mValidBackgroundResource;
    }

    public void setValidBackgroundResource(int mValidBackgroundResource) {
        this.mValidBackgroundResource = mValidBackgroundResource;
    }

    public int getInvalidBackgroundResource() {
        return mInvalidBackgroundResource;
    }

    public void setInvalidBackgroundResource(int mInvalidBackgroundResource) {
        this.mInvalidBackgroundResource = mInvalidBackgroundResource;
    }

    public Validator getValidator() {
        return mValidator;
    }

    public void setValidator(Validator mValidator) {
        this.mValidator = mValidator;
    }

    public CustomFontEditText getEditText() {
        return mEditText;
    }

    public CustomFontTextView getValidatorTextView() {
        return mValidatorTextView;
    }

    public void setValidatorTextView(CustomFontTextView textView) {
        mValidatorTextView = textView;
    }

    public OnValidityChangeListener getOnValidityChangeListener() {
        return mOnValidityChangeListener;
    }

    public void setOnValidityChangeListener(OnValidityChangeListener mOnValidityChangeListener) {
        this.mOnValidityChangeListener = mOnValidityChangeListener;
    }

    public Editable getText() {
        return mEditText.getText();
    }

    public void setText(CharSequence text) {
        mEditText.setText(text);
    }

    public void setSelection(int sel) {
        mEditText.setSelection(sel);
    }

    public void addTextChangedListener(TextWatcher watcher) {
        mEditText.addTextChangedListener(watcher);
    }

    public void removeTextChangedListener(TextWatcher watcher) {
        mEditText.removeTextChangedListener(watcher);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        /* nothing to do here */
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        /* nothing to do here */
    }

    public boolean isValid() {
        return mValidatorMessage == null;
    }

    public void validate() {
        afterTextChanged(mEditText.getText());
    }

    private void setEditTextBackground(int res) {
        int bottom = mEditText.getPaddingBottom();
        int top = mEditText.getPaddingTop();
        int right = mEditText.getPaddingRight();
        int left = mEditText.getPaddingLeft();
        mEditText.setBackgroundResource(res);
        mEditText.setPadding(left, top, right, bottom);
    }

    private void validate(Editable s, boolean doListener) {
        if(mValidator != null) {
            mValidatorMessage = mValidator.getValidatorMessage(s);
            if(mValidatorMessage != null) {
                if (mValidatorTextView.getVisibility() != VISIBLE) {
                    if(mInvalidBackgroundResource > 0) {
                        setEditTextBackground(mInvalidBackgroundResource);
                    }
                    if (mValidatorTextViewShowAnimator == null || !mValidatorTextViewShowAnimator.isStarted()) {
                        mValidatorTextViewShowAnimator = mAnimatorBuilder.buildFadeInAnimator(mValidatorTextView);
                        mValidatorTextViewShowAnimator.addListener(mShowAnimatorListener);
                        mValidatorTextViewShowAnimator.start();
                    }
                    if(mOnValidityChangeListener != null && doListener) {
                        mOnValidityChangeListener.onValidityChange(false);
                    }
                }

                mValidatorTextView.setText(mValidatorMessage);
            } else {
                if(mValidatorTextView.getVisibility() == VISIBLE) {
                    if(mValidBackgroundResource > 0) {
                        setEditTextBackground(mValidBackgroundResource);
                    }
                    mValidatorTextView.setVisibility(GONE);
                    if(mOnValidityChangeListener != null && doListener) {
                        mOnValidityChangeListener.onValidityChange(true);
                    }
                }

                if(mValidatorTextViewShowAnimator != null && mValidatorTextViewShowAnimator.isStarted()) {
                    mValidatorTextViewShowAnimator.cancel();
                    mValidatorTextViewShowAnimator = null;
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if(mLinkedEditText != null) {
            mLinkedEditText.validate(mLinkedEditText.getText(), false);
        }
        validate(s, true);
    }
}
