package com.peppermint.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.io.Serializable;
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
public class Message implements Serializable {

    /**
     * Gets the message data inside the Cursor's current position and puts it in an instance
     * of the Message structure.
     *
     * @param cursor the cursor
     * @return the Message instance
     */
    public static Message getFromCursor(SQLiteDatabase db, Cursor cursor) {
        Message message = new Message();
        message.setId(cursor.getLong(cursor.getColumnIndex("message_id")));

        message.setEmailSubject(cursor.getString(cursor.getColumnIndex("email_subject")));
        message.setEmailBody(cursor.getString(cursor.getColumnIndex("email_body")));

        if(db != null) {
            if(!cursor.isNull(cursor.getColumnIndex("chat_id"))) {
                message.setChat(Chat.get(db, cursor.getLong(cursor.getColumnIndex("chat_id"))));
            }
            if(!cursor.isNull(cursor.getColumnIndex("recipient_id"))) {
                message.setRecipient(Recipient.get(db, cursor.getLong(cursor.getColumnIndex("recipient_id"))));
            }
            if(!cursor.isNull(cursor.getColumnIndex("recording_id"))) {
                message.setRecording(Recording.get(db, cursor.getLong(cursor.getColumnIndex("recording_id"))));
            }
        }

        message.setServerId(cursor.getString(cursor.getColumnIndex("server_message_id")));
        message.setServerShortUrl(cursor.getString(cursor.getColumnIndex("server_short_url")));
        message.setServerCanonicalUrl(cursor.getString(cursor.getColumnIndex("server_canonical_url")));
        message.setServerTranscriptionUrl(cursor.getString(cursor.getColumnIndex("server_transcription_url")));

        message.setRegistrationTimestamp(cursor.getString(cursor.getColumnIndex("registration_ts")));
        message.setSent(cursor.getInt(cursor.getColumnIndex("sent")) > 0);
        message.setReceived(cursor.getInt(cursor.getColumnIndex("received")) > 0);
        message.setPlayed(cursor.getInt(cursor.getColumnIndex("played")) > 0);

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
        ContentValues cv = new ContentValues();

        cv.put("email_subject", message.getEmailSubject());
        cv.put("email_body", message.getEmailBody());

        if(message.getChat() != null) {
            cv.put("chat_id", message.getChat().getId());
        } else {
            cv.putNull("chat_id");
        }
        if(message.getRecipient() != null) {
            cv.put("recipient_id", message.getRecipient().getId());
        } else {
            cv.putNull("recipient_id");
        }
        if(message.getRecording() != null) {
            cv.put("recording_id", message.getRecording().getId());
        } else {
            cv.putNull("recording_id");
        }

        cv.put("server_message_id", message.getServerId());
        cv.put("server_short_url", message.getServerShortUrl());
        cv.put("server_canonical_url", message.getServerCanonicalUrl());
        cv.put("server_transcription_url", message.getServerTranscriptionUrl());
        cv.put("registration_ts", message.getRegistrationTimestamp());

        cv.put("sent", message.isSent() ? 1 : 0);
        cv.put("received", message.isReceived() ? 1 : 0);
        cv.put("played", message.isPlayed() ? 1 : 0);

        long id = db.insert("tbl_message", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert message!");
        }

        message.setId(id);
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
        ContentValues cv = new ContentValues();

        cv.put("email_subject", message.getEmailSubject());
        cv.put("email_body", message.getEmailBody());

        if(message.getChat() != null) {
            cv.put("chat_id", message.getChat().getId());
        } else {
            cv.putNull("chat_id");
        }
        if(message.getRecipient() != null) {
            cv.put("recipient_id", message.getRecipient().getId());
        } else {
            cv.putNull("recipient_id");
        }
        if(message.getRecording() != null) {
            cv.put("recording_id", message.getRecording().getId());
        } else {
            cv.putNull("recording_id");
        }

        cv.put("server_message_id", message.getServerId());
        cv.put("server_short_url", message.getServerShortUrl());
        cv.put("server_canonical_url", message.getServerCanonicalUrl());
        cv.put("server_transcription_url", message.getServerTranscriptionUrl());

        cv.put("sent", message.isSent() ? 1 : 0);
        cv.put("received", message.isReceived() ? 1 : 0);
        cv.put("played", message.isPlayed() ? 1 : 0);

        long id = db.update("tbl_message", cv, "message_id = " + message.getId(), null);
        if(id < 0) {
            throw new SQLException("Unable to update message!");
        }
        return id;
    }

    /**
     * If a message id is supplied it performs an update, otherwise, it inserts the message.
     *
     * @param db the local database connection
     * @param message the message
     * @throws SQLException
     */
    public static long insertOrUpdate(SQLiteDatabase db, Message message) throws  SQLException {

        Message foundMessage = null;
        if(message.getId() > 0 || message.getServerId() != null) {
            foundMessage = getByIdOrServerId(db, message.getId(), message.getServerId());
        }

        if(foundMessage == null) {
            return insert(db, message);
        }
        message.setId(foundMessage.getId());
        return update(db, message);
    }

    /**
     * Deletes the supplied message data (id must be supplied).
     * An SQLException is thrown if the message id does not exist in the database.
     *
     * @param db the local database connection
     * @param id the message id
     * @throws SQLException
     */
    public static long delete(SQLiteDatabase db, long id) throws SQLException {
        long retId = db.delete("tbl_message", "message_id = " + id, null);
        if(retId < 0) {
            throw new SQLException("Unable to delete message!");
        }
        return retId;
    }

