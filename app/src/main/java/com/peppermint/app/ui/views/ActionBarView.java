package com.peppermint.app.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;

import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 17-09-2015.
 */
public class ActionBarView extends FrameLayout implements AdapterView.OnItemClickListener {

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

    private ImageButton mBtnList;
    private EditText mTxtSearch;
    private ListPopupWindow mListPopupWindow;

    private ActionBarListAdapter mAdapter;
    private int mMinSearchCharacters = MIN_SEARCH_CHARACTERS;
    private int mSelectedItemPosition = 0;

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(mTxtSearch.length() <= 0) {
                innerSetSelectedItemPosition(0);
            }
            doSearch();
        }
    };

    private OnSearchListener mListener;

    public ActionBarView(Context context) {
        super(context);
        init(null);
    }

    public ActionBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ActionBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ActionBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mLayoutInflater = LayoutInflater.from(getContext());
        mLayoutInflater.inflate(R.layout.v_search_and_list_box_layout, this);

        mBtnList = (ImageButton) findViewById(R.id.btnList);
        mBtnList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            if(!mListPopupWindow.isShowing()) {
                mListPopupWindow.show();
                mBtnList.setEnabled(false);
            }
            }
        });

        mTxtSearch = (EditText) findViewById(R.id.txtSearch);
        mTxtSearch.addTextChangedListener(mTextWatcher);

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
                    R.styleable.ActionBarView,
                    0, 0);

            try {
                mTxtSearch.setHint(a.getString(R.styleable.ActionBarView_hint));
                mListPopupWindow.setListSelector(Utils.getDrawable(getContext(), a.getResourceId(R.styleable.ActionBarView_listSelector, R.drawable.background_transparent_to_solid_pressed)));
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

    public void removeSearchTextFocus(MotionEvent event) {
        if(event == null) {
            requestFocus();
            mTxtSearch.clearFocus();
            mInputMethodManager.hideSoftInputFromWindow(mTxtSearch.getWindowToken(), 0);
            return;
        }

        if (mTxtSearch.isFocused()) {
            Rect outRect = new Rect();
            mTxtSearch.getGlobalVisibleRect(outRect);
            if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                requestFocus();
                mTxtSearch.clearFocus();
                mInputMethodManager.hideSoftInputFromWindow(mTxtSearch.getWindowToken(), 0);
            }
        }
    }

    private void doSearch() {
        String searchText = getSearchText();
        if(searchText != null && !getSelectedItem().isSearchable()) {
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

        if(mListener != null) {
            mListener.onSearch(getSearchText());
        }
    }

    private void innerSetSelectedItemPosition(int position) {
        mSelectedItemPosition = position;
        ListItem item = (ListItem) mAdapter.getItem(position);
        mBtnList.setImageResource(item.getDrawableResource());
        mListPopupWindow.setSelection(position);
        mTxtSearch.removeTextChangedListener(mTextWatcher);
        if(!item.isSearchable()) {
            mTxtSearch.setText("");
        }
        mTxtSearch.addTextChangedListener(mTextWatcher);
    }

    public void setSelectedItemPosition(int position) {
        innerSetSelectedItemPosition(position);
        doSearch();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setSelectedItemPosition(position);
        mListPopupWindow.dismiss();
    }

    public void setListAdapter(ActionBarListAdapter adapter) {
        mAdapter = adapter;
        mListPopupWindow.setAdapter(adapter);
    }

    public ActionBarListAdapter getListAdapter() {
        return mAdapter;
    }

    public int getSelectedItemPosition() {
        int pos = mListPopupWindow.getSelectedItemPosition();
        if(pos == AdapterView.INVALID_POSITION) {
            pos = mSelectedItemPosition;
        }
        return pos;
    }

    public ListItem getSelectedItem() {
        return (ListItem) mAdapter.getItem(getSelectedItemPosition());
    }

    public boolean clearSearch(int resetToPosition) {
        if(mTxtSearch.length() > 0) {
            if(resetToPosition >= 0) {
                innerSetSelectedItemPosition(resetToPosition);
            }
            mTxtSearch.setText("");
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
