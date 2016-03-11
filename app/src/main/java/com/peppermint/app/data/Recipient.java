package com.peppermint.app.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz on 26/08/2015.
 *
 * Represents a Recipient of a recorded message.
 */
public class Recipient implements Serializable {

    private long mRawId;
    private boolean mDeleted = false;
    private String mAccountType, mAccountName;
    private String mDisplayName;
    private String mPhotoUri;

    private Map<String, Contact> mContacts = new HashMap<>();

    public Recipient() {
    }

    public Recipient(long mRawId, boolean mDeleted, String mAccountType, String mAccountName, String mDisplayName, String mPhotoUri) {
        this.mRawId = mRawId;
        this.mDeleted = mDeleted;
        this.mAccountType = mAccountType;
        this.mAccountName = mAccountName;
        this.mDisplayName = mDisplayName;
        this.mPhotoUri = mPhotoUri;
    }

    public Recipient(long mRawId, boolean mDeleted, String mAccountType, String mAccountName, String mDisplayName, String mPhotoUri, Contact mEmail, Contact mPhone, Contact mPeppermint) {
        this.mRawId = mRawId;
        this.mDeleted = mDeleted;
        this.mAccountType = mAccountType;
        this.mAccountName = mAccountName;
        this.mDisplayName = mDisplayName;
        this.mPhotoUri = mPhotoUri;

        mContacts.put(Contact.EMAIL_MIMETYPE, mEmail);
        mContacts.put(Contact.PHONE_MIMETYPE, mPhone);
        mContacts.put(Contact.PEPPERMINT_MIMETYPE, mPeppermint);
    }

    public Recipient(long mRawId, boolean mDeleted, String mAccountType, String mAccountName, String mDisplayName, String mPhotoUri, String mEmailVia, String mPhoneVia, String mPeppermintVia) {
        this.mRawId = mRawId;
        this.mDeleted = mDeleted;
        this.mAccountType = mAccountType;
        this.mAccountName = mAccountName;
        this.mDisplayName = mDisplayName;
        this.mPhotoUri = mPhotoUri;

        if(mPeppermintVia != null) {
            mContacts.put(Contact.PEPPERMINT_MIMETYPE, new Contact(0, mRawId, false, Contact.PEPPERMINT_MIMETYPE, mPeppermintVia));
        } else if(mEmailVia != null) {
            mContacts.put(Contact.EMAIL_MIMETYPE, new Contact(0, mRawId, false, Contact.EMAIL_MIMETYPE, mEmailVia));
        } else if(mPhoneVia != null) {
            mContacts.put(Contact.PHONE_MIMETYPE, new Contact(0, mRawId, false, Contact.PHONE_MIMETYPE, mPhoneVia));
        }
    }

    public long getEmailOrPhoneContactId() {
        // FIXME we are assuming only one is present at a time; we should probably remove this and use getRawId (aggregated contact)
        return getEmail() != null ? getEmail().getId() : (getPhone() != null ? getPhone().getId() : 0);
    }

    public String getContactVia() {
        return getPeppermint() != null ? getPeppermint().getVia() : (
                getEmail() != null ? getEmail().getVia() : (
                        getPhone() != null ? getPhone().getVia() : null
                        )
                );
    }

    public String getContactMimetype() {
        return getPeppermint() != null ? getPeppermint().getMimeType() : (
                getEmail() != null ? getEmail().getMimeType() : (
                        getPhone() != null ? getPhone().getMimeType() : null
                )
        );
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

    public Contact getEmail() {
        return mContacts.get(Contact.EMAIL_MIMETYPE);
    }

    public void setEmail(Contact mEmail) {
        mContacts.put(Contact.EMAIL_MIMETYPE, mEmail);
    }

    public Contact getPhone() {
        return mContacts.get(Contact.PHONE_MIMETYPE);
    }

    public void setPhone(Contact mPhone) {
        mContacts.put(Contact.PHONE_MIMETYPE, mPhone);
    }

    public Contact getPeppermint() {
        return mContacts.get(Contact.PEPPERMINT_MIMETYPE);
    }

    public void setPeppermint(Contact mPeppermint) {
        mContacts.put(Contact.PEPPERMINT_MIMETYPE, mPeppermint);
    }

    public void setContact(Contact mContact) {
        mContacts.put(mContact.getMimeType(), mContact);
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
        return "Recipient{" +
                "mContacts=" + mContacts +
                ", mPhotoUri='" + mPhotoUri + '\'' +
                ", mDisplayName='" + mDisplayName + '\'' +
                ", mAccountName='" + mAccountName + '\'' +
                ", mAccountType='" + mAccountType + '\'' +
                ", mDeleted=" + mDeleted +
                ", mRawId=" + mRawId +
                '}';
    }
}
