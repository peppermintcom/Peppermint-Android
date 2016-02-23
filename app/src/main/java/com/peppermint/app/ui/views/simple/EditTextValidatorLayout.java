package com.peppermint.app.ui.views.simple;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;

import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Nuno Luz on 16-02-2016.
 */
public class EditTextValidatorLayout extends LinearLayout implements ViewGroup.OnHierarchyChangeListener, View.OnFocusChangeListener, TextWatcher {

    public interface OnValidityChangeListener {
        void onValidityChange(boolean isValid);
    }

    public interface Validator {
        String getValidatorMessage(Set<Integer> indicesWithError, CharSequence... text);
    }

    public static class ValidityChecker {
        private Set<EditTextValidatorLayout> mSet;

        public ValidityChecker() {
            mSet = new HashSet<>();
        }

        public ValidityChecker(EditTextValidatorLayout... validatorLayouts) {
            mSet = new HashSet<>();
            Collections.addAll(mSet, validatorLayouts);
        }

        public boolean areValid() {
            for(EditTextValidatorLayout validatorLayout : mSet) {
                if(!validatorLayout.isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final String TAG = EditTextValidatorLayout.class.getSimpleName();
    private static final String VALIDATOR_MESSAGE_STATE_KEY = TAG + "_ValidatorMessage";
    private static final String VALIDATOR_VIEW_STATE_KEY = TAG + "_ValidatorView";
    private static final String SUPER_STATE_KEY = TAG + "_Super";

    private EditTextValidatorLayout mLinkedEditTextValidatorLayout;

    private List<EditText> mEditTextList;
    private CustomFontTextView mValidatorTextView;
    private Validator mValidator;
    private OnValidityChangeListener mOnValidityChangeListener;
    private String mValidatorMessage;
    private int mValidBackgroundResource = -1;
    private int mInvalidBackgroundResource = -1;

    public EditTextValidatorLayout(Context context) {
        super(context);
        init(context, null, -1, -1);
    }

    public EditTextValidatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, -1, -1);
    }

    public EditTextValidatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, -1);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EditTextValidatorLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setOrientation(VERTICAL);
        mEditTextList = new ArrayList<>();
        setSaveEnabled(true);

        LayoutTransition layoutTransition = new LayoutTransition();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        }
        setLayoutTransition(layoutTransition);

        mValidatorTextView = new CustomFontTextView(context);
        mValidatorTextView.setVisibility(GONE);
        mValidatorTextView.setTypeface(CustomFontEditText.getTypeface(context, context.getString(R.string.font_regular)));
        mValidatorTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        mValidatorTextView.setTextColor(Utils.getColor(context, R.color.white));
        mValidatorTextView.setBackgroundResource(R.color.orange_text);

        final int dp3 = Utils.dpToPx(context, 3);
        final int dp5 = Utils.dpToPx(context, 5);
        mValidatorTextView.setPadding(dp5, dp3, dp5, dp3);

        addView(mValidatorTextView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        setOnHierarchyChangeListener(this);
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        if(child.equals(mValidatorTextView)) {
            return;
        }

        if(parent.equals(this)) {
            removeView(mValidatorTextView);
            addView(mValidatorTextView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        initChild(child);
    }

    private void initChild(View child) {
        if(child instanceof EditText) {
            child.setOnFocusChangeListener(this);
            ((EditText) child).addTextChangedListener(this);
            mEditTextList.add((EditText) child);
        } else if(child instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) child;
            viewGroup.setOnHierarchyChangeListener(this);
            int childCount = viewGroup.getChildCount();
            for(int i=0; i<childCount; i++) {
                initChild(viewGroup.getChildAt(i));
            }
        }
    }

    private void deinitChild(View child) {
        if(child instanceof EditText) {
            mEditTextList.remove(child);
            ((EditText) child).removeTextChangedListener(this);
            child.setOnFocusChangeListener(null);
        } else if(child instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) child;
            viewGroup.setOnHierarchyChangeListener(null);
            int childCount = viewGroup.getChildCount();
            for(int i=0; i<childCount; i++) {
                deinitChild(viewGroup.getChildAt(i));
            }
        }
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        if(child.equals(mValidatorTextView)) {
            return;
        }

        deinitChild(child);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        validate();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* nothing to do */ }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { /* nothing to do */ }

    @Override
    public void afterTextChanged(Editable s) {
        validate();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState());
        bundle.putString(VALIDATOR_MESSAGE_STATE_KEY, mValidatorMessage);
        bundle.putParcelable(VALIDATOR_VIEW_STATE_KEY, mValidatorTextView.onSaveInstanceState());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state != null) {
            Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable(SUPER_STATE_KEY));
            mValidatorMessage = bundle.getString(VALIDATOR_MESSAGE_STATE_KEY);
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

    public EditTextValidatorLayout getLinkedEditTextValidatorLayout() {
        return mLinkedEditTextValidatorLayout;
    }

    public void setLinkedEditTextValidatorLayout(EditTextValidatorLayout mLinkedEditTextValidatorLayout) {
        if(this.mLinkedEditTextValidatorLayout == mLinkedEditTextValidatorLayout) {
            return;
        }
        this.mLinkedEditTextValidatorLayout = mLinkedEditTextValidatorLayout;
        this.mLinkedEditTextValidatorLayout.setLinkedEditTextValidatorLayout(this);
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

    public boolean isValid() {
        return mValidatorMessage == null;
    }

    public void validate() {
        if(mLinkedEditTextValidatorLayout != null) {
            mLinkedEditTextValidatorLayout.validate(false);
        }
        validate(true);
    }

    private void setEditTextBackground(EditText editText, int res) {
        int bottom = editText.getPaddingBottom();
        int top = editText.getPaddingTop();
        int right = editText.getPaddingRight();
        int left = editText.getPaddingLeft();
        editText.setBackgroundResource(res);
        editText.setPadding(left, top, right, bottom);
    }

    private void validate(boolean doListener) {
        if(mValidator != null && mEditTextList.size() > 0) {
            CharSequence[] textArray = new CharSequence[mEditTextList.size()];
            for(int i=0; i<textArray.length; i++) {
                EditText editText = mEditTextList.get(i);
                textArray[i] = editText.getText();
            }

            Set<Integer> indicesWithError = new HashSet<>();
            mValidatorMessage = mValidator.getValidatorMessage(indicesWithError, textArray);
            if (mValidatorMessage != null) {
                if (mValidatorTextView.getVisibility() != VISIBLE) {
                    if (mInvalidBackgroundResource > 0) {
                        for(int i : indicesWithError) {
                            setEditTextBackground(mEditTextList.get(i), mInvalidBackgroundResource);
                        }
                    }
                    mValidatorTextView.setVisibility(VISIBLE);
                    if (mOnValidityChangeListener != null && doListener) {
                        mOnValidityChangeListener.onValidityChange(false);
                    }
                }

                mValidatorTextView.setText(mValidatorMessage);
            } else {
                if (mValidatorTextView.getVisibility() == VISIBLE) {
                    if (mValidBackgroundResource > 0) {
                        for (int i : indicesWithError) {
                            setEditTextBackground(mEditTextList.get(i), mValidBackgroundResource);
                        }
                    }
                    mValidatorTextView.setVisibility(GONE);
                    if (mOnValidityChangeListener != null && doListener) {
                        mOnValidityChangeListener.onValidityChange(true);
                    }
                }
            }
        }
    }

}
