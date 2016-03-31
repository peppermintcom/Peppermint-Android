package com.peppermint.app.ui.recipients;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.peppermint.app.R;

import java.util.List;

/**
 * Created by Nuno Luz on 17-09-2015.
 *
 * Search bar (edittext) view by item type (spinner).
 */
public class SearchListBarView extends FrameLayout implements AdapterView.OnItemClickListener {

    private static final String TAG = SearchListBarView.class.getSimpleName();

    private static final String SELECTED_POSITION_BEFORE_SEARCH_KEY = TAG + "_SelectedPositionBeforeSearch";
    private static final String SELECTED_POSITION_KEY = TAG + "_SelectedPosition";
    private static final String SEARCH_TEXT_KEY = TAG + "_SearchText";
    private static final String SUPER_STATE_KEY = TAG + "_SuperState";

    private static final int MIN_SEARCH_CHARACTERS = 1;

    public interface OnSearchListener {
        void onSearch(String searchText);
    }

    public interface ListCategory {
        boolean isSearchable();
        String getText();
    }

    private InputMethodManager mInputMethodManager;

    private ImageButton mBtnClear;
    private EditText mTxtSearch;

    private List<? extends ListCategory> mCategories;
    private int mMinSearchCharacters = MIN_SEARCH_CHARACTERS;
    private int mSelectedItemPosition = 0, mSelectedItemPositionBeforeSearch = 0;

