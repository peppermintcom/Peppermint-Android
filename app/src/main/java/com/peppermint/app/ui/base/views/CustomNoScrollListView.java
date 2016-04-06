package com.peppermint.app.ui.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

/**
 * Created by Nuno Luz on 05-01-2016.
 */
public class CustomNoScrollListView extends LinearLayout implements OnClickListener {

    private AdapterView.OnItemClickListener mOnItemClickListener;
    private ListAdapter mAdapter;
    private AdapterDataSetObserver mDataSetObserver;

    private int mCheckedPosition = -1;

    @Override
    public void onClick(View v) {
        if(mOnItemClickListener != null) {
            int pos = (int) v.getTag();
            mOnItemClickListener.onItemClick(null, v, pos, mAdapter.getItemId(pos));
        }
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
        }
    }

    public CustomNoScrollListView(Context context) {
        super(context);
        init(context, null);
    }

    public CustomNoScrollListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomNoScrollListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomNoScrollListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);
    }

    public int getCheckedItemPosition() {
        return mCheckedPosition;
    }

    public void setItemChecked(int pos, boolean val) {
        final int count = getChildCount();
        mCheckedPosition = -1;
        if(count <= 0) {
            return;
        }

        for(int i=0; i<count; i++) {
            if(pos == i) {
                mCheckedPosition = i;
                getChildAt(i).setActivated(true);
            } else {
                getChildAt(i).setActivated(false);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }
    }

    private void resetList() {
        removeAllViews();

        if(mAdapter == null) {
            return;
        }

        final int adapterSize = mAdapter.getCount();
        for(int i=0; i<adapterSize; i++) {
            View v = mAdapter.getView(i, null, this);
            v.setTag(i);
            v.setClickable(true);
            v.setOnClickListener(this);
            addView(v);
        }
    }

    public ListAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(ListAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }

        this.mAdapter = adapter;

        resetList();

        mCheckedPosition = -1;

        if (mAdapter != null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }

        requestLayout();
    }

    public AdapterView.OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }
}
