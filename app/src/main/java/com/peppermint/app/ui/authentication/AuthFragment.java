package com.peppermint.app.ui.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.views.simple.CustomNoScrollListView;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 10-11-2015.
 */
public class AuthFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = AuthFragment.class.getSimpleName();
    private static final int NEW_ACCOUNT_CODE = 1234;

    public static boolean startAuthentication(Activity callerActivity, int requestCode, boolean authorize) {
        PepperMintPreferences prefs = new PepperMintPreferences(callerActivity);
        String displayName = Utils.capitalizeFully(prefs.getFullName());

        // if the display name is not valid, no need to check anything else
        if(displayName != null && displayName.length() > 0 && Utils.isValidName(displayName)) {
            // check if there's already a preferred account
            if (prefs.getGmailPreferences().getPreferredAccountName() != null) {
                TrackerManager.getInstance(callerActivity.getApplicationContext()).setUserEmail(prefs.getGmailPreferences().getPreferredAccountName());

                if(authorize) {
                    // authorize the Gmail API and all other necessary apis
                    SenderServiceManager senderManager = new SenderServiceManager(callerActivity.getApplicationContext());
                    senderManager.startAndAuthorize();
                }

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

    private static final String KEY_FIRST_NAME = TAG + "_FirstName";
    private static final String KEY_LAST_NAME = TAG + "_LastName";
    private static final String KEY_SEL_ACCOUNT = TAG + "_SelectedAccount";

    private static final String SCREEN_ID = "Authentication";

    private Runnable mDismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            dismissPopup();
        }
    };
    private Runnable mValidateDataRunnable = new Runnable() {
        @Override
        public void run() {
            validateData();
        }
    };
    private final Handler mHandler = new Handler();
    private boolean mDestroyed = false;

    private ViewGroup mLytEmpty;
    private EditText mTxtFirstName, mTxtLastName;
    private Button mBtnNext, mBtnAddAccount;
    private AuthArrayAdapter mAdapter;
    private CustomNoScrollListView mListView;
    private Account[] mAccounts;
    private String mSelectedAccount;
    private PepperMintPreferences mPreferences;
    private CustomActionBarActivity mActivity;
    private boolean mDontSetNameFromPrefs = false;

    private PopupWindow mNamePopup;
    private TextView mTxtPopup;

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            validateData();
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (CustomActionBarActivity) context;
        mPreferences = new PepperMintPreferences(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // hold popup
        mTxtPopup = (TextView) inflater.inflate(R.layout.v_name_popup, null);
        mNamePopup = new PopupWindow(mActivity);
        mNamePopup.setContentView(mTxtPopup);
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
                    Rect outRectFirst = new Rect();
                    Rect outRectLast = new Rect();
                    mTxtFirstName.getGlobalVisibleRect(outRectFirst);
                    mTxtLastName.getGlobalVisibleRect(outRectLast);
                    if (!outRectFirst.contains((int) event.getRawX(), (int) event.getRawY()) && !outRectLast.contains((int) event.getRawX(), (int) event.getRawY())) {
                        Utils.hideKeyboard(mActivity);
                    }
                }
                dismissPopup();
                return false;
            }
        });

        // inflate the view
        View v = inflater.inflate(R.layout.f_authentication, container, false);

        mBtnNext = (Button) v.findViewById(R.id.btnNext);
        mBtnNext.setOnClickListener(this);

        mBtnAddAccount = (Button) v.findViewById(R.id.btnAddAccount);
        mBtnAddAccount.setOnClickListener(this);

        mTxtFirstName = (EditText) v.findViewById(R.id.txtFirstName);
        mTxtFirstName.addTextChangedListener(mTextWatcher);
        mTxtLastName = (EditText) v.findViewById(R.id.txtLastName);
        mTxtLastName.addTextChangedListener(mTextWatcher);

        mListView = (CustomNoScrollListView) v.findViewById(android.R.id.list);
        mLytEmpty = (ViewGroup) v.findViewById(android.R.id.empty);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListView.setOnItemClickListener(this);

        String firstName = savedInstanceState != null && savedInstanceState.containsKey(KEY_FIRST_NAME) ? savedInstanceState.getString(KEY_FIRST_NAME) : null;
        String lastName = savedInstanceState != null && savedInstanceState.containsKey(KEY_LAST_NAME) ? savedInstanceState.getString(KEY_LAST_NAME) : null;
        mSelectedAccount = savedInstanceState != null && savedInstanceState.containsKey(KEY_SEL_ACCOUNT) ? savedInstanceState.getString(KEY_SEL_ACCOUNT) : null;

        if(firstName != null || lastName != null) {
            if(firstName != null) {
                mTxtFirstName.setText(firstName);
            }
            if(lastName != null) {
                mTxtLastName.setText(lastName);
            }
            mDontSetNameFromPrefs = true;
        } else {
            mDontSetNameFromPrefs = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_FIRST_NAME, mTxtFirstName.getText().toString());
        outState.putString(KEY_LAST_NAME, mTxtLastName.getText().toString());
        if(mSelectedAccount != null) {
            outState.putString(KEY_SEL_ACCOUNT, mSelectedAccount);
        }
        super.onSaveInstanceState(outState);
    }

    private void refreshAccountList() {
        mAccounts = AccountManager.get(mActivity).getAccountsByType("com.google");
        mAdapter = new AuthArrayAdapter(mActivity, mAccounts);
        mListView.setAdapter(mAdapter);

        if(mAccounts != null && mAccounts.length > 0) {
            mListView.setVisibility(View.VISIBLE);
            mLytEmpty.setVisibility(View.GONE);
            int pos = 0;

            if(mSelectedAccount != null) {
                pos = -1;
                for(int i=0; i<mAccounts.length && pos < 0; i++) {
                    if(mAccounts[i].name.compareTo(mSelectedAccount) == 0) {
                        pos = i;
                    }
                }
                if(pos < 0) {
                    pos = 0;
                }
            }

            mSelectedAccount = mAccounts[pos].name;
            mListView.setItemChecked(pos, true);
        } else {
            mListView.setVisibility(View.GONE);
            mLytEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // although this code is also at onResume() and is executed
        // only after adding it here the list is properly refreshed with the new account
        if(requestCode == NEW_ACCOUNT_CODE && resultCode == Activity.RESULT_OK) {
            refreshAccountList();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAccountList();

        if(!mDontSetNameFromPrefs) {
            mTxtFirstName.setText(mPreferences.getFirstName());
            mTxtLastName.setText(mPreferences.getLastName());
            mDontSetNameFromPrefs = true;
        }
        mTxtFirstName.setSelection(mTxtFirstName.getText().length());
        mTxtLastName.setSelection(mTxtLastName.getText().length());

        TrackerManager.getInstance(getActivity().getApplicationContext()).trackScreenView(SCREEN_ID);

        mHandler.postDelayed(mValidateDataRunnable, 100);
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;
        super.onDestroy();
    }

    private int validateData() {
        dismissPopup();

        if(mTxtFirstName.getText().toString().trim().length() <= 0 || !Utils.isValidName(mTxtFirstName.getText().toString().trim())) {
            mBtnNext.setEnabled(false);
            showPopup(mActivity, mTxtFirstName, R.string.msg_insert_name);
            return 1;
        }

        if(!Utils.isValidName(mTxtLastName.getText().toString().trim())) {
            mBtnNext.setEnabled(false);
            showPopup(mActivity, mTxtLastName, R.string.msg_insert_name);
            return 2;
        }

        if(mAccounts == null || mAccounts.length <= 0 || mListView.getCheckedItemPosition() < 0) {
            mBtnNext.setEnabled(false);
            showPopup(mActivity, mBtnAddAccount, R.string.msg_insert_account);
            return 3;
        }

        mBtnNext.setEnabled(true);
        return 0;
    }

    @Override
    public void onClick(View v) {
        if(v.equals(mBtnAddAccount)) {
            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {"com.google"});
            startActivityForResult(intent, NEW_ACCOUNT_CODE);
            return;
        }

        if(v.equals(mBtnNext)) {
            if(validateData() != 0) {
                return;
            }

            mPreferences.setFirstName(Utils.capitalizeFully(mTxtFirstName.getText().toString()));
            mPreferences.setLastName(Utils.capitalizeFully(mTxtLastName.getText().toString()));
            mPreferences.getGmailPreferences().setPreferredAccountName(mAccounts[mListView.getCheckedItemPosition()].name);

            mActivity.setResult(Activity.RESULT_OK);
            mActivity.finish();
        }
    }

    private void dismissPopup() {
        if (mNamePopup.isShowing() && !isDetached() && !mDestroyed) {
            mNamePopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    private void showPopup(final Activity context, View parent, int strResId) {
        if(parent.getWindowToken() == null) {
            return;
        }

        Rect outRect = new Rect();
        parent.getGlobalVisibleRect(outRect);

        dismissPopup();
        mTxtPopup.setText(strResId);
        mNamePopup.showAtLocation(parent, Gravity.NO_GRAVITY, Utils.dpToPx(mActivity, 40), outRect.bottom);
        mHandler.postDelayed(mDismissPopupRunnable, 6000);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSelectedAccount = mAccounts[position].name;
        mListView.setItemChecked(position, true);
        validateData();
    }
}
