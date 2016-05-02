package com.peppermint.app.data;

import android.provider.ContactsContract;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Represents a {@link ContactsContract} single contact data entry.
 */
public class ContactData implements Serializable {

    public static final String EMAIL_MIMETYPE = ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE;
    public static final String PHONE_MIMETYPE = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
    public static final String PHOTO_MIMETYPE = ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE;
    public static final String NAME_MIMETYPE = ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE;
    public static final String PEPPERMINT_MIMETYPE = "com.peppermint.app.cursor.item/contact_v1";

    private long mId, mRawId, mContactId;
    private boolean mStarred;
    private String mMimeType;
    private String mVia;

    public ContactData() {
    }

    public ContactData(long mId, long mRawId, long mContactId, boolean mStarred, String mMimeType, String mVia) {
        this.mId = mId;
        this.mRawId = mRawId;
        this.mContactId = mContactId;
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

    public long getContactId() {
        return mContactId;
    }

    public void setContactId(long mContactId) {
        this.mContactId = mContactId;
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
        return mMimeType != null && NAME_MIMETYPE.compareTo(mMimeType) == 0;
    }

    public boolean isPhoto() {
        return mMimeType != null && PHOTO_MIMETYPE.compareTo(mMimeType) == 0;
    }

    public boolean isEmail() {
        return mMimeType != null && EMAIL_MIMETYPE.compareTo(mMimeType) == 0;
    }

    public boolean isPeppermint() {
        return mMimeType != null && PEPPERMINT_MIMETYPE.compareTo(mMimeType) == 0;
    }

    @Override
    public String toString() {
        return "ContactData{" +
                "mId=" + mId +
                ", mRawId=" + mRawId +
                ", mContactId=" + mContactId +
                ", mStarred=" + mStarred +
                ", mMimeType='" + mMimeType + '\'' +
                ", mVia='" + mVia + '\'' +
                '}';
    }
}
