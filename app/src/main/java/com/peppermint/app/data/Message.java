package com.peppermint.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an audio message from/to a particular recipient.
 */
public class Message {

    /**
     * Gets the message data inside the Cursor's current position and puts it in an instance
     * of the Message structure.
     *
     * @param cursor the cursor
     * @return the Message instance
     */
    public static Message getFromCursor(SQLiteDatabase db, Cursor cursor) {
        Message message = new Message();
        message.setId(UUID.fromString(cursor.getString(cursor.getColumnIndex("message_uuid"))));
        message.setEmailSubject(cursor.getString(cursor.getColumnIndex("email_subject")));
        message.setEmailBody(cursor.getString(cursor.getColumnIndex("email_body")));
        message.setServerShortUrl(cursor.getString(cursor.getColumnIndex("short_url")));
        message.setServerCanonicalUrl(cursor.getString(cursor.getColumnIndex("canonical_url")));
        if(db != null) {
            message.setChat(Chat.get(db, cursor.getLong(cursor.getColumnIndex("chat_id"))));
            message.setRecipient(Recipient.get(db, cursor.getLong(cursor.getColumnIndex("recipient_id"))));
            message.setRecording(Recording.get(db, cursor.getLong(cursor.getColumnIndex("recording_id"))));
        }
        message.setRegistrationTimestamp(cursor.getString(cursor.getColumnIndex("registration_ts")));
        message.setSent(cursor.getInt(cursor.getColumnIndex("sent")) > 0);
        return message;
    }

    /**
     * Inserts the supplied message into the supplied local database.
     *
     * @param db the local database connection
     * @param message the message
     * @throws SQLException
     */
    public static long insert(SQLiteDatabase db, Message message) throws SQLException {
        Chat.insertOrUpdate(db, message.getChat());
        Recipient.insert(db, message.getRecipient());
        Recording.insert(db, message.getRecording());

        ContentValues cv = new ContentValues();
        cv.put("message_uuid", message.getId().toString());
        cv.put("email_subject", message.getEmailSubject());
        cv.put("email_body", message.getEmailBody());
        if(message.getRecording() != null) {
            cv.put("recording_id", message.getRecording().getId());
        } else {
            cv.putNull("recording_id");
        }
        cv.put("recipient_id", message.getRecipient().getId());
        cv.put("chat_id", message.getChat().getId());
        cv.put("registration_ts", message.getRegistrationTimestamp());
        cv.put("sent", message.isSent() ? 1 : 0);
        cv.put("short_url", message.getServerShortUrl());
        cv.put("canonical_url", message.getServerCanonicalUrl());

        long id = db.insert("tbl_message", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert message!");
        }
        return id;
    }

    /**
     * Updates the supplied message data (UUID must be supplied).
     * An SQLException is thrown if the message UUID does not exist in the database.
     *
     * @param db the local database connection
     * @param message the message
     * @throws SQLException
     */
    public static long update(SQLiteDatabase db, Message message) throws SQLException {
        Chat.insertOrUpdate(db, message.getChat());
        Recipient.update(db, message.getRecipient());
        Recording.update(db, message.getRecording());

        ContentValues cv = new ContentValues();
        //cv.put("sending_request_uuid", message.getId().toString());
        cv.put("email_subject", message.getEmailSubject());
        cv.put("email_body", message.getEmailBody());
        if(message.getRecording() != null) {
            cv.put("recording_id", message.getRecording().getId());
        } else {
            cv.putNull("recording_id");
        }
        cv.put("recipient_id", message.getRecipient().getId());
        cv.put("chat_id", message.getChat().getId());
        cv.put("short_url", message.getServerShortUrl());
        cv.put("canonical_url", message.getServerCanonicalUrl());
        cv.put("sent", message.isSent() ? 1 : 0);

        long id = db.update("tbl_message", cv, "message_uuid = ?", new String[]{message.getId().toString()});
        if(id < 0) {
            throw new SQLException("Unable to update message!");
        }
        return id;
    }

    /**
     * If a message UUID is supplied it performs an update, otherwise, it inserts the sending request.
     *
     * @param db the local database connection
     * @param message the sending request
     * @throws SQLException
     */
    public static long insertOrUpdate(SQLiteDatabase db, Message message) throws  SQLException {
        if(get(db, message.getId()) == null) {
            return insert(db, message);
        }
        return update(db, message);
    }

    /**
     * Deletes the supplied message data (UUID must be supplied).
     * An SQLException is thrown if the message UUID does not exist in the database.
     *
     * @param db the local database connection
     * @param message the message
     * @throws SQLException
     */
    public static long delete(SQLiteDatabase db, Message message) throws SQLException {
        Recipient.delete(db, message.getRecipient());
        Recording.delete(db, message.getRecording());

        long id = db.delete("tbl_message", "message_uuid = ?", new String[]{message.getId().toString()});
        if(id < 0) {
            throw new SQLException("Unable to delete message!");
        }
        return id;
    }

