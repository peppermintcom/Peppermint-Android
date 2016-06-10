package com.peppermint.app.dal;

import android.util.SparseArray;

/**
 * Created by Nuno Luz on 03-06-2016.
 *
 * A data object that can be stored in a database. Allows tracking all changes performed to the object.<br />
 * Great for updating the object in the database efficiently and triggering update events.
 */
public abstract class DataObject {

    private SparseArray<DataObjectUpdate> mUpdateHistory = new SparseArray<>();

    protected synchronized boolean registerUpdate(int fieldId, Object beforeChangeData, Object afterChangeData) {
        if (beforeChangeData == null && afterChangeData == null) {
            return false;
        }
        if (beforeChangeData == null || afterChangeData == null || !beforeChangeData.equals(afterChangeData)) {
            final DataObjectUpdate change = mUpdateHistory.get(fieldId);
            if (change == null) {
                mUpdateHistory.put(fieldId, new DataObjectUpdate(fieldId, beforeChangeData, afterChangeData));
            } else {
                // remove if before = after
                if((change.getBeforeUpdateValue() == null && change.getBeforeUpdateValue() == afterChangeData) ||
                        (change.getBeforeUpdateValue() != null && afterChangeData != null && change.getBeforeUpdateValue().equals(afterChangeData))) {
                    mUpdateHistory.remove(fieldId);
                } else {
                    change.setAfterUpdateValue(afterChangeData);
                }
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

    @Override
    public String toString() {
        return "UpdateHistory=" + mUpdateHistory;
    }
}
