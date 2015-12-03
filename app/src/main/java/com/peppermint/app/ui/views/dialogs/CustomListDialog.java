package com.peppermint.app.ui.views.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.peppermint.app.R;
import com.peppermint.app.ui.views.simple.CustomFontTextView;

/**
 * Created by Nuno Luz on 13-11-2015.
 *
 * Custom dialog that asks for confirmation with two "Yes" and "No" buttons.
 * Includes a {@link CustomFontTextView}
 */
public class CustomListDialog extends CustomDialog implements AdapterView.OnItemClickListener {

    private static final String LIST_STATE_KEY = CustomListDialog.class.getCanonicalName() + "_ListState";

    private ListView mListView;
    private ListAdapter mListAdapter;
    private AdapterView.OnItemClickListener mListItemClickListener;

    public CustomListDialog(Context context) {
        super(context);
        setLayout(R.layout.d_custom_list);
        init(context);
    }

    public CustomListDialog(Context context, int themeResId) {
        super(context, themeResId);
        setLayout(R.layout.d_custom_list);
        init(context);
    }

    public CustomListDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        setLayout(R.layout.d_custom_list);
        init(context);
    }

    private void init(Context context) {
        setNegativeButtonText(null);
        setPositiveButtonText(null);
        setNeutralButtonText(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListView = (ListView) findViewById(R.id.list);
        mListView.setOnItemClickListener(this);

        if(mListAdapter != null) {
            mListView.setAdapter(mListAdapter);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        if(bundle == null) {
            bundle = new Bundle();
        }
        if(mListView != null) {
            bundle.putParcelable(LIST_STATE_KEY, mListView.onSaveInstanceState());
        }
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null && mListView != null) {
            Parcelable state = savedInstanceState.getParcelable(LIST_STATE_KEY);
            mListView.onRestoreInstanceState(state);
        }
    }

    public ListView getListView() {
        return mListView;
    }

    public ListAdapter getListAdapter() {
        return mListAdapter;
    }

    public void setListAdapter(ListAdapter mListAdapter) {
        this.mListAdapter = mListAdapter;
        if(mListView != null) {
            mListView.setAdapter(mListAdapter);
        }
    }

    public void setListOnItemClickListener(AdapterView.OnItemClickListener listener) {
        this.mListItemClickListener = listener;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mListItemClickListener != null) {
            mListItemClickListener.onItemClick(parent, view, position, id);
        }
    }
}
