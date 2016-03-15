package com.peppermint.app.data;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Represents a way of contacting a particular recipient.
 */
public class ChatRecipient implements Serializable {

    private long mId, mRawContactId, mContactId;
    private long mChatId;

    private String mDisplayName;
    private String mMimeType;
    private String mVia;
    private String mPhotoUri;
    private String mAddedTimestamp;

    public ChatRecipient() {
    }

    public ChatRecipient(long mId, long mRawContactId, long mContactId, long mChatId, String mDisplayName, String mMimeType, String mVia, String mPhotoUri, String mAddedTimestamp) {
        this.mId = mId;
        this.mRawContactId = mRawContactId;
        this.mContactId = mContactId;
        this.mChatId = mChatId;
        this.mDisplayName = mDisplayName;
        this.mMimeType = mMimeType;
        this.mVia = mVia;
        this.mPhotoUri = mPhotoUri;
        this.mAddedTimestamp = mAddedTimestamp;
    }

    public ChatRecipient(long mChatId, ContactRaw contactRaw, String mAddedTimestamp) {
        this.mChatId = mChatId;
        this.mAddedTimestamp = mAddedTimestamp;
        setFromRawContact(contactRaw);
    }

    public void setFromRawContact(ContactRaw contactRaw) {
        setRawContactId(contactRaw.getRawId());
        setContactId(contactRaw.getEmailOrPhoneContactId());
        setDisplayName(contactRaw.getDisplayName());
        setMimeType(contactRaw.getContactMimetype());
        setVia(contactRaw.getContactVia());
        setPhotoUri(contactRaw.getPhotoUri());
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public long getRawContactId() {
        return mRawContactId;
    }

    public void setRawContactId(long mRawContactId) {
        this.mRawContactId = mRawContactId;
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

    public long getContactId() {
        return mContactId;
    }

    public void setContactId(long mContactId) {
        this.mContactId = mContactId;
    }

    public long getChatId() {
        return mChatId;
    }

    public void setChatId(long mChatId) {
        this.mChatId = mChatId;
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

    public boolean isPhone() {
        return mMimeType != null && mMimeType.compareTo(Contact.PHONE_MIMETYPE) == 0;
    }

    public boolean isEmail() {
        return mMimeType != null && mMimeType.compareTo(Contact.EMAIL_MIMETYPE) == 0;
    }

    @Override
    public String toString() {
        return "ChatRecipient{" +
                "mId=" + mId +
                ", mRawContactId=" + mRawContactId +
                ", mContactId=" + mContactId +
                ", mChatId=" + mChatId +
                ", mDisplayName='" + mDisplayName + '\'' +
                ", mMimeType='" + mMimeType + '\'' +
                ", mVia='" + mVia + '\'' +
                ", mPhotoUri='" + mPhotoUri + '\'' +
                ", mAddedTimestamp='" + mAddedTimestamp + '\'' +
                '}';
    }
}
