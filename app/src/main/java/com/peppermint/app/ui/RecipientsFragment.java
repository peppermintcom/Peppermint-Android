package com.peppermint.app.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;

import com.peppermint.app.R;
import com.peppermint.app.RecordActivity;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.utils.PepperMintPreferences;

import java.util.List;

public class RecipientsFragment extends ListFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    private ImageButton mMenuButton;
    private EditText mSearchText;

    private Spinner mRecipientTypeSpinner;
    private RecipientTypeAdapter mRecipientTypeAdapter;

    private CursorAdapter mCursorAdapter;

    private PepperMintPreferences mPreferences;

    public RecipientsFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPreferences = new PepperMintPreferences(activity);
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 2) {
                    filterData();
                }
            }
        });
        mSearchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    mRecipientTypeSpinner.setSelection(1, true);
                }
            }
        });

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

    public void filterData() {
        String filter = mSearchText.getText().toString();
        if(filter.length() < 3) {
            filter = null;
        }

        RecipientType recipientType = (RecipientType) mRecipientTypeSpinner.getSelectedItem();

        List<Long> recentList = mPreferences.getRecentContactUris();
        Long[] recentArray = null;
        if(recentList != null && recentList.size() > 0 && recipientType.isStarred() != null && recipientType.isStarred()) {
            recentArray = recentList.toArray(new Long[recentList.size()]);
        }

        mCursorAdapter = RecipientAdapter.get(getActivity(), recentArray, filter, recentArray == null ? recipientType.isStarred() : null, recipientType.getMimeTypes());
        getListView().setAdapter(mCursorAdapter);
        mCursorAdapter.notifyDataSetChanged();
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
        Intent recordIntent = new Intent(getActivity(), RecordActivity.class);
        recordIntent.putExtra(RecordFragment.RECIPIENT_URI_EXTRA, ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, id));
        startActivity(recordIntent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        filterData();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
