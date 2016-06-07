package com.peppermint.app.dal;

import android.util.SparseArray;

/**
 * Created by Nuno Luz on 03-06-2016.
 */
public abstract class DataObject {

    private boolean mKeepUpdateHistory = true;
    private SparseArray<DataObjectUpdate> mUpdateHistory = new SparseArray<>();

    protected synchronized boolean registerUpdate(int fieldId, Object beforeChangeData, Object afterChangeData) {
        if(!mKeepUpdateHistory) {
            return false;
        }
        if (beforeChangeData == null && afterChangeData == null) {
            return false;
        }
        if (beforeChangeData == null || afterChangeData == null || !beforeChangeData.equals(afterChangeData)) {
            final DataObjectUpdate change = mUpdateHistory.get(fieldId);
            if (change == null) {
                mUpdateHistory.put(fieldId, new DataObjectUpdate(fieldId, beforeChangeData, afterChangeData));
            } else {
                change.setAfterUpdateValue(afterChangeData);
            }
            return true;
        }
        return false;
    }

    public synchronized SparseArray<DataObjectUpdate> getUpdateHistory() {
        return mUpdateHistory;
    }

    public synchronized void clearUpdateHistory() {
        mUpdateHistory.clear();
    }

    public boolean isKeepUpdateHistory() {
        return mKeepUpdateHistory;
    }

    public void setKeepUpdateHistory(boolean mKeepUpdateHistory) {
        this.mKeepUpdateHistory = mKeepUpdateHistory;
        clearUpdateHistory();
    }

    @Override
    public String toString() {
        return "UpdateHistory [" + mKeepUpdateHistory +
                "] = " + mUpdateHistory;
    }
}
