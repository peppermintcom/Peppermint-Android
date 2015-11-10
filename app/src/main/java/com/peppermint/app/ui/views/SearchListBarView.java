package com.peppermint.app.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
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
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 17-09-2015.
 */
public class SearchListBarView extends FrameLayout implements AdapterView.OnItemClickListener {

    private static final int MIN_SEARCH_CHARACTERS = 1;

    public interface OnSearchListener {
        void onSearch(String searchText);
    }

    public interface ListItem {
        boolean isSearchable();
        String getText();
        int getDrawableResource();
    }

    private LayoutInflater mLayoutInflater;
    private InputMethodManager mInputMethodManager;

    private ImageButton mBtnList, mBtnClear;
    private EditText mTxtSearch;
    private ListPopupWindow mListPopupWindow;

    private SearchListBarAdapter mAdapter;
    private int mMinSearchCharacters = MIN_SEARCH_CHARACTERS;
    private int mSelectedItemPosition = 0;

    /*private Handler mHandler = new Handler();*/

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* nothing to do here */ }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { /* nothing to do here */ }
        @Override
        public void afterTextChanged(Editable s) {
            /*mHandler.removeCallbacks(mSearchRunnable);
            mHandler.postDelayed(mSearchRunnable, 10);*/
            mSearchRunnable.run();
        }
    };

    private Runnable mSearchRunnable = new Runnable() {
        @Override
        public void run() {
            String searchText = getSearchText();

            // go back to recent/fav contacts if the search field was emptied
            if(searchText == null) {
                innerSetSelectedItemPosition(0);
            } else {
                // otherwise, if the current pos doesn't allow searching - move to next
                // position until one that allows searching is found
                if(!getSelectedItem().isSearchable()) {
                    int searchableItemPos = -1;
                    int count = mAdapter.getCount();
                    for(int i=0; i<count && searchableItemPos < 0; i++) {
                        if(mAdapter.getItem(i).isSearchable()) {
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
        mLayoutInflater = LayoutInflater.from(getContext());
        mLayoutInflater.inflate(R.layout.v_search_and_list_box_layout, this);

        mBtnClear = (ImageButton) findViewById(R.id.btnClear);
        mBtnClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSearch(-1);
            }
        });

        mBtnList = (ImageButton) findViewById(R.id.btnList);
        mBtnList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            if(!mListPopupWindow.isShowing()) {
                mListPopupWindow.show();
                // this is required since setSelection() only works when the view is showing
                mListPopupWindow.setSelection(mSelectedItemPosition);
                mBtnList.setEnabled(false);
            }
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

        TypedArray a = getContext().getTheme().obtainStyledAttributes(new int[] {R.attr.normalBoxRadius});
        int offset = a.getDimensionPixelSize(0, 0) + Utils.dpToPx(getContext(), 1);
        a.recycle();

        mListPopupWindow = new ListPopupWindow(getContext());
        mListPopupWindow.setAnchorView(this);
        mListPopupWindow.setVerticalOffset(-offset);
        mListPopupWindow.setOnItemClickListener(this);
        mListPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBtnList.setEnabled(true);
                    }
                }, 250);
            }
        });

        if(attrs != null) {
            a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.SearchListBarView,
                    0, 0);

            try {
                mTxtSearch.setHint(a.getString(R.styleable.SearchListBarView_hint));
                mListPopupWindow.setListSelector(Utils.getDrawable(getContext(), a.getResourceId(R.styleable.SearchListBarView_listSelector, R.drawable.background_transparent_to_solid_pressed)));
            } finally {
                a.recycle();
            }
        }
    }

    public void hideList() {
        if(mListPopupWindow.isShowing()) {
            mListPopupWindow.dismiss();
        }
    }

    private void hideKeyboard() {
        requestFocus();
        mTxtSearch.clearFocus();
        mInputMethodManager.hideSoftInputFromWindow(mTxtSearch.getWindowToken(), 0);
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
        mListPopupWindow.setSelection(position);

        // set imagebutton drawable with selected contact list icon
        ListItem item = (ListItem) mAdapter.getItem(position);
        mBtnList.setImageResource(item.getDrawableResource());

        // reset text if not searchable
        if(!item.isSearchable()) {
            mTxtSearch.removeTextChangedListener(mTextWatcher);
            mTxtSearch.setText("");
            mTxtSearch.addTextChangedListener(mTextWatcher);
        }
    }

    public void setSelectedItemPosition(int position) {
        innerSetSelectedItemPosition(position);
        innerTriggerSearch();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setSelectedItemPosition(position);
        mListPopupWindow.dismiss();
    }

    public void setListAdapter(SearchListBarAdapter adapter) {
        mAdapter = adapter;
        mListPopupWindow.setAdapter(adapter);
    }

    public SearchListBarAdapter getListAdapter() {
        return mAdapter;
    }

    public int getSelectedItemPosition() {
        return mSelectedItemPosition;
    }

    public ListItem getSelectedItem() {
        return (ListItem) mAdapter.getItem(getSelectedItemPosition());
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
        return false;
    }

    public OnSearchListener getOnSearchListener() {
        return mListener;
    }

    public void setOnSearchListener(OnSearchListener mListener) {
        this.mListener = mListener;
    }

    public void setTypeface(Typeface font) {
        mTxtSearch.setTypeface(font);
    }

    public boolean isSearchFocused() {
        return mTxtSearch.isFocused();
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

    public boolean isShowingList() {
        return mListPopupWindow.isShowing();
    }

    public void deinit() {
        if(mListPopupWindow.isShowing()) {
            mListPopupWindow.dismiss();
        }
    }
}
