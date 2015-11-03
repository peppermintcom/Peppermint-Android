package com.peppermint.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * Created by Nuno Luz on 26/08/2015.
 *
 * Represents a Recipient of a recorded message.
 */
public class Recipient implements Serializable {

    /**
     * Gets the data inside the Cursor's current position and puts it in an instance of the
     * Recipient structure.
     *
     * @param cursor the cursor
     * @return the Recipient instance
     */
    private static Recipient getFromCursor(Cursor cursor) {
        Recipient recipient = new Recipient();
        recipient.setId(cursor.getLong(cursor.getColumnIndex("recipient_id")));
        recipient.setMimeType(cursor.getString(cursor.getColumnIndex("mime_type")));
        recipient.setPhotoUri(cursor.getString(cursor.getColumnIndex("photo_uri")));
        recipient.setName(cursor.getString(cursor.getColumnIndex("name")));
        recipient.setVia(cursor.getString(cursor.getColumnIndex("via")));
        recipient.setType(cursor.getString(cursor.getColumnIndex("account_type")));
        return recipient;
    }

    /**
     * Inserts the supplied recipient into the supplied local database.
     *
     * @param db the local database connection
     * @param recipient the recipient
     * @throws SQLException
     */
    public static void insert(SQLiteDatabase db, Recipient recipient) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("mime_type", recipient.getMimeType());
        cv.put("photo_uri", recipient.getPhotoUri());
        cv.put("name", recipient.getName());
        cv.put("via", recipient.getVia());
        cv.put("account_type", recipient.getType());

        long id = db.insert("tbl_sending_request_recipient", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert recipient!");
        }

        recipient.setId(id);
    }

    /**
     * Updates the supplied recipient data (ID must be supplied).
     * An SQLException is thrown if the recipient ID does not exist in the database.
     *
     * @param db the local database connection
     * @param recipient the recipient
     * @throws SQLException
     */
    public static void update(SQLiteDatabase db, Recipient recipient) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("mime_type", recipient.getMimeType());
        cv.put("photo_uri", recipient.getPhotoUri());
        cv.put("name", recipient.getName());
        cv.put("via", recipient.getVia());
        cv.put("account_type", recipient.getType());

        long id = db.update("tbl_sending_request_recipient", cv, "recipient_id = " + recipient.getId(), null);
        if(id < 0) {
            throw new SQLException("Unable to update recipient!");
        }
    }

    /**
     * If a recipient ID is supplied it performs an update, otherwise, it inserts the recipient.
     *
     * @param db the local database connection
     * @param recipient the recipient
     * @throws SQLException
     */
    public static void insertOrUpdate(SQLiteDatabase db, Recipient recipient) throws  SQLException {
        if(recipient.getId() <= 0) {
            insert(db, recipient);
            return;
        }
        update(db, recipient);
    }

    /**
     * Deletes the supplied recipient data (ID must be supplied).
     * An SQLException is thrown if the recipient ID does not exist in the database.
     *
     * @param db the local database connection
     * @param recipient the recipient
     * @throws SQLException
     */
    public static void delete(SQLiteDatabase db, Recipient recipient) throws SQLException {
        long id = db.delete("tbl_sending_request_recipient", "recipient_id = " + recipient.getId(), null);
        if(id < 0) {
            throw new SQLException("Unable to delete the recipient!");
        }
    }

    /**
     * Obtains the recipient with the supplied ID from the database.
     *
     * @param db the local database connection
     * @param id the recipient ID
     * @return the recipient instance with all data
     */
    public static Recipient get(SQLiteDatabase db, long id) {
        Recipient recipient = null;
        Cursor cursor = db.rawQuery("SELECT * FROM tbl_sending_request_recipient WHERE recipient_id = " + id, null);
        if(cursor != null && cursor.moveToFirst()) {
            recipient = getFromCursor(cursor);
        }
        return recipient;
    }

    /**
     * Get the recipient with the supplied "via" value from the local database.
     * @param db the local database connection
     * @param via the via value
     * @return the recipient instance with all data
     */
    public static Recipient getByVia(SQLiteDatabase db, String via) {
        Recipient recipient = null;
        Cursor cursor = db.rawQuery("SELECT * FROM tbl_sending_request_recipient WHERE via = ?;", new String[]{ via });
        if(cursor != null && cursor.moveToFirst()) {
            recipient = getFromCursor(cursor);
        }
        return recipient;
    }

    private long mContactId;
    private long mId;
    private boolean mStarred;
    private String mMimeType;
    private String mPhotoUri;
    private String mName;
    private String mType;
    private String mVia;

    public Recipient() {
    }

    public Recipient(long contactId, boolean starred, String mimeType, String name, String type, String photo, String via) {
        this.mContactId = contactId;
        this.mStarred = starred;
        this.mMimeType = mimeType;
        this.mName = name;
        this.mType = type;
        this.mPhotoUri = photo;
        this.mVia = via;
    }

    public long getContactId() {
        return mContactId;
    }

    public void setContactId(long mId) {
        this.mContactId = mId;
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

    public String getVia() {
        return mVia;
    }

    public void setVia(String mVia) {
        this.mVia = mVia;
    }

}
