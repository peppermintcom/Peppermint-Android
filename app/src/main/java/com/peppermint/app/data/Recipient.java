package com.peppermint.app.data;

import java.io.Serializable;

/**
 * Created by NunoLuz on 26/08/2015.
 */
public class Recipient implements Serializable {

    private long mId;
    private long mRawId;
    private boolean mStarred;
    private String mMimeType;
    private String mPhotoUri;
    private String mName;
    private String mType;
    private String mVia;

    public Recipient(long id, long rawId, boolean starred, String mimeType, String name, String type, String photo, String via) {
        this.mId = id;
        this.mRawId = rawId;
        this.mStarred = starred;
        this.mMimeType = mimeType;
        this.mName = name;
        this.mType = type;
        this.mPhotoUri = photo;
        this.mVia = via;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getType() {
        return mType;
    }

    public void setType(String mType) {
        this.mType = mType;
    }

    public String getPhotoUri() {
        return mPhotoUri;
    }

    public void setPhotoUri(String mPhotoUri) {
        this.mPhotoUri = mPhotoUri;
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

    public boolean isStarred() {
        return mStarred;
    }

    public void setStarred(boolean mStarred) {
        this.mStarred = mStarred;
    }

    public String getVia() {
        return mVia;
    }

    public void setVia(String mVia) {
        this.mVia = mVia;
    }
}
