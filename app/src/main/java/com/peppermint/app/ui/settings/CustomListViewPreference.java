package com.peppermint.app.ui.settings;

import android.annotation.TargetApi;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.views.CustomFontEditText;
import com.peppermint.app.utils.ResourceUtils;

import java.util.List;

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
public class CustomListViewPreference extends CustomDialogPreference implements AdapterView.OnItemClickListener {

    public static class ListItem {
        public String key, value;

        public ListItem(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof ListItem) {
                return ((ListItem) o).key == key;
            }
            return super.equals(o);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final String TAG = CustomListViewPreference.class.getSimpleName();

    private String mContent;
    private TextView mTxtContent;

    private ListView mDialogView;
    private ArrayAdapter<ListItem> mListAdapter;
    private String mSelectedKey;

    public CustomListViewPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.v_preference);
        init(context, null);
    }

    public CustomListViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.v_preference);
        init(context, attrs);
    }

    public CustomListViewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.v_preference);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomListViewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.v_preference);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setDialogLayoutResource(R.layout.d_custom_list);
        mListAdapter = new ArrayAdapter<>(context, R.layout.d_custom_textview, R.id.text);
    }

    public void setItems(List<ListItem> items) {
        mListAdapter.clear();
        mListAdapter.addAll(items);
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

        if(mDialogView == null || mListAdapter == null) {
            return;
        }

        final String selectedKey = mListAdapter.getItem(mDialogView.getCheckedItemPosition()).key;
        if (callChangeListener(selectedKey)) {
            setText(selectedKey);
        }
    }

    @Override
    protected void onPrepareDialog(View view) {
        super.onPrepareDialog(view);

        mDialogView = (ListView) view;
        mDialogView.setAdapter(mListAdapter);
        mDialogView.setOnItemClickListener(this);

        if(mListAdapter != null) {
            final String selectedKey = getText();
            final int itemCount = mListAdapter.getCount();
            int selectedIndex = 0;
            for (int i = 0; i < itemCount && selectedIndex <= 0; i++) {
                ListItem itItem = mListAdapter.getItem(i);
                if((itItem.key == null && selectedKey == null) ||
                        (itItem.key != null && selectedKey != null && itItem.key.equals(selectedKey))) {
                    selectedIndex = i;
                }
            }
            mDialogView.setSelection(selectedIndex);
            mDialogView.setItemChecked(selectedIndex, true);
        }
    }

    /**
     * Saves the text to the {@link SharedPreferences}.
     *
     * @param text The text to save
     */
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mSelectedKey = text;

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
        return mSelectedKey;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setText(restoreValue ? getPersistedString(mSelectedKey) : (String) defaultValue);
    }

    @Override
    protected boolean needInputMethod() {
        return false;
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(mSelectedKey) || super.shouldDisableDependents();
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ListItem listItem = (ListItem) mDialogView.getSelectedItem();
        final String selectedKey = listItem != null ? listItem.key : null;
        persistString(selectedKey);
        getDialog().dismiss();
    }

}
