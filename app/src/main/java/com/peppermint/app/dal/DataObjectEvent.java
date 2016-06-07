package com.peppermint.app.dal;

import android.util.SparseArray;

/**
 * Created by Nuno Luz on 03-06-2016.
 *
 * An event triggered by some update/insert/delete operation on a data object.
 */
public class DataObjectEvent<T extends DataObject> {

    public static final int TYPE_CREATE = 1;
    public static final int TYPE_UPDATE = 2;
    public static final int TYPE_DELETE = 3;

    private T mDataObject;
    private SparseArray<DataObjectUpdate> mUpdates;
    private int mType;

    public DataObjectEvent(int mType, T mDataObject, SparseArray<DataObjectUpdate> mUpdates) {
        this.mType = mType;
        this.mDataObject = mDataObject;
        this.mUpdates = mUpdates;
    }

    public T getDataObject() {
        return mDataObject;
    }

    public void setDataObject(T mDataObject) {
        this.mDataObject = mDataObject;
    }

    public SparseArray<DataObjectUpdate> getUpdates() {
        return mUpdates;
    }

    public void setUpdates(SparseArray<DataObjectUpdate> mUpdates) {
        this.mUpdates = mUpdates;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    @Override
    public String toString() {
        return "DataObjectEvent{" +
                (mType == TYPE_CREATE ? "TYPE_CREATE" : (mType == TYPE_UPDATE ? "TYPE_UPDATE" : (mType == TYPE_DELETE ? "TYPE_DELETE" : mType))) +
                ", mDataObject=" + mDataObject +
                ", mUpdates=" + mUpdates +
                '}';
    }
}