    public static void deleteByChat(SQLiteDatabase db, long chatId) throws SQLException {
        long id = db.delete("tbl_message", "chat_id = " + chatId, null);
        if(id < 0) {
            throw new SQLException("Unable to delete messages!");
        }
    }

    /**
     * Obtains the message cursor with the supplied id or server id from the database.
     *
     * @param db the local database connection
     * @param id the message id
     * @param serverId the message server id
     * @return the cursor instance with all data
     */
    public static Cursor getCursorByIdOrServerId(SQLiteDatabase db, long id, String serverId) {
        String where = Utils.joinString(" OR ", id > 0 ? "message_id = " + id : null, serverId != null ? "server_message_id = ?" : null);
        return db.rawQuery("SELECT * FROM tbl_message WHERE " + where + ";", serverId != null ? new String[]{serverId} : null);
    }

    /**
     * Obtains the message with the supplied id or server id from the database.
     *
     * @param db the local database connection
     * @param id the message id
     * @param serverId the message server id
     * @return the message instance with all data
     */
    public static Message getByIdOrServerId(SQLiteDatabase db, long id, String serverId) {
        Message message = null;
        Cursor cursor = getCursorByIdOrServerId(db, id, serverId);
        if(cursor.moveToFirst()) {
            message = getFromCursor(db, cursor);
        }
        cursor.close();
        return message;
    }

    public static Cursor getByChatCursor(SQLiteDatabase db, long chatId) {
        return db.rawQuery("SELECT tbl_message.*,tbl_message.message_id AS _id FROM tbl_message WHERE chat_id = " + chatId + " ORDER BY registration_ts ASC", null);
    }

    /**
     * Obtains the cursor with all queued messages stored in the local database.
     *
     * @param db the local database connection
     * @return the cursor with all queued messages
     */
    public static Cursor getQueuedCursor(SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM tbl_message WHERE sent <= 0 AND received <= 0 ORDER BY registration_ts ASC", null);
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
        if(cursor.moveToFirst()) {
            do {
                list.add(getFromCursor(db, cursor));
            } while(cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public static int getUnopenedCountByChat(SQLiteDatabase db, long chatId) {
        int count = 0;
        Cursor cursor = db.rawQuery("select count(*) from tbl_message where played <= 0 AND received >= 1 AND chat_id = " + chatId, null);
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    private UUID mUUID = UUID.randomUUID();
    private long mId;

    private Chat mChat;
    private Recording mRecording;
    private Recipient mRecipient;

    private String mEmailSubject;
    private String mEmailBody;

    private String mRegistrationTimestamp = DateContainer.getCurrentUTCTimestamp();
    private boolean mSent = false;
    private boolean mReceived = false;
    private boolean mPlayed = false;

    private String mServerId;
    private String mServerCanonicalUrl, mServerShortUrl, mServerTranscriptionUrl;

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

    public Message(long mId, Chat mChat, Recording mRecording, Recipient mRecipient, String mEmailSubject, String mEmailBody, String mRegistrationTimestamp, boolean mReceived, boolean mSent, boolean mPlayed, String mServerId, String mServerCanonicalUrl, String mServerShortUrl, String mServerTranscriptionUrl) {
        this.mId = mId;
        this.mChat = mChat;
        this.mRecording = mRecording;
        this.mRecipient = mRecipient;
        this.mEmailSubject = mEmailSubject;
        this.mEmailBody = mEmailBody;
        this.mRegistrationTimestamp = mRegistrationTimestamp;
        this.mReceived = mReceived;
        this.mSent = mSent;
        this.mPlayed = mPlayed;
        this.mServerId = mServerId;
        this.mServerCanonicalUrl = mServerCanonicalUrl;
        this.mServerShortUrl = mServerShortUrl;
        this.mServerTranscriptionUrl = mServerTranscriptionUrl;
    }

    public boolean isPlayed() {
        return mPlayed;
    }

    public void setPlayed(boolean mPlayed) {
        this.mPlayed = mPlayed;
    }

    public boolean isReceived() {
        return mReceived;
    }

    public void setReceived(boolean mReceived) {
        this.mReceived = mReceived;
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

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
        this.mUUID = new UUID(mId, mId);
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

    public String getServerTranscriptionUrl() {
        return mServerTranscriptionUrl;
    }

    public void setServerTranscriptionUrl(String mServerTranscriptionUrl) {
        this.mServerTranscriptionUrl = mServerTranscriptionUrl;
    }

    public String getServerId() {
        return mServerId;
    }

    public void setServerId(String mServerId) {
        this.mServerId = mServerId;
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

    public UUID getUUID() {
        return mUUID;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Message) {
            return ((Message) o).getUUID().equals(mUUID);
        }
        return super.equals(o);
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
                ", mReceived=" + mReceived +
                ", mSent=" + mSent +
                ", mPlayed=" + mPlayed +
                ", mServerId='" + mServerId + '\'' +
                ", mServerCanonicalUrl='" + mServerCanonicalUrl + '\'' +
                ", mServerShortUrl='" + mServerShortUrl + '\'' +
                ", mServerTranscriptionUrl='" + mServerTranscriptionUrl + '\'' +
                ", mParameters=" + mParameters +
                '}';
    }
}
