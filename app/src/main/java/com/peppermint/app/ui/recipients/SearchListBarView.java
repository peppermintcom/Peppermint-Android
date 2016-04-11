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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.peppermint.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 17-09-2015.
 *
 * Search bar (edittext) view by item type (spinner).
 */
public class SearchListBarView extends FrameLayout {

    private static final String TAG = SearchListBarView.class.getSimpleName();

    private static final String STATE_PARAM_SEARCH_TEXT = TAG + "_stateParamSearchText";
    private static final String STATE_PARAM_SUPER = TAG + "_stateParamSuper";

    private static final int MIN_SEARCH_CHARACTERS = 1;

    public interface OnSearchListener {
        boolean onSearch(String searchText, boolean wasClear);
    }

    private InputMethodManager mInputMethodManager;
    private List<OnSearchListener> mOnSearchListeners = new ArrayList<>();
    private List<Integer> mOnSearchListenerPriorities = new ArrayList<>();
    private int mMinSearchCharacters = MIN_SEARCH_CHARACTERS;

    // UI
    private ImageButton mBtnClear;
    private EditText mTxtSearch;

    private TextWatcher mTextWatcher = new TextWatcher() {
        private int _startVia = -1;
        private int _endVia = -1;
        private String _previousText;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            _previousText = getSearchText();
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
                // just trigger the search process
                innerTriggerSearch(getSearchText() == null && _previousText != null);
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
                clearSearch(false);
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

    private void innerTriggerSearch(boolean wasClear) {
        String searchText = getSearchText();
        if(searchText == null) {
            mBtnClear.setVisibility(GONE);
        } else {
            mBtnClear.setVisibility(VISIBLE);
        }

        int onSearchListenerAmount = mOnSearchListeners.size();
        boolean stopPropagation = false;
        for(int i=0; i<onSearchListenerAmount && !stopPropagation; i++) {
            stopPropagation = mOnSearchListeners.get(i).onSearch(searchText, wasClear);
        }
    }

    public boolean clearSearch(boolean forceTriggerListeners) {
        // reset only if there's a search going on
        if(getSearchText() != null) {
            mTxtSearch.setText("");
            // also remove the input focus
            removeSearchTextFocus(null);
            return true;
        }

        mTxtSearch.setText("");
        if(forceTriggerListeners) {
            innerTriggerSearch(true);
        }
        return false;
    }

    /**
     * Higher priority listeners have the chance to stop the event propagation to other
     * listeners.
     * @param mOnSearchListener the OnSearchListener
     * @param priority the priority value
     */
    public void addOnSearchListener(OnSearchListener mOnSearchListener, int priority) {
        int onSearchListenerAmount = mOnSearchListeners.size();
        int newIndex = -1;
        for(int i=0; i<onSearchListenerAmount && newIndex < 0; i++) {
            if(mOnSearchListenerPriorities.get(i) < priority) {
                newIndex = i;
            }
        }

        if(newIndex < 0) {
            newIndex = onSearchListenerAmount;
        }

        this.mOnSearchListenerPriorities.add(newIndex, priority);
        this.mOnSearchListeners.add(newIndex, mOnSearchListener);
    }

    public boolean removeOnSearchListener(OnSearchListener mOnSearchListener) {
        int index = this.mOnSearchListeners.indexOf(mOnSearchListener);
        if(index < 0) {
            return false;
        }

        this.mOnSearchListenerPriorities.remove(index);
        this.mOnSearchListeners.remove(index);

        return true;
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

    public EditText getSearchEditText() {
        return mTxtSearch;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putCharSequence(STATE_PARAM_SEARCH_TEXT, mTxtSearch.getText());
        bundle.putParcelable(STATE_PARAM_SUPER, super.onSaveInstanceState());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        super.onRestoreInstanceState(bundle.getParcelable(STATE_PARAM_SUPER));
        CharSequence searchCharSequence = bundle.getCharSequence(STATE_PARAM_SEARCH_TEXT);
        String searchText = searchCharSequence == null || searchCharSequence.length() <= 0 ? null : searchCharSequence.toString();
        if(!(searchText == null && getSearchText() == null) &&
                !(searchText != null && getSearchText() != null && searchText.compareTo(getSearchText()) == 0)) {
            // do not trigger the listener while restoring instance state
            List<OnSearchListener> tmpList = mOnSearchListeners;
            mOnSearchListeners = new ArrayList<>();
            mTxtSearch.setText(searchText);
            mOnSearchListeners = tmpList;
            innerTriggerSearch(false);
        }
    }
}
