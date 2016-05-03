package com.peppermint.app.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz on 26/08/2015.
 *
 * Represents a {@link android.provider.ContactsContract} raw contact.
 * Aggregates all raw contact data entries with phone, email, name and photo data.
 */
public class ContactRaw implements Serializable {

    private long mRawId;
    private boolean mDeleted = false;
    private String mAccountType, mAccountName;
    private String mDisplayName;
    private String mPhotoUri;
    private long mContactId;

    private Map<String, ContactData> mContactData = new HashMap<>();

    public ContactRaw() {
    }

    public ContactRaw(long mRawId, long mContactId, boolean mDeleted, String mAccountType, String mAccountName, String mDisplayName, String mPhotoUri) {
        this.mRawId = mRawId;
        this.mContactId = mContactId;
        this.mDeleted = mDeleted;
        this.mAccountType = mAccountType;
        this.mAccountName = mAccountName;
        this.mDisplayName = mDisplayName;
        this.mPhotoUri = mPhotoUri;
    }

    public ContactRaw(long mRawId, long mContactId, boolean mDeleted, String mAccountType, String mAccountName, String mDisplayName, String mPhotoUri, ContactData mEmail, ContactData mPeppermint) {
        this.mRawId = mRawId;
        this.mContactId = mContactId;
        this.mDeleted = mDeleted;
        this.mAccountType = mAccountType;
        this.mAccountName = mAccountName;
        this.mDisplayName = mDisplayName;
        this.mPhotoUri = mPhotoUri;

        mContactData.put(ContactData.EMAIL_MIMETYPE, mEmail);
        mContactData.put(ContactData.PEPPERMINT_MIMETYPE, mPeppermint);
    }

    public ContactRaw(long mRawId, long mContactId, boolean mDeleted, String mAccountType, String mAccountName, String mDisplayName, String mPhotoUri, String mEmailVia, String mPeppermintVia) {
        this.mRawId = mRawId;
        this.mContactId = mContactId;
        this.mDeleted = mDeleted;
        this.mAccountType = mAccountType;
        this.mAccountName = mAccountName;
        this.mDisplayName = mDisplayName;
        this.mPhotoUri = mPhotoUri;

        if(mPeppermintVia != null) {
            mContactData.put(ContactData.PEPPERMINT_MIMETYPE, new ContactData(0, mRawId, mContactId, false, ContactData.PEPPERMINT_MIMETYPE, mPeppermintVia));
        } else if(mEmailVia != null) {
            mContactData.put(ContactData.EMAIL_MIMETYPE, new ContactData(0, mRawId, mContactId, false, ContactData.EMAIL_MIMETYPE, mEmailVia));
        }
    }

    public long getMainDataId() {
        return getEmail() != null ? getEmail().getId() : 0;
    }

    public String getMainDataVia() {
        return getEmail() != null ? getEmail().getVia() : null;
    }

    public String getMainDataMimetype() {
        return getEmail() != null ? getEmail().getMimeType() : null;
    }

    public long getContactId() {
        return mContactId;
    }

    public void setContactId(long mContactId) {
        this.mContactId = mContactId;
    }

    public long getRawId() {
        return mRawId;
    }

    public void setRawId(long mRawId) {
        this.mRawId = mRawId;
    }

    public boolean isDeleted() {
        return mDeleted;
    }

    public void setDeleted(boolean mDeleted) {
        this.mDeleted = mDeleted;
    }

    public String getAccountType() {
        return mAccountType;
    }

    public void setAccountType(String mAccountType) {
        this.mAccountType = mAccountType;
    }

    public String getAccountName() {
        return mAccountName;
    }

    public void setAccountName(String mAccountName) {
        this.mAccountName = mAccountName;
    }

    public ContactData getEmail() {
        return mContactData.get(ContactData.EMAIL_MIMETYPE);
    }

    public void setEmail(ContactData mEmail) {
        mContactData.put(ContactData.EMAIL_MIMETYPE, mEmail);
    }

    public ContactData getPeppermint() {
        return mContactData.get(ContactData.PEPPERMINT_MIMETYPE);
    }

    public void setPeppermint(ContactData mPeppermint) {
        mContactData.put(ContactData.PEPPERMINT_MIMETYPE, mPeppermint);
    }

    public void setContactData(ContactData contactData) {
        if(contactData != null) {
            mContactData.put(contactData.getMimeType(), contactData);
        }
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String mDisplayName) {
        this.mDisplayName = mDisplayName;
    }

    public String getPhotoUri() {
        return mPhotoUri;
    }

    public void setPhotoUri(String mPhotoUri) {
        this.mPhotoUri = mPhotoUri;
    }

    @Override
    public String toString() {
        return "ContactRaw{" +
                "mContacts=" + mContactData +
                ", mPhotoUri='" + mPhotoUri + '\'' +
                ", mDisplayName='" + mDisplayName + '\'' +
                ", mAccountName='" + mAccountName + '\'' +
                ", mAccountType='" + mAccountType + '\'' +
                ", mDeleted=" + mDeleted +
                ", mRawId=" + mRawId +
                ", mContactId=" + mContactId +
                '}';
    }
}
