package com.peppermint.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents a file/recording send request to a particular recipient.
 */
public class SendingRequest {

    /**
     * Gets the sending request data inside the Cursor's current position and puts it in an instance
     * of the SendingRequest structure.
     *
     * @param cursor the cursor
     * @return the SendingRequest instance
     */
    public static SendingRequest getFromCursor(SQLiteDatabase db, Cursor cursor) {
        SendingRequest sendingRequest = new SendingRequest();
        sendingRequest.setId(UUID.fromString(cursor.getString(cursor.getColumnIndex("sending_request_uuid"))));
        sendingRequest.setSubject(cursor.getString(cursor.getColumnIndex("subject")));
        sendingRequest.setBody(cursor.getString(cursor.getColumnIndex("body")));
        if(db != null) {
            sendingRequest.setRecipient(Recipient.get(db, cursor.getLong(cursor.getColumnIndex("recipient_id"))));
            sendingRequest.setRecording(Recording.get(db, cursor.getLong(cursor.getColumnIndex("recording_id"))));
        }
        sendingRequest.setRegistrationTimestamp(cursor.getString(cursor.getColumnIndex("registration_ts")));
        sendingRequest.setSent(cursor.getInt(cursor.getColumnIndex("sent")) > 0);
        return sendingRequest;
    }

    /**
     * Inserts the supplied sending request into the supplied local database.
     *
     * @param db the local database connection
     * @param sendingRequest the sending request
     * @throws SQLException
     */
    public static void insert(SQLiteDatabase db, SendingRequest sendingRequest) throws SQLException {
        Recipient.insert(db, sendingRequest.getRecipient());
        Recording.insert(db, sendingRequest.getRecording());

        ContentValues cv = new ContentValues();
        cv.put("sending_request_uuid", sendingRequest.getId().toString());
        cv.put("subject", sendingRequest.getSubject());
        cv.put("body", sendingRequest.getBody());
        cv.put("recording_id", sendingRequest.getRecording().getId());
        cv.put("recipient_id", sendingRequest.getRecipient().getId());
        cv.put("registration_ts", sendingRequest.getRegistrationTimestamp());
        cv.put("sent", sendingRequest.isSent() ? 1 : 0);

        long id = db.insert("tbl_sending_request", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert sending request!");
        }
    }

    /**
     * Updates the supplied sending request data (UUID must be supplied).
     * An SQLException is thrown if the sending request UUID does not exist in the database.
     *
     * @param db the local database connection
     * @param sendingRequest the sending request
     * @throws SQLException
     */
    public static void update(SQLiteDatabase db, SendingRequest sendingRequest) throws SQLException {
        Recipient.update(db, sendingRequest.getRecipient());
        Recording.update(db, sendingRequest.getRecording());

        ContentValues cv = new ContentValues();
        //cv.put("sending_request_uuid", sendingRequest.getId().toString());
        cv.put("subject", sendingRequest.getSubject());
        cv.put("body", sendingRequest.getBody());
        cv.put("recording_id", sendingRequest.getRecording().getId());
        cv.put("recipient_id", sendingRequest.getRecipient().getId());
        cv.put("sent", sendingRequest.isSent() ? 1 : 0);

        long id = db.update("tbl_sending_request", cv, "sending_request_uuid = ?", new String[]{sendingRequest.getId().toString()});
        if(id < 0) {
            throw new SQLException("Unable to update sending request!");
        }
    }

    /**
     * If a sending request UUID is supplied it performs an update, otherwise, it inserts the sending request.
     *
     * @param db the local database connection
     * @param sendingRequest the sending request
     * @throws SQLException
     */
    public static void insertOrUpdate(SQLiteDatabase db, SendingRequest sendingRequest) throws  SQLException {
        if(get(db, sendingRequest.getId()) == null) {
            insert(db, sendingRequest);
            return;
        }
        update(db, sendingRequest);
    }

    /**
     * Obtains the sending request cursor with the supplied UUID from the database.
     *
     * @param db the local database connection
     * @param uuid the sending request UUID
     * @return the cursor instance with all data
     */
    public static Cursor getCursor(SQLiteDatabase db, UUID uuid) {
        return db.rawQuery("SELECT * FROM tbl_sending_request WHERE sending_request_uuid = ?;", new String[]{uuid.toString()});
    }

    /**
     * Obtains the sending request with the supplied UUID from the database.
     *
     * @param db the local database connection
     * @param uuid the sending request UUID
     * @return the sending request instance with all data
     */
    public static SendingRequest get(SQLiteDatabase db, UUID uuid) {
        SendingRequest sendingRequest = null;
        Cursor cursor = getCursor(db, uuid);
        if(cursor != null && cursor.moveToFirst()) {
            sendingRequest = getFromCursor(db, cursor);
        }
        return sendingRequest;
    }

    /**
     * Obtains the cursor with all queued sending requests stored in the local database.
     *
     * @param db the local database connection
     * @return the cursor with all queued sending requests
     */
    public static Cursor getQueuedCursor(SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM tbl_sending_request WHERE sent <= 0 ORDER BY registration_ts ASC", null);
    }

    /**
     * Obtains a list with all queued sending requests stored in the local database.
     *
     * @param db the local database connection
     * @return the list of all queued sending requests
     */
    public static List<SendingRequest> getQueued(SQLiteDatabase db) {
        List<SendingRequest> list = new ArrayList<>();
        Cursor cursor = getQueuedCursor(db);
        if(cursor != null && cursor.moveToFirst()) {
            do {
                list.add(getFromCursor(db, cursor));
            } while(cursor.moveToNext());
        }
        return list;
    }

    private UUID mId = UUID.randomUUID();

    private Recording mRecording;
    private Recipient mRecipient;

    private String mSubject;            // only used for email
    private String mBody;
    private String mRegistrationTimestamp = Utils.getCurrentTimestamp();
    private boolean mSent = false;

    public SendingRequest() {
    }

    public SendingRequest(Recording recording, Recipient recipient) {
        this.mRecipient = recipient;
        this.mRecording = recording;
    }

    public SendingRequest(Recording recording, Recipient recipient, String subject, String body) {
        this.mRecipient = recipient;
        this.mRecording = recording;
        this.mSubject = subject;
        this.mBody = body;
    }

    public Recording getRecording() {
        return mRecording;
    }

    public void setRecording(Recording mRecording) {
        this.mRecording = mRecording;
    }

    public Recipient getRecipient() {
        return mRecipient;
    }

    public void setRecipient(Recipient mRecipient) {
        this.mRecipient = mRecipient;
    }

    public String getSubject() {
        return mSubject;
    }

    public void setSubject(String mSubject) {
        this.mSubject = mSubject;
    }

    public String getBody() {
        return mBody;
    }

    public void setBody(String mBody) {
        this.mBody = mBody;
    }

    public UUID getId() {
        return mId;
    }

    public void setId(UUID mId) {
        this.mId = mId;
    }

    public String getRegistrationTimestamp() {
        return mRegistrationTimestamp;
    }

    public void setRegistrationTimestamp(String mRegistrationTimestamp) {
        this.mRegistrationTimestamp = mRegistrationTimestamp;
    }

    public boolean isSent() {
        return mSent;
    }

    public void setSent(boolean mSent) {
        this.mSent = mSent;
    }
}
