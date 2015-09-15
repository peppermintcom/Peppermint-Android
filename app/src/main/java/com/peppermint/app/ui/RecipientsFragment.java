package com.peppermint.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;

import com.peppermint.app.R;
import com.peppermint.app.RecordActivity;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

import java.util.List;

public class RecipientsFragment extends ListFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    private static final int REQUEST_RECORD = 1;

    private ImageButton mMenuButton;
    private EditText mSearchText;

    private Spinner mRecipientTypeSpinner;
    private RecipientTypeAdapter mRecipientTypeAdapter;

    private RecipientAdapter mCursorAdapter;

    private PepperMintPreferences mPreferences;

    private boolean mListShown;
    private View mProgressContainer;
    private View mListContainer;

    //private AlertDialog mInternetDialog;

    public RecipientsFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPreferences = new PepperMintPreferences(activity);

     /*   AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.msg_no_internet);
        builder.setPositiveButton(R.string.go_to_internet_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        mInternetDialog = builder.create();*/
    }

    protected void removeSearchTextFocus() {
        if(getView() != null) {
            getView().requestFocus();
        }
        mSearchText.clearFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.recipients_layout, container, false);

        v.findViewById(R.id.touchInterceptor).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (mSearchText.isFocused()) {
                        Rect outRect = new Rect();
                        mSearchText.getGlobalVisibleRect(outRect);
                        if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                            removeSearchTextFocus();
                        }
                    }
                }
                return false;
            }
        });

        mListContainer =  v.findViewById(R.id.listContainer);
        mProgressContainer = v.findViewById(R.id.progressContainer);
        mListShown = true;

        mRecipientTypeAdapter = new RecipientTypeAdapter(getActivity(), RecipientType.getAll(getActivity()));

        mRecipientTypeSpinner = (Spinner) v.findViewById(R.id.type);
        mRecipientTypeSpinner.setAdapter(mRecipientTypeAdapter);
        mRecipientTypeSpinner.setOnItemSelectedListener(this);

        mMenuButton = (ImageButton) v.findViewById(R.id.menu);
        mMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });

        mSearchText = (EditText) v.findViewById(R.id.search);
        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 2) {
                    filterData();
                }
            }
        });
        /*mSearchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mRecipientTypeSpinner.setSelection(1, true);
                }
            }
        });*/

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Sets the adapter for the ListView
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        filterData();
    }

    @Override
    public void onDestroy() {
        if(mCursorAdapter != null) {
            mCursorAdapter.changeCursor(null);
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_RECORD) {
            if(resultCode == Activity.RESULT_OK && mRecipientTypeSpinner.getSelectedItemPosition() != 0) {
                mRecipientTypeSpinner.setSelection(0);
                clearSearchFilter();
            }
        }
    }

    public void setListShown(boolean shown, boolean animate){
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        }
    }
    public void setListShown(boolean shown){
        setListShown(shown, true);
    }
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    public void filterData() {
        String filter = mSearchText.getText().toString().trim();
        if(filter.length() < 2) {
            filter = null;
        }

        RecipientType recipientType = (RecipientType) mRecipientTypeSpinner.getSelectedItem();
        if(mRecipientTypeSpinner.getSelectedItemPosition() == 0 && filter != null) {
            mRecipientTypeSpinner.setSelection(1, true);
        }

        List<Long> recentList = mPreferences.getRecentContactUris();
        Long[] recentArray = null;
        if(recentList != null && recentList.size() > 0 && recipientType.isStarred() != null && recipientType.isStarred()) {
            recentArray = recentList.toArray(new Long[recentList.size()]);
        }

        FilteredCursor cursor = RecipientAdapter.getContactsCursor(getActivity(), recentArray, filter, recentArray == null ? recipientType.isStarred() : null, recipientType.getMimeTypes());
        setListShown(false);
        cursor.filterAsync(new FilteredCursor.FilterCallback() {
            @Override
            public void done(FilteredCursor cursor) {
                if (mCursorAdapter != null) {
                    mCursorAdapter.changeCursor(cursor);
                } else {
                    mCursorAdapter = new RecipientAdapter(getActivity(), cursor);
                    getListView().setAdapter(mCursorAdapter);
                }
                mCursorAdapter.notifyDataSetChanged();
                setListShown(true);
            }
        });
    }

    public boolean clearSearchFilter() {
        if(mSearchText.length() > 0) {
            mSearchText.setText("");
            filterData();
            removeSearchTextFocus();
            return true;
        }
        return false;
    }

    // show the options menu
    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        popup.inflate(R.menu.menu_main);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    default:
                        return false;
                }
            }

        });
        popup.show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //if(Utils.isInternetAvailable(getActivity())) {
            Intent recordIntent = new Intent(getActivity(), RecordActivity.class);
            recordIntent.putExtra(RecordFragment.RECIPIENT_EXTRA, mCursorAdapter.getRecipient(position));
            startActivityForResult(recordIntent, REQUEST_RECORD);
        /*} else {
            if(!mInternetDialog.isShowing()) {
                mInternetDialog.show();
            }
        }*/
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        filterData();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