    private TextWatcher mTextWatcher = new TextWatcher() {
        private int _startVia = -1;
        private int _endVia = -1;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            _endVia = -1;
            if (count == 1 && after == 0 && s.charAt(start) == '<' && (s.length() > (start+1) && s.charAt(start+1) == '>')) {
                _endVia = start;
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            _startVia = -1;
            if (count == 1 && before == 0 && s.charAt(start) == '<') {
                _startVia = start;
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(_startVia < 0 && _endVia < 0) {
                mSearchRunnable.run();
            } else {
                if(_startVia >= 0) {
                    int keepVia = _startVia;
                    s.append(">");
                    mTxtSearch.setSelection(keepVia + 1);
                }
                if(_endVia >= 0) {
                    s.delete(_endVia, _endVia + 1);
                }
            }
        }
    };

    private Runnable mSearchRunnable = new Runnable() {
        @Override
        public void run() {
            String searchText = getSearchText();

            // go back to recent/fav contacts if the search field was emptied
            if(searchText == null) {
                innerSetSelectedItemPosition(mSelectedItemPositionBeforeSearch);
            } else {
                // otherwise, if the current pos doesn't allow searching - move to next
                // position until one that allows searching is found
                if(!getSelectedItem().isSearchable()) {
                    int searchableItemPos = -1;
                    int count = mCategories.size();
                    for(int i=0; i<count && searchableItemPos < 0; i++) {
                        if(mCategories.get(i).isSearchable()) {
                            searchableItemPos = i;
                        }
                    }
                    if(searchableItemPos >= 0) {
                        innerSetSelectedItemPosition(searchableItemPos);
                    }
                }
            }

            // just trigger the search process
            innerTriggerSearch();
        }
    };

    private OnSearchListener mListener;

    public SearchListBarView(Context context) {
        super(context);
        init(null);
    }

    public SearchListBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SearchListBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SearchListBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        layoutInflater.inflate(R.layout.v_search_and_list_box_layout, this);

        mBtnClear = (ImageButton) findViewById(R.id.btnClear);
        mBtnClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSearch(-1);
            }
        });

        mTxtSearch = (EditText) findViewById(R.id.txtSearch);
        // this disables default restore view state mechanism (it was triggering
        // the text watcher every time the screen was rotated)
        mTxtSearch.setSaveEnabled(false);
        mTxtSearch.addTextChangedListener(mTextWatcher);
        mTxtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });

        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.SearchListBarView,
                    0, 0);

            try {
                mTxtSearch.setHint(a.getString(R.styleable.SearchListBarView_hint));
            } finally {
                a.recycle();
            }
        }
    }

    private void hideKeyboard() {
        requestFocus();
        mTxtSearch.clearFocus();
        mInputMethodManager.hideSoftInputFromWindow(mTxtSearch.getWindowToken(), 0);
    }

    @Override
    public void clearFocus() {
        super.clearFocus();
        removeSearchTextFocus(null);
    }

    public void removeSearchTextFocus(MotionEvent event) {
        if(event == null) {
            hideKeyboard();
            return;
        }

        if (mTxtSearch.isFocused()) {
            Rect outRect = new Rect();
            mTxtSearch.getGlobalVisibleRect(outRect);
            if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                hideKeyboard();
            }
        }
    }

    private void innerTriggerSearch() {
        String searchText = getSearchText();
        if(searchText == null) {
            mBtnClear.setVisibility(GONE);
        } else {
            mBtnClear.setVisibility(VISIBLE);
        }
        if(mListener != null) {
            mListener.onSearch(searchText);
        }
    }

    private void innerSetSelectedItemPosition(int position) {
        mSelectedItemPosition = position;

        // set imagebutton drawable with selected contact list icon
        ListCategory item = mCategories.get(position);

        // reset text if not searchable
        if(!item.isSearchable() && getSearchText() != null) {
            mTxtSearch.removeTextChangedListener(mTextWatcher);
            mTxtSearch.setText("");
            mTxtSearch.addTextChangedListener(mTextWatcher);
        }
    }

    public void setSelectedItemPosition(int position) {
        /*mSelectedItemPositionBeforeSearch = position;*/
        innerSetSelectedItemPosition(position);
        innerTriggerSearch();
    }

    /*public void setSelectedItemPositionBeforeSearch(int position) {
        mSelectedItemPositionBeforeSearch = position;
    }

    public int getSelectedItemPositionBeforeSearch() {
        return mSelectedItemPositionBeforeSearch;
    }*/

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setSelectedItemPosition(position);
    }

    public void setListCategories(List<? extends ListCategory> categories) {
        mCategories = categories;
    }

    public int getSelectedItemPosition() {
        return mSelectedItemPosition;
    }

    public ListCategory getSelectedItem() {
        return mCategories.get(getSelectedItemPosition());
    }

    public boolean clearSearch(int resetToPosition) {
        // reset only if there's a search going on
        if(getSearchText() != null) {
            if(resetToPosition >= 0) {
                // reset to a particular position
                innerSetSelectedItemPosition(resetToPosition);
            }
            mTxtSearch.setText("");
            // also remove the input focus
            removeSearchTextFocus(null);
            return true;
        }

        mTxtSearch.setText("");
        return false;
    }

    public void setOnSearchListener(OnSearchListener mListener) {
        this.mListener = mListener;
    }

    public String getSearchText() {
        String searchText = mTxtSearch.getText().toString().trim();
        if(searchText.length() < mMinSearchCharacters) {
            searchText = null;
        }
        return searchText;
    }

    public void setSearchText(String text) {
        if(text == null) {
            text = "";
        }
        mTxtSearch.setText(text);
    }

    public void deinit() {
    }

    public EditText getSearchEditText() {
        return mTxtSearch;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putCharSequence(SEARCH_TEXT_KEY, mTxtSearch.getText());
        bundle.putInt(SELECTED_POSITION_KEY, mSelectedItemPosition);
        bundle.putInt(SELECTED_POSITION_BEFORE_SEARCH_KEY, mSelectedItemPositionBeforeSearch);
        bundle.putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        super.onRestoreInstanceState(bundle.getParcelable(SUPER_STATE_KEY));
        mTxtSearch.setText(bundle.getCharSequence(SEARCH_TEXT_KEY));
        mSelectedItemPositionBeforeSearch = bundle.getInt(SELECTED_POSITION_BEFORE_SEARCH_KEY, 0);
        innerSetSelectedItemPosition(bundle.getInt(SELECTED_POSITION_KEY, 0));
        innerTriggerSearch();
    }
}
