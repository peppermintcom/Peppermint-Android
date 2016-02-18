package com.peppermint.app.data;

import android.provider.ContactsContract;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 18-02-2016.
 */
public class Contact implements Serializable {

    private long mId, mRawId;
    private boolean mStarred;
    private String mMimeType;
    private String mVia;

    public Contact() {
    }

    public Contact(long mId, long mRawId, boolean mStarred, String mMimeType, String mVia) {
        this.mId = mId;
        this.mRawId = mRawId;
        this.mStarred = mStarred;
        this.mMimeType = mMimeType;
        this.mVia = mVia;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public boolean isStarred() {
        return mStarred;
    }

    public void setStarred(boolean mStarred) {
        this.mStarred = mStarred;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mMimeType) {
        this.mMimeType = mMimeType;
    }

    public long getRawId() {
        return mRawId;
    }

    public void setRawId(long mRawId) {
        this.mRawId = mRawId;
    }

    public String getVia() {
        return mVia;
    }

    public void setVia(String mVia) {
        this.mVia = mVia;
    }

    public boolean isName() {
        return mMimeType != null && ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.compareTo(mMimeType) == 0;
    }

    public boolean isPhoto() {
        return mMimeType != null && ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE.compareTo(mMimeType) == 0;
    }

    public boolean isEmail() {
        return mMimeType != null && ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.compareTo(mMimeType) == 0;
    }

    public boolean isPhone() {
        return mMimeType != null && ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.compareTo(mMimeType) == 0;
    }

    public boolean isPeppermint() {
        return mMimeType != null && RecipientManager.CONTENT_TYPE.compareTo(mMimeType) == 0;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "mId=" + mId +
                ", mRawId=" + mRawId +
                ", mStarred=" + mStarred +
                ", mMimeType='" + mMimeType + '\'' +
                ", mVia='" + mVia + '\'' +
                '}';
    }
}
