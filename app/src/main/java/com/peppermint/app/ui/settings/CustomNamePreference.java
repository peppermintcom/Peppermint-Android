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
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.views.simple.CustomFontEditText;
import com.peppermint.app.utils.Utils;

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
public class CustomNamePreference extends CustomDialogPreference {

    private static final String TAG = CustomNamePreference.class.getSimpleName();

    private String mFirstNameKey, mLastNameKey;

    private String mContent;
    private TextView mTxtContent;

    private ViewGroup mDialogView;
    private String mFirstName, mLastName;

    public CustomNamePreference(Context context) {
        super(context);
        setLayoutResource(R.layout.v_preference);
        init(context, null);
    }

    public CustomNamePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.v_preference);
        init(context, attrs);
    }

    public CustomNamePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.v_preference);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomNamePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.v_preference);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setDialogLayoutResource(R.layout.d_custom_name);

        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CustomNamePreference,
                    0, 0);

            try {
                mFirstNameKey = a.getString(R.styleable.CustomNamePreference_keyFirstName);
                mLastNameKey = a.getString(R.styleable.CustomNamePreference_keyLastName);
            } finally {
                a.recycle();
            }
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mTxtContent = (TextView) view.findViewById(R.id.content);

        TextView txtTitle = (TextView) view.findViewById(android.R.id.title);
        TextView txtSummary = (TextView) view.findViewById(android.R.id.summary);
        TextView txtContent = (TextView) view.findViewById(R.id.content);

        if(mContent == null) {
            txtContent.setVisibility(View.GONE);
        } else {
            txtContent.setText(mContent);
        }

        txtTitle.setTextColor(Utils.getColor(getContext(), R.color.black));
        txtSummary.setTextColor(Utils.getColor(getContext(), R.color.dark_grey_text));
        txtContent.setTextColor(Utils.getColor(getContext(), R.color.black));

        PeppermintApp app = (PeppermintApp) ((Activity) getContext()).getApplication();
        txtTitle.setTypeface(app.getFontSemibold());
        txtSummary.setTypeface(app.getFontRegular());
        txtContent.setTypeface(app.getFontRegular());

        try {
            txtTitle.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_black_to_darkgrey_disabled));
        } catch (Exception e) {
            TrackerManager.getInstance(getContext().getApplicationContext()).logException(e);
            txtTitle.setTextColor(Utils.getColor(getContext(), R.color.black));
        }

        try {
            txtContent.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_black_to_darkgrey_disabled));
        } catch (Exception e) {
            TrackerManager.getInstance(getContext().getApplicationContext()).logException(e);
            txtContent.setTextColor(Utils.getColor(getContext(), R.color.black));
        }

        try {
            txtSummary.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_darkgrey_to_grey_disabled));
        } catch (Exception e) {
            TrackerManager.getInstance(getContext().getApplicationContext()).logException(e);
            txtSummary.setTextColor(Utils.getColor(getContext(), R.color.dark_grey_text));
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

        if(mDialogView == null) {
            return;
        }

        CustomFontEditText firstNameView = (CustomFontEditText) mDialogView.findViewById(R.id.text1);
        CustomFontEditText lastNameView = (CustomFontEditText) mDialogView.findViewById(R.id.text2);

        if (positiveResult && firstNameView != null && lastNameView != null) {
            String firstName = firstNameView.getText().toString().trim();
            String lastName = lastNameView.getText().toString().trim();
            String value = Utils.capitalizeFully(firstName + " " + lastName).trim();
            if (callChangeListener(value)) {
                setFullName(firstName, lastName);
            }
        }

        Utils.hideKeyboard((Activity) getContext());
    }

    @Override
    protected void onPrepareDialog(View view) {
        super.onPrepareDialog(view);

        mDialogView = (ViewGroup) view;

        CustomFontEditText firstNameView = (CustomFontEditText) mDialogView.findViewById(R.id.text1);
        CustomFontEditText lastNameView = (CustomFontEditText) mDialogView.findViewById(R.id.text2);

        String firstName = getFirstName();
        firstNameView.setText(firstName);
        firstNameView.setSelection(firstName.length());

        String lastName = getLastName();
        lastNameView.setText(lastName);
        lastNameView.setSelection(lastName.length());
    }

    public void setFullName(String fullName) {
        fullName = Utils.capitalizeFully(fullName);
        String[] names = fullName.split("\\s+");

        if(names.length > 1) {
            String lastName = names[names.length - 1].trim();
            String firstName = fullName.substring(0, fullName.length() - names[names.length - 1].length()).trim();
            setFullName(firstName, lastName);
        } else {
            setFullName(fullName, "");
        }
    }

    /**
     * Saves the text to the {@link SharedPreferences}.
     *
     * @param firstName The first name to save
     * @param lastName The last name to save
     */
    public void setFullName(String firstName, String lastName) {
        final boolean wasBlocking = shouldDisableDependents();

        mFirstName = firstName == null ? "" : Utils.capitalizeFully(firstName);
        mLastName = lastName == null ? "" : Utils.capitalizeFully(lastName);

        persistString(Utils.capitalizeFully(mFirstName + " " + mLastName));

        if (shouldPersist()) {
            SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
            editor.putString(mFirstNameKey, mFirstName);
            editor.putString(mLastNameKey, mLastName);
            editor.commit();
        }

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
    public String getFullName() {
        if(mLastName == null && mFirstName == null) {
            return "";
        }

        if(mLastName == null) {
            return Utils.capitalizeFully(mFirstName);
        }

        if(mFirstName == null) {
            return Utils.capitalizeFully(mLastName);
        }

        return Utils.capitalizeFully(mFirstName + " " + mLastName);
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    protected String getPersistedFirstName(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        return getPreferenceManager().getSharedPreferences().getString(mFirstNameKey, defaultReturnValue);
    }

    protected String getPersistedLastName(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        return getPreferenceManager().getSharedPreferences().getString(mLastNameKey, defaultReturnValue);
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
        String[] names = Utils.getFirstAndLastNames(getPersistedString(getFullName()));
        mFirstName = getPersistedFirstName(getFirstName());
        mLastName = getPersistedLastName(getLastName());
        if((mFirstName == null || mFirstName.trim().length() <= 0) && (mLastName == null || mLastName.trim().length() <= 0)) {
            mFirstName = names[0];
            mLastName = names[1];
        }

        /*setFullName(restoreValue ? firstName : (String) defaultValue, restoreValue ? lastName : (String) defaultValue);*/
    }

    @Override
    protected boolean needInputMethod() {
        return true;
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(getFullName()) || super.shouldDisableDependents();
    }

    /**
     * Returns the {@link EditText} widget that will be shown in the dialog.
     *
     * @return The {@link EditText} widget that will be shown in the dialog.
     */
    public ViewGroup getDialogView() {
        return mDialogView;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.firstName = getFirstName();
        myState.lastName = getLastName();
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
        setFullName(myState.firstName, myState.lastName);
    }

    private static class SavedState extends BaseSavedState {
        String firstName, lastName;

        public SavedState(Parcel source) {
            super(source);
            firstName = source.readString();
            lastName = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(firstName);
            dest.writeString(lastName);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

}
