package com.peppermint.app.ui.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.views.CustomFontEditText;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.Utils;

import java.lang.reflect.Field;

/**
 * Created by Nuno Luz on 22-10-2015.
 *
 * Custom {@link android.preference.EditTextPreference} for Peppermint styled dialogs.<br />
 * Allows dialogs with a {@link CustomFontEditText} to be triggered by a preference.
 *
 * <p>
 *     Taken and customized from {@link android.preference.EditTextPreference}
 * </p>
 */
public class CustomEditTextPreference extends CustomDialogPreference {

    private static final String TAG = CustomEditTextPreference.class.getSimpleName();

    private String mContent;
    private TextView mTxtContent;

    private CustomFontEditText mEditText;
    private String mText;

    public CustomEditTextPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.v_preference);
        init(context, null);
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.v_preference);
        init(context, attrs);
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.v_preference);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.v_preference);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        mEditText = new CustomFontEditText(context, attrs);

        // Give it an ID so it can be saved/restored
        mEditText.setId(R.id.text);

        // FIXME this currently overrides all attributes specified in the preferences XML
        int dp20 = Utils.dpToPx(context, 20);

        mEditText.setPadding(dp20, 0, dp20, dp20);
        mEditText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        mEditText.setSingleLine(true);
        mEditText.setTextColor(ResourceUtils.getColor(context, R.color.black));
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        mEditText.setTypeface(context.getString(R.string.font_regular));

        // set the cursor drawable
        // there are no public methods to set this, so use reflection
        try {
            // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(mEditText, R.drawable.ic_cursor);
        } catch (Exception e) {
            Log.e(TAG, "Unable to set cursor drawable", e);
            TrackerManager.getInstance(getContext().getApplicationContext()).logException(e);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mTxtContent = (TextView) view.findViewById(R.id.content);

        TextView txtTitle = (TextView) view.findViewById(android.R.id.title);
        TextView txtSummary = (TextView) view.findViewById(android.R.id.summary);

        if(mContent == null) {
            mTxtContent.setVisibility(View.GONE);
        } else {
            mTxtContent.setText(mContent);
        }

        txtTitle.setTextColor(ResourceUtils.getColor(getContext(), R.color.black));
        txtSummary.setTextColor(ResourceUtils.getColor(getContext(), R.color.dark_grey_text));
        mTxtContent.setTextColor(ResourceUtils.getColor(getContext(), R.color.black));

        try {
            txtTitle.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_black_to_darkgrey_disabled));
        } catch (Exception e) {
            TrackerManager.getInstance(getContext().getApplicationContext()).logException(e);
            txtTitle.setTextColor(ResourceUtils.getColor(getContext(), R.color.black));
        }

        try {
            mTxtContent.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_black_to_darkgrey_disabled));
        } catch (Exception e) {
            TrackerManager.getInstance(getContext().getApplicationContext()).logException(e);
            mTxtContent.setTextColor(ResourceUtils.getColor(getContext(), R.color.black));
        }

        try {
            txtSummary.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_darkgrey_to_grey_disabled));
        } catch (Exception e) {
            TrackerManager.getInstance(getContext().getApplicationContext()).logException(e);
            txtSummary.setTextColor(ResourceUtils.getColor(getContext(), R.color.dark_grey_text));
        }
    }

    public void setContent(String value) {
        this.mContent = value;
        if(mTxtContent != null) {
            if(mContent == null) {
                mTxtContent.setVisibility(View.GONE);
            } else {
                mTxtContent.setText(mContent);
                mTxtContent.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            String value = mEditText.getText().toString();
            if (callChangeListener(value)) {
                setText(value);
            }
        }

        Utils.hideKeyboard((Activity) getContext());
    }

    @Override
    protected View onCreateDialogView() {
        return new FrameLayout(getContext());
    }

    @Override
    protected void onPrepareDialog(View view) {
        super.onPrepareDialog(view);

        EditText editText = mEditText;
        editText.setText(getText());

        ViewParent oldParent = editText.getParent();
        if (oldParent != view) {
            if (oldParent != null) {
                ((ViewGroup) oldParent).removeView(editText);
            }
            onAddEditTextToDialogView(view, editText);
        }

        getEditText().setSelection(getEditText().getText().length());
    }

    /**
     * Saves the text to the {@link SharedPreferences}.
     *
     * @param text The text to save
     */
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mText = text;

        persistString(text);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    /**
     * Gets the text from the {@link SharedPreferences}.
     *
     * @return The current preference value.
     */
    public String getText() {
        return mText;
    }

    /**
     * Adds the EditText widget of this preference to the dialog's view.
     *
     * @param view The dialog view.
     */
    protected void onAddEditTextToDialogView(View view, EditText editText) {
        ((ViewGroup) view).addView(editText, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setText(restoreValue ? getPersistedString(mText) : (String) defaultValue);
    }

    @Override
    protected boolean needInputMethod() {
        return true;
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
    }

    /**
     * Returns the {@link EditText} widget that will be shown in the dialog.
     *
     * @return The {@link EditText} widget that will be shown in the dialog.
     */
    public EditText getEditText() {
        return mEditText;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.text = getText();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setText(myState.text);
    }

    private static class SavedState extends BaseSavedState {
        String text;

        public SavedState(Parcel source) {
            super(source);
            text = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(text);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

}
