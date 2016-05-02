package com.peppermint.app.data;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Represents a way of contacting a person.
 */
public class Recipient implements Serializable {

    private long mId, mDroidContactRawId, mDroidContactId, mDroidContactDataId;
    private String mDisplayName;
    private String mMimeType;
    private String mVia;
    private String mPhotoUri;
    private String mAddedTimestamp;
    private boolean mPeppermint;

    public Recipient() {
    }

    public Recipient(long mId, long mDroidContactRawId, long mDroidContactId, long mDroidContactDataId, String mDisplayName, String mMimeType, String mVia, String mPhotoUri, String mAddedTimestamp, boolean mPeppermint) {
        this.mId = mId;
        this.mDroidContactRawId = mDroidContactRawId;
        this.mDroidContactId = mDroidContactId;
        this.mDroidContactDataId = mDroidContactDataId;
        this.mDisplayName = mDisplayName;
        this.mMimeType = mMimeType;
        this.mVia = mVia;
        this.mPhotoUri = mPhotoUri;
        this.mAddedTimestamp = mAddedTimestamp;
        this.mPeppermint = mPeppermint;
    }

    public Recipient(ContactRaw droidContactRaw, String mAddedTimestamp) {
        this.mAddedTimestamp = mAddedTimestamp;
        setFromDroidContactRaw(droidContactRaw);
    }

    /**
     * Fills this instance with the data from the {@link ContactRaw} instance.
     * @param droidContactRaw the ContactRaw instance
     */
    public void setFromDroidContactRaw(ContactRaw droidContactRaw) {
        setDroidContactDataId(droidContactRaw.getMainDataId());
        setDroidContactRawId(droidContactRaw.getRawId());
        setDroidContactId(droidContactRaw.getContactId());
        setDisplayName(droidContactRaw.getDisplayName());
        setMimeType(droidContactRaw.getMainDataMimetype());
        setVia(droidContactRaw.getMainDataVia());
        setPhotoUri(droidContactRaw.getPhotoUri());
        setPeppermint(droidContactRaw.getPeppermint() != null && droidContactRaw.getPeppermint().getVia().compareTo(mVia) == 0);
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public long getDroidContactRawId() {
        return mDroidContactRawId;
    }

    public void setDroidContactRawId(long mDroidContactRawId) {
        this.mDroidContactRawId = mDroidContactRawId;
    }

    public long getDroidContactId() {
        return mDroidContactId;
    }

    public void setDroidContactId(long mDroidContactId) {
        this.mDroidContactId = mDroidContactId;
    }

    public long getDroidContactDataId() {
        return mDroidContactDataId;
    }

    public void setDroidContactDataId(long mDroidContactDataId) {
        this.mDroidContactDataId = mDroidContactDataId;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mMimeType) {
        this.mMimeType = mMimeType;
    }

    public String getVia() {
        return mVia;
    }

    public void setVia(String mVia) {
        this.mVia = mVia;
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

    public String getAddedTimestamp() {
        return mAddedTimestamp;
    }

    public void setAddedTimestamp(String mAddedTimestamp) {
        this.mAddedTimestamp = mAddedTimestamp;
    }

    public boolean isPeppermint() {
        return mPeppermint;
    }

    public void setPeppermint(boolean mPeppermint) {
        this.mPeppermint = mPeppermint;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Recipient) {
            Recipient chatRecipient = (Recipient) o;
            if(chatRecipient.getId() > 0 && mId > 0) {
                return chatRecipient.getId() == mId;
            }
            if(mVia == null || chatRecipient.getVia() == null ||
                    mMimeType == null || chatRecipient.getMimeType() == null) {
                return false;
            }
            return mVia.compareTo(chatRecipient.getVia()) == 0 &&
                    mMimeType.compareTo(chatRecipient.getMimeType()) == 0;
        }

        return super.equals(o);
    }

    @Override
    public String toString() {
        return "Recipient{" +
                "mId=" + mId +
                ", mDroidContactRawId=" + mDroidContactRawId +
                ", mDroidContactId=" + mDroidContactId +
                ", mDroidContactDataId=" + mDroidContactDataId +
                ", mDisplayName='" + mDisplayName + '\'' +
                ", mMimeType='" + mMimeType + '\'' +
                ", mVia='" + mVia + '\'' +
                ", mPhotoUri='" + mPhotoUri + '\'' +
                ", mAddedTimestamp='" + mAddedTimestamp + '\'' +
                ", mPeppermint=" + mPeppermint +
                '}';
    }
}
