package com.peppermint.app.dal;

/**
 * Created by Nuno Luz on 03-06-2016.
 *
 * Represents a set of changes performed to a data object's fields.
 */
public class DataObjectUpdate {
    private int mFieldId;
    private Object mBeforeUpdateValue, mAfterUpdateValue;

    public DataObjectUpdate(int mFieldId, Object mBeforeUpdateValue, Object mAfterUpdateValue) {
        this.mFieldId = mFieldId;
        this.mBeforeUpdateValue = mBeforeUpdateValue;
        this.mAfterUpdateValue = mAfterUpdateValue;
    }

    public int getFieldId() {
        return mFieldId;
    }

    public void setFieldId(int mFieldId) {
        this.mFieldId = mFieldId;
    }

    public Object getBeforeUpdateValue() {
        return mBeforeUpdateValue;
    }

    public void setBeforeUpdateValue(Object mBeforeUpdateValue) {
        this.mBeforeUpdateValue = mBeforeUpdateValue;
    }

    public Object getAfterUpdateValue() {
        return mAfterUpdateValue;
    }

    public void setAfterUpdateValue(Object mAfterUpdateValue) {
        this.mAfterUpdateValue = mAfterUpdateValue;
    }

    @Override
    public String toString() {
        return "DataObjectUpdate{" +
                "mFieldId=" + mFieldId +
                ", mBeforeUpdateValue=" + mBeforeUpdateValue +
                ", mAfterUpdateValue=" + mAfterUpdateValue +
                '}';
    }
}
