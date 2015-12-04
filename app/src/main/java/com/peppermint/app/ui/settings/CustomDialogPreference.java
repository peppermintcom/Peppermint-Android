package com.peppermint.app.ui.settings;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.peppermint.app.R;
import com.peppermint.app.ui.views.dialogs.CustomDialog;

/**
 * Created by Nuno Luz on 02-12-2015.
 *
 * Custom {@link android.preference.DialogPreference} for Peppermint styled dialogs.<br />
 * Allows dialogs of several kinds to be triggered by a preference.
 * <p>
 *     Taken and customized from {@link android.preference.DialogPreference}
 * </p>
 *
 */
public abstract class CustomDialogPreference extends Preference implements
        View.OnClickListener, DialogInterface.OnDismissListener,
        PreferenceManager.OnActivityDestroyListener {

    private CharSequence mDialogTitle;
    private CharSequence mPositiveButtonText;
    private CharSequence mNegativeButtonText;
    private int mDialogLayoutResId;

    /** The dialog, if it is showing. */
    private CustomDialog mDialog;

    /** Which button was clicked. */
    private int mWhichButtonClicked;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public CustomDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CustomDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomDialogPreference(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        mDialogTitle = getTitle();

        setPositiveButtonText(R.string.ok);
        setNegativeButtonText(R.string.cancel);
    }

    /**
     * Sets the title of the dialog. This will be shown on subsequent dialogs.
     *
     * @param dialogTitle The title.
     */
    public void setDialogTitle(CharSequence dialogTitle) {
        mDialogTitle = dialogTitle;
    }

    /**
     * @see #setDialogTitle(CharSequence)
     * @param dialogTitleResId The dialog title as a resource.
     */
    public void setDialogTitle(int dialogTitleResId) {
        setDialogTitle(getContext().getString(dialogTitleResId));
    }

    /**
     * Returns the title to be shown on subsequent dialogs.
     * @return The title.
     */
    public CharSequence getDialogTitle() {
        return mDialogTitle;
    }

    /**
     * Sets the text of the positive button of the dialog. This will be shown on
     * subsequent dialogs.
     *
     * @param positiveButtonText The text of the positive button.
     */
    public void setPositiveButtonText(CharSequence positiveButtonText) {
        mPositiveButtonText = positiveButtonText;
    }

    /**
     * @see #setPositiveButtonText(CharSequence)
     * @param positiveButtonTextResId The positive button text as a resource.
     */
    public void setPositiveButtonText(int positiveButtonTextResId) {
        setPositiveButtonText(getContext().getString(positiveButtonTextResId));
    }

    /**
     * Returns the text of the positive button to be shown on subsequent
     * dialogs.
     *
     * @return The text of the positive button.
     */
    public CharSequence getPositiveButtonText() {
        return mPositiveButtonText;
    }

    /**
     * Sets the text of the negative button of the dialog. This will be shown on
     * subsequent dialogs.
     *
     * @param negativeButtonText The text of the negative button.
     */
    public void setNegativeButtonText(CharSequence negativeButtonText) {
        mNegativeButtonText = negativeButtonText;
    }

    /**
     * @see #setNegativeButtonText(CharSequence)
     * @param negativeButtonTextResId The negative button text as a resource.
     */
    public void setNegativeButtonText(int negativeButtonTextResId) {
        setNegativeButtonText(getContext().getString(negativeButtonTextResId));
    }

    /**
     * Returns the text of the negative button to be shown on subsequent
     * dialogs.
     *
     * @return The text of the negative button.
     */
    public CharSequence getNegativeButtonText() {
        return mNegativeButtonText;
    }

    /**
     * Sets the layout resource that is inflated as the {@link View} to be shown
     * as the content View of subsequent dialogs.
     *
     * @param dialogLayoutResId The layout resource ID to be inflated.
     */
    public void setDialogLayoutResource(int dialogLayoutResId) {
        mDialogLayoutResId = dialogLayoutResId;
    }

    /**
     * Returns the layout resource that is used as the content View for
     * subsequent dialogs.
     *
     * @return The layout resource.
     */
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }

    /**
     * Prepares the dialog builder to be shown when the preference is clicked.
     * Use this to set custom properties on the dialog.
     */
    protected void onPrepareDialog(View view) {
    }

    @Override
    protected void onClick() {
        if (mDialog != null && mDialog.isShowing()) return;
        showDialog(null);
    }

    /**
     * Shows the dialog associated with this Preference. This is normally initiated
     * automatically on clicking on the preference. Call this method if you need to
     * show the dialog on some other event.
     *
     * @param state Optional instance state to restore on the dialog
     */
    protected void showDialog(Bundle state) {
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;

        final CustomDialog dialog = new CustomDialog(getContext());
        dialog.setCancelable(true);
        dialog.setLayout(mDialogLayoutResId);
        dialog.setTitleText(mDialogTitle);
        dialog.setPositiveButtonText(mPositiveButtonText);
        dialog.setPositiveButtonListener(this);
        dialog.setNegativeButtonText(mNegativeButtonText);
        dialog.setNegativeButtonListener(this);

        View contentView = onCreateDialogView();
        if (contentView != null) {
            onPrepareDialog(contentView);
            dialog.setLayoutView(contentView);
        }

        if (state != null) {
            dialog.onRestoreInstanceState(state);
        }
        if (needInputMethod()) {
            requestInputMethod(dialog);
        }

        dialog.setOnDismissListener(this);
        dialog.show();

        mDialog = dialog;
    }

    /**
     * Creates the content view for the dialog (if a custom content view is
     * required). By default, it inflates the dialog layout resource if it is
     * set.
     *
     * @return The content View for the dialog.
     * @see #setLayoutResource(int)
     */
    protected View onCreateDialogView() {
        if (mDialogLayoutResId == 0) {
            return null;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        return inflater.inflate(mDialogLayoutResId, null);
    }

    /**
     * Returns whether the preference needs to display a soft input method when the dialog
     * is displayed. Default is false. Subclasses should override this method if they need
     * the soft input method brought up automatically.
     */
    protected boolean needInputMethod() {
        return false;
    }

    /**
     * Sets the required flags on the dialog window to enable input method window to show up.
     */
    private void requestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button1) {
            mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;
        } else if(v.getId() == R.id.button2) {
            mWhichButtonClicked = DialogInterface.BUTTON_NEUTRAL;
        } if(v.getId() == R.id.button3) {
            mWhichButtonClicked = DialogInterface.BUTTON_POSITIVE;
        }

        // dismiss on click
        if(mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    public void onDismiss(DialogInterface dialog) {
        mDialog = null;
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
    }

    /**
     * Called when the dialog is dismissed and should be used to save data to
     * the {@link SharedPreferences}.
     *
     * @param positiveResult Whether the positive button was clicked (true), or
     *            the negative button was clicked or the dialog was canceled (false).
     */
    protected void onDialogClosed(boolean positiveResult) {
    }

    /**
     * Gets the dialog that is shown by this preference.
     *
     * @return The dialog, or null if a dialog is not being shown.
     */
    public Dialog getDialog() {
        return mDialog;
    }

    public void onActivityDestroy() {

        if (mDialog == null || !mDialog.isShowing()) {
            return;
        }

        mDialog.dismiss();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = true;
        myState.dialogBundle = mDialog.onSaveInstanceState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (myState.isDialogShowing) {
            showDialog(myState.dialogBundle);
        }
    }

    private static class SavedState extends BaseSavedState {
        boolean isDialogShowing;
        Bundle dialogBundle;

        public SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeBundle(dialogBundle);
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