    public static void deleteByChat(SQLiteDatabase db, long chatId) throws SQLException {
        long id = db.delete("tbl_message", "chat_id = " + chatId, null);
        if(id < 0) {
            throw new SQLException("Unable to delete messages!");
        }
    }

    /**
     * Obtains the message cursor with the supplied UUID from the database.
     *
     * @param db the local database connection
     * @param uuid the message UUID
     * @return the cursor instance with all data
     */
    public static Cursor getCursor(SQLiteDatabase db, UUID uuid) {
        return db.rawQuery("SELECT * FROM tbl_message WHERE message_uuid = ?;", new String[]{uuid.toString()});
    }

    /**
     * Obtains the message with the supplied UUID from the database.
     *
     * @param db the local database connection
     * @param uuid the message UUID
     * @return the message instance with all data
     */
    public static Message get(SQLiteDatabase db, UUID uuid) {
        Message message = null;
        Cursor cursor = getCursor(db, uuid);
        if(cursor != null && cursor.moveToFirst()) {
            message = getFromCursor(db, cursor);
        }
        return message;
    }

    /**
     * Obtains the cursor with all queued messages stored in the local database.
     *
     * @param db the local database connection
     * @return the cursor with all queued messages
     */
    public static Cursor getQueuedCursor(SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM tbl_message WHERE sent <= 0 AND recording_id is not null ORDER BY registration_ts ASC", null);
    }

    /**
     * Obtains a list with all queued messages stored in the local database.
     *
     * @param db the local database connection
     * @return the list of all queued messages
     */
    public static List<Message> getQueued(SQLiteDatabase db) {
        List<Message> list = new ArrayList<>();
        Cursor cursor = getQueuedCursor(db);
        if(cursor != null && cursor.moveToFirst()) {
            do {
                list.add(getFromCursor(db, cursor));
            } while(cursor.moveToNext());
        }
        return list;
    }

    private UUID mId = UUID.randomUUID();

    private Chat mChat;
    private Recording mRecording;
    private Recipient mRecipient;

    private String mEmailSubject;
    private String mEmailBody;
    private String mRegistrationTimestamp = Utils.getCurrentTimestamp();
    private boolean mSent = false;

    private String mServerCanonicalUrl, mServerShortUrl;

    // extra parameters about the message that can be stored by/feed to senders
    private Map<String, Object> mParameters = new HashMap<>();

    public Message() {
    }

    public Message(Recording recording, Recipient recipient) {
        this.mRecipient = recipient;
        this.mRecording = recording;
    }

    public Message(Recording recording, Recipient recipient, String emailSubject, String emailBody) {
        this.mRecipient = recipient;
        this.mRecording = recording;
        this.mEmailSubject = emailSubject;
        this.mEmailBody = emailBody;
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

    public String getEmailSubject() {
        return mEmailSubject;
    }

    public void setEmailSubject(String mEmailSubject) {
        this.mEmailSubject = mEmailSubject;
    }

    public String getEmailBody() {
        return mEmailBody;
    }

    public void setEmailBody(String mEmailBody) {
        this.mEmailBody = mEmailBody;
    }

    public Chat getChat() {
        return mChat;
    }

    public void setChat(Chat mChat) {
        this.mChat = mChat;
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

    public Map<String, Object> getParameters() {
        return mParameters;
    }

    public Message setParameters(Map<String, Object> mParameters) {
        this.mParameters = mParameters;
        return this;
    }

    public Object getParameter(String key) {
        if(!mParameters.containsKey(key)) {
            return null;
        }
        return mParameters.get(key);
    }

    public Message setParameter(String key, Object value) {
        mParameters.put(key, value);
        return this;
    }

    public String getServerShortUrl() {
        return mServerShortUrl;
    }

    public void setServerShortUrl(String mServerShortUrl) {
        this.mServerShortUrl = mServerShortUrl;
    }

    public String getServerCanonicalUrl() {
        return mServerCanonicalUrl;
    }

    public void setServerCanonicalUrl(String mServerCanonicalUrl) {
        this.mServerCanonicalUrl = mServerCanonicalUrl;
    }

    @Override
    public String toString() {
        return "Message{" +
                "mId=" + mId +
                ", mChat=" + mChat +
                ", mRecording=" + mRecording +
                ", mRecipient=" + mRecipient +
                ", mEmailSubject='" + mEmailSubject + '\'' +
                ", mEmailBody='" + mEmailBody + '\'' +
                ", mRegistrationTimestamp='" + mRegistrationTimestamp + '\'' +
                ", mSent=" + mSent +
                ", mServerCanonicalUrl='" + mServerCanonicalUrl + '\'' +
                ", mServerShortUrl='" + mServerShortUrl + '\'' +
                ", mParameters=" + mParameters +
                '}';
    }
}
