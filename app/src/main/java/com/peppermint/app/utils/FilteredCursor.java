package com.peppermint.app.utils;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 11-09-2015.
 *
 * Inspired on the solution found at http://stackoverflow.com/questions/3766688/filtering-a-cursor-the-right-way
 */
public class FilteredCursor implements Cursor {

    public interface FilterCallback {
        void done(FilteredCursor cursor);
    }

    private class FilterTask extends AsyncTask<Void, Void, Void> {
        private Filter mFilter;
        private FilterCallback mDoneCallback;

        public FilterTask(Filter filter, FilterCallback doneCallback) {
            this.mFilter = filter;
            this.mDoneCallback = doneCallback;
        }

        @Override
        protected Void doInBackground(Void... params) {
            filter(mFilter);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(mDoneCallback != null) {
                mDoneCallback.done(FilteredCursor.this);
            }
        }
    }

    private static final String TAG = FilteredCursor.class.getSimpleName();

    public interface Filter {
        boolean isValid(Cursor cursor);
    }

    private Cursor mCursor;
    private List<Integer> mFilterList;
    private int mPos = -1;
    private Filter mFilter;

    public FilteredCursor(Cursor cursor) {
        this.mFilterList = new ArrayList<>();
        this.mCursor = cursor;
    }

    public FilteredCursor(Cursor cursor, Filter filter) {
        this(cursor);
        this.mFilter = filter;
    }

    public void filter() {
        filter(mFilter);
    }

    public void filter(Filter filter) {
        if(filter == null) {
            return;
        }

        Log.d(TAG, "Cursor Count: " + mCursor.getCount());
        mCursor.moveToPosition(-1);
        while(mCursor.moveToNext()) {
            if(filter.isValid(mCursor)) {
                mFilterList.add(mCursor.getPosition());
            }
        }
        mCursor.moveToPosition(-1);
    }

    public void filterAsync(FilterCallback doneCallback) {
        filterAsync(mFilter, doneCallback);
    }

    public void filterAsync(Filter filter, FilterCallback doneCallback) {
        new FilterTask(filter, doneCallback).execute();
    }

    @Override
    public int getCount() {
        return mFilterList.size();
    }

    @Override
    public int getPosition() {
        return mPos;
    }

    @Override
    public boolean move(int offset) {
        return moveToPosition(mPos + offset);
    }

    @Override
    public boolean moveToPosition(int position) {
        // Make sure position isn't past the end of the cursor
        final int count = getCount();
        if (position >= count) {
            mPos = count;
            moveToPosition(count);
            return false;
        }

        // Make sure position isn't before the beginning of the cursor
        if (position < 0) {
            mPos = -1;
            mCursor.moveToPosition(-1);
            return false;
        }

        final int realPosition = mFilterList.get(position);

        // When moving to an empty position, just pretend we did it
        boolean moved = mCursor.moveToPosition(realPosition);
        if(moved) {
            mPos = position;
        } else {
            mPos = -1;
        }
        return moved;
    }

    @Override
    public boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }

    @Override
    public boolean moveToNext() {
        return moveToPosition(mPos + 1);
    }

    @Override
    public boolean moveToPrevious() {
        return moveToPosition(mPos - 1);
    }

    @Override
    public boolean isFirst() {
        return mPos == 0 && getCount() != 0;
    }

    @Override
    public boolean isLast() {
        int count = getCount();
        return mPos == (count - 1) && count != 0;
    }

    @Override
    public boolean isBeforeFirst() {
        return getCount() == 0 || mPos == -1;
    }

    @Override
    public boolean isAfterLast() {
        return getCount() == 0 || mPos == getCount();
    }

    @Override
    public int getColumnIndex(String columnName) {
        return mCursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        return mCursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) {
        return mCursor.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames() {
        return mCursor.getColumnNames();
    }

    @Override
    public int getColumnCount() {
        return mCursor.getColumnCount();
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        return mCursor.getBlob(columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        return mCursor.getString(columnIndex);
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        mCursor.copyStringToBuffer(columnIndex, buffer);
    }

    @Override
    public short getShort(int columnIndex) {
        return mCursor.getShort(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        return mCursor.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        return mCursor.getLong(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        return mCursor.getFloat(columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) {
        return mCursor.getDouble(columnIndex);
    }

    @Override
    public int getType(int columnIndex) {
        return mCursor.getType(columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex) {
        return mCursor.isNull(columnIndex);
    }

    @Override
    public void deactivate() {
        //noinspection deprecation
        mCursor.deactivate();
    }

    @Override
    public boolean requery() {
        //noinspection deprecation
        return mCursor.requery();
    }

    @Override
    public void close() {
        mCursor.close();
        mFilterList.clear();
        mFilterList = null;
        mFilter = null;
    }

    @Override
    public boolean isClosed() {
        return mCursor.isClosed();
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mCursor.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mCursor.unregisterDataSetObserver(observer);
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        mCursor.setNotificationUri(cr, uri);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public Uri getNotificationUri() {
        return mCursor.getNotificationUri();
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return mCursor.getWantsAllOnMoveCalls();
    }

    @Override
    public Bundle getExtras() {
        return mCursor.getExtras();
    }

    @Override
    public Bundle respond(Bundle extras) {
        return mCursor.respond(extras);
    }

    public Cursor getOriginalCursor() {
        return mCursor;
    }

    public List<Integer> getFilterList() {
        return mFilterList;
    }

    public void setFilterList(List<Integer> mFilterList) {
        this.mFilterList = mFilterList;
    }

    public Filter getFilter() {
        return mFilter;
    }

    public void setFilter(Filter mFilter) {
        this.mFilter = mFilter;
    }
}
