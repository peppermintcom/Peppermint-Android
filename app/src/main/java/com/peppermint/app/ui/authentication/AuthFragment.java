package com.peppermint.app.ui.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 10-11-2015.
 */
public class AuthFragment extends ListFragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    public static boolean startAuthentication(Activity callerActivity, int requestCode) {
        PepperMintPreferences prefs = new PepperMintPreferences(callerActivity);
        String displayName = prefs.getDisplayName().trim();

        // if the display name is not valid, no need to check anything else
        if(displayName != null && displayName.length() > 0 && !Utils.isValidPhoneNumber(displayName)) {
            // check if there's already a preferred account
            if (prefs.getGmailPreferences().getPreferredAccountName() != null) {
                Crashlytics.setUserEmail(prefs.getGmailPreferences().getPreferredAccountName());
                return false;
            }

            // otherwise check if there's only one account and set that one as the preferred
            Account[] accounts = AccountManager.get(callerActivity).getAccountsByType("com.google");
            if (accounts.length == 1) {
                prefs.getGmailPreferences().setPreferredAccountName(accounts[0].name);
                return false;
            }
        }

        // just show the auth screen
        Intent intent = new Intent(callerActivity, AuthActivity.class);
        callerActivity.startActivityForResult(intent, requestCode);
        return true;
    }

    private static final String KEY_NAME = "AuthFragment_Name";

    private Runnable mDismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            dismissPopup();
        }
    };
    private final Handler mHandler = new Handler();
    private boolean mDestroyed = false;

    private EditText mTxtName;
    private Button mBtnAddAccount;
    private AuthArrayAdapter mAdapter;
    private Account[] mAccounts;
    private PepperMintPreferences mPreferences;
    private CustomActionBarActivity mActivity;
    private boolean mDontSetNameFromPrefs = false;

    private PopupWindow mNamePopup;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (CustomActionBarActivity) context;
        mPreferences = new PepperMintPreferences(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) mActivity.getApplication();

        // hold popup
        mNamePopup = new PopupWindow(mActivity);
        mNamePopup.setContentView(inflater.inflate(R.layout.v_name_popup, null));
        //noinspection deprecation
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        mNamePopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mNamePopup.setBackgroundDrawable(Utils.getDrawable(mActivity, R.drawable.img_popup));
        mNamePopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);
        // do not let the popup get in the way of user interaction
        mNamePopup.setFocusable(false);
        mNamePopup.setTouchable(false);

        // global touch interceptor to hide keyboard
        mActivity.getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Rect outRect = new Rect();
                    mTxtName.getGlobalVisibleRect(outRect);
                    if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        Utils.hideKeyboard(mActivity);
                    }
                }
                dismissPopup();
                return false;
            }
        });

        // inflate the view
        View v = inflater.inflate(R.layout.f_authentication, container, false);

        mBtnAddAccount = (Button) v.findViewById(R.id.btnAddAccount);
        mBtnAddAccount.setOnClickListener(this);

        mTxtName = (EditText) v.findViewById(R.id.txtDisplayName);
        mTxtName.setTypeface(app.getFontSemibold());

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(this);

        if(savedInstanceState != null && savedInstanceState.containsKey(KEY_NAME) && savedInstanceState.getString(KEY_NAME) != null) {
            mTxtName.setText(savedInstanceState.getString(KEY_NAME));
            mDontSetNameFromPrefs = true;
        } else {
            mDontSetNameFromPrefs = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_NAME, mTxtName.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAccounts = AccountManager.get(mActivity).getAccountsByType("com.google");
        mAdapter = new AuthArrayAdapter(mActivity, mAccounts);
        getListView().setAdapter(mAdapter);
        if(!mDontSetNameFromPrefs) {
            mTxtName.setText(mPreferences.getDisplayName());
            mDontSetNameFromPrefs = true;
        }
        mTxtName.setSelection(mTxtName.getText().length());
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;
        mActivity = null;
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if(v.equals(mBtnAddAccount)) {
            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {"com.google"});
            startActivity(intent);
            return;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mTxtName.getText().toString().trim().length() <= 0 || Utils.isValidPhoneNumber(mTxtName.getText().toString().trim())) {
            showPopup(mActivity, mTxtName);
            return;
        }

        mPreferences.setDisplayName(mTxtName.getText().toString().trim());
        mPreferences.getGmailPreferences().setPreferredAccountName(mAccounts[position].name);

        SenderServiceManager senderManager = new SenderServiceManager(mActivity.getApplicationContext());
        senderManager.startAndAuthorize();

        mActivity.setResult(Activity.RESULT_OK);
        mActivity.finish();
    }

    private void dismissPopup() {
        if (mNamePopup.isShowing() && !isDetached() && !mDestroyed) {
            mNamePopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    private void showPopup(final Activity context, View parent) {
        Rect outRect = new Rect();
        mTxtName.getGlobalVisibleRect(outRect);

        dismissPopup();
        mNamePopup.showAtLocation(parent, Gravity.NO_GRAVITY, Utils.dpToPx(mActivity, 40), outRect.centerY());
        mHandler.postDelayed(mDismissPopupRunnable, 6000);
    }
}
