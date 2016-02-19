package com.peppermint.app.data;

import java.io.Serializable;

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

    private Contact mEmail, mPhone, mPeppermint;

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
        this.mEmail = mEmail;
        this.mPhone = mPhone;
        this.mPeppermint = mPeppermint;
    }

    public long getContactId() {
        // FIXME we are assuming only one is present at a time; we should probably remove this and use getRawId (aggregated contact)
        return mEmail != null ? mEmail.getId() : (mPhone != null ? mPhone.getId() : 0);
    }

    public Contact getContact() {
        // FIXME we are assuming only one is present at a time; we should probably remove this and use getRawId (aggregated contact)
        return mEmail != null ? mEmail : mPhone;
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
        return mEmail;
    }

    public void setEmail(Contact mEmail) {
        this.mEmail = mEmail;
    }

    public Contact getPhone() {
        return mPhone;
    }

    public void setPhone(Contact mPhone) {
        this.mPhone = mPhone;
    }

    public Contact getPeppermint() {
        return mPeppermint;
    }

    public void setPeppermint(Contact mPeppermint) {
        this.mPeppermint = mPeppermint;
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
                "mPeppermint=" + mPeppermint +
                ", mPhone=" + mPhone +
                ", mEmail=" + mEmail +
                ", mPhotoUri='" + mPhotoUri + '\'' +
                ", mDisplayName='" + mDisplayName + '\'' +
                ", mAccountName='" + mAccountName + '\'' +
                ", mAccountType='" + mAccountType + '\'' +
                ", mDeleted=" + mDeleted +
                ", mRawId=" + mRawId +
                '}';
    }
}
