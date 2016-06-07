package com.peppermint.app.dal.recipient;

import com.peppermint.app.dal.DataObject;
import com.peppermint.app.dal.contact.ContactRaw;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Represents a way of contacting a person.
 */
public class Recipient extends DataObject implements Serializable {

    public static final int FIELD_DROIDCONTACT_RAW_ID = 1;
    public static final int FIELD_DROIDCONTACT_ID = 2;
    public static final int FIELD_DROIDCONTACT_DATA_ID = 3;

    public static final int FIELD_DISPLAY_NAME = 4;
    public static final int FIELD_MIME_TYPE = 5;
    public static final int FIELD_VIA = 6;
    public static final int FIELD_PHOTO_URI = 7;
    public static final int FIELD_ADDED_TIMESTAMP = 8;
    public static final int FIELD_PEPPERMINT = 9;

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
        long oldValue = this.mDroidContactRawId;
        this.mDroidContactRawId = mDroidContactRawId;
        registerUpdate(FIELD_DROIDCONTACT_RAW_ID, oldValue, mDroidContactRawId);
    }

    public long getDroidContactId() {
        return mDroidContactId;
    }

    public void setDroidContactId(long mDroidContactId) {
        long oldValue = this.mDroidContactId;
        this.mDroidContactId = mDroidContactId;
        registerUpdate(FIELD_DROIDCONTACT_ID, oldValue, mDroidContactId);
    }

    public long getDroidContactDataId() {
        return mDroidContactDataId;
    }

    public void setDroidContactDataId(long mDroidContactDataId) {
        long oldValue = this.mDroidContactDataId;
        this.mDroidContactDataId = mDroidContactDataId;
        registerUpdate(FIELD_DROIDCONTACT_DATA_ID, oldValue, mDroidContactDataId);
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mMimeType) {
        String oldValue = this.mMimeType;
        this.mMimeType = mMimeType;
        registerUpdate(FIELD_MIME_TYPE, oldValue, mMimeType);
    }

    public String getVia() {
        return mVia;
    }

    public void setVia(String mVia) {
        String oldValue = this.mVia;
        this.mVia = mVia;
        registerUpdate(FIELD_VIA, oldValue, mVia);
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String mDisplayName) {
        String oldValue = this.mDisplayName;
        this.mDisplayName = mDisplayName;
        registerUpdate(FIELD_DISPLAY_NAME, oldValue, mDisplayName);
    }

    public String getPhotoUri() {
        return mPhotoUri;
    }

    public void setPhotoUri(String mPhotoUri) {
        String oldValue = this.mPhotoUri;
        this.mPhotoUri = mPhotoUri;
        registerUpdate(FIELD_PHOTO_URI, oldValue, mPhotoUri);
    }

    public String getAddedTimestamp() {
        return mAddedTimestamp;
    }

    public void setAddedTimestamp(String mAddedTimestamp) {
        String oldValue = this.mAddedTimestamp;
        this.mAddedTimestamp = mAddedTimestamp;
        registerUpdate(FIELD_ADDED_TIMESTAMP, oldValue, mAddedTimestamp);
    }

    public boolean isPeppermint() {
        return mPeppermint;
    }

    public void setPeppermint(boolean mPeppermint) {
        boolean oldValue = this.mPeppermint;
        this.mPeppermint = mPeppermint;
        registerUpdate(FIELD_PEPPERMINT, oldValue, mPeppermint);
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
                ", " + super.toString() +
                '}';
    }
}
