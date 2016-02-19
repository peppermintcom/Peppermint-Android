package com.peppermint.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Database operations for {@link Message}
 */
public class MessageManager {

    /**
     * Gets the message data inside the Cursor's current position and puts it in an instance
     * of the Message structure.<br />
     * <strong>If context is supplied, the recipient will be retrieved and available through {@link Message#getRecipientParameter()}<br />
     * The db is supplied, the recording will be available through {@link Message#getRecordingParameter()}</strong>
     *
     * @param context the app context (optional)
     * @param db the local database connection (optional)
     * @param cursor the cursor
     * @return the Message instance
     */
    public static Message getFromCursor(Context context, SQLiteDatabase db, Cursor cursor) {
        Message message = new Message();
        message.setId(cursor.getLong(cursor.getColumnIndex("message_id")));

        message.setEmailSubject(cursor.getString(cursor.getColumnIndex("email_subject")));
        message.setEmailBody(cursor.getString(cursor.getColumnIndex("email_body")));

        message.setChatId(cursor.getLong(cursor.getColumnIndex("chat_id")));
        message.setRecipientContactId(cursor.getLong(cursor.getColumnIndex("recipient_id")));
        message.setRecordingId(cursor.getLong(cursor.getColumnIndex("recording_id")));

        message.setServerId(cursor.getString(cursor.getColumnIndex("server_message_id")));
        message.setServerShortUrl(cursor.getString(cursor.getColumnIndex("server_short_url")));
        message.setServerCanonicalUrl(cursor.getString(cursor.getColumnIndex("server_canonical_url")));
        message.setServerTranscriptionUrl(cursor.getString(cursor.getColumnIndex("server_transcription_url")));

        message.setRegistrationTimestamp(cursor.getString(cursor.getColumnIndex("registration_ts")));
        message.setSent(cursor.getInt(cursor.getColumnIndex("sent")) > 0);
        message.setReceived(cursor.getInt(cursor.getColumnIndex("received")) > 0);
        message.setPlayed(cursor.getInt(cursor.getColumnIndex("played")) > 0);

        if(db != null) {
            message.setRecordingParameter(RecordingManager.getRecordingById(db, message.getRecordingId()));
        }
        if(context != null) {
            message.setRecipientParameter(RecipientManager.getRecipientByContactId(context, message.getRecipientContactId()));
        }

        return message;
    }

    /**
     * Obtains the message cursor with the supplied id or server id from the database.
     *
     * @param db the local database connection
     * @param messageId the message id
     * @param serverId the message server id
     * @return the cursor instance with all data
     */
    public static Cursor getByIdOrServerId(SQLiteDatabase db, long messageId, String serverId) {
        String where = Utils.joinString(" OR ", messageId > 0 ? "message_id = " + messageId : null, serverId != null ? "server_message_id = ?" : null);
        return db.rawQuery("SELECT * FROM tbl_message WHERE " + where + ";", serverId != null ? new String[]{serverId} : null);
    }

    /**
     * Obtains the message with the supplied id or server id from the database.
     *
     * @param context the app context (optional)
     * @param db the local database connection
     * @param messageId the message id
     * @param serverId the message server id
     * @return the message instance with all data
     */
    public static Message getMessageByIdOrServerId(Context context, SQLiteDatabase db, long messageId, String serverId) {
        Message message = null;
        Cursor cursor = getByIdOrServerId(db, messageId, serverId);
        if(cursor.moveToFirst()) {
            message = getFromCursor(context, db, cursor);
        }
        cursor.close();
        return message;
    }

    public static Cursor getByChatId(SQLiteDatabase db, long chatId) {
        return db.rawQuery("SELECT tbl_message.*,tbl_message.message_id AS _id FROM tbl_message WHERE chat_id = " + chatId + " ORDER BY registration_ts ASC", null);
    }

    /**
     * Obtains the cursor with all queued messages stored in the local database.
     *
     * @param db the local database connection
     * @return the cursor with all queued messages
     */
    public static Cursor getQueued(SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM tbl_message WHERE sent <= 0 AND received <= 0 ORDER BY registration_ts ASC", null);
    }

    /**
     * Obtains a list with all queued messages stored in the local database.
     *
     * @param context the app context (optional)
     * @param db the local database connection
     * @return the list of all queued messages
     */
    public static List<Message> getMessagesQueued(Context context, SQLiteDatabase db) {
        List<Message> list = new ArrayList<>();
        Cursor cursor = getQueued(db);
        if(cursor.moveToFirst()) {
            do {
                Message message = getFromCursor(context, db, cursor);
                message.setRecordingParameter(RecordingManager.getRecordingById(db, message.getRecordingId()));
                if(context != null) {
                    message.setRecipientParameter(RecipientManager.getRecipientByContactId(context, message.getRecipientContactId()));
                }
                list.add(message);
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

    /**
     * Inserts the supplied message into the supplied local database.
     *
     * @param db the local database connection
     * @param chatId the message's chat id
     * @param recipientContactId the message's recipient contact id
     * @param recordingId the message's recording id
     * @param serverId the message's server id
     * @param shortUrl the message's short URL
     * @param canonicalUrl the message's canonical URL
     * @param transcriptionUrl the message's transcription URL
     * @param emailSubject the message's email subject
     * @param emailBody the message's email body
     * @param registrationTimestamp the message's registration timestamp
     * @param sent if the message has been sent
     * @param received if the message has been received
     * @param played if the message has been played
     * @throws SQLException
     */
    public static Message insert(SQLiteDatabase db, long chatId, long recipientContactId, long recordingId,
                                 String serverId, String shortUrl, String canonicalUrl, String transcriptionUrl,
                                 String emailSubject, String emailBody,
                                 String registrationTimestamp,
                                 boolean sent, boolean received, boolean played) throws SQLException {
        ContentValues cv = new ContentValues();

        cv.put("email_subject", emailSubject);
        cv.put("email_body", emailBody);

        if(chatId > 0) {
            cv.put("chat_id", chatId);
        } else {
            cv.putNull("chat_id");
        }
        if(recipientContactId > 0) {
            cv.put("recipient_id", recipientContactId);
        } else {
            cv.putNull("recipient_id");
        }
        if(recordingId > 0) {
            cv.put("recording_id", recordingId);
        } else {
            cv.putNull("recording_id");
        }

        cv.put("server_message_id", serverId);
        cv.put("server_short_url", shortUrl);
        cv.put("server_canonical_url", canonicalUrl);
        cv.put("server_transcription_url", transcriptionUrl);
        cv.put("registration_ts", registrationTimestamp);

        cv.put("sent", sent ? 1 : 0);
        cv.put("received", received ? 1 : 0);
        cv.put("played", played ? 1 : 0);

        long id = db.insert("tbl_message", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert message!");
        }

        return new Message(id, chatId, recordingId, recipientContactId, emailSubject, emailBody,
                registrationTimestamp, received, sent, played, serverId, canonicalUrl,
                shortUrl, transcriptionUrl);
    }

    /**
     * Updates the supplied message data (UUID must be supplied).
     * An SQLException is thrown if the message UUID does not exist in the database.
     *
     * @param db the local database connection
     * @param messageId the message id
     * @param chatId the message's chat id
     * @param recipientContactId the message's recipient contact id
     * @param recordingId the message's recording id
     * @param serverId the message's server id
     * @param shortUrl the message's short URL
     * @param canonicalUrl the message's canonical URL
     * @param transcriptionUrl the message's transcription URL
     * @param emailSubject the message's email subject
     * @param emailBody the message's email body
     * @param registrationTimestamp the message's registration timestamp
     * @param sent if the message has been sent
     * @param received if the message has been received
     * @param played if the message has been played
     * @throws SQLException
     */
    public static Message update(SQLiteDatabase db, long messageId, long chatId, long recipientContactId, long recordingId,
                              String serverId, String shortUrl, String canonicalUrl, String transcriptionUrl,
                              String emailSubject, String emailBody,
                              String registrationTimestamp,
                              boolean sent, boolean received, boolean played) throws SQLException {
        ContentValues cv = new ContentValues();

        cv.put("email_subject", emailSubject);
        cv.put("email_body", emailBody);

        if(chatId > 0) {
            cv.put("chat_id", chatId);
        } else {
            cv.putNull("chat_id");
        }
        if(recipientContactId > 0) {
            cv.put("recipient_id", recipientContactId);
        } else {
            cv.putNull("recipient_id");
        }
        if(recordingId > 0) {
            cv.put("recording_id", recordingId);
        } else {
            cv.putNull("recording_id");
        }

        cv.put("server_message_id", serverId);
        cv.put("server_short_url", shortUrl);
        cv.put("server_canonical_url", canonicalUrl);
        cv.put("server_transcription_url", transcriptionUrl);
        cv.put("registration_ts", registrationTimestamp);

        cv.put("sent", sent ? 1 : 0);
        cv.put("received", received ? 1 : 0);
        cv.put("played", played ? 1 : 0);

        long id = db.update("tbl_message", cv, "message_id = " + messageId, null);
        if(id < 0) {
            throw new SQLException("Unable to update message!");
        }

        return new Message(messageId, chatId, recordingId, recipientContactId, emailSubject, emailBody,
                registrationTimestamp, received, sent, played, serverId, canonicalUrl,
                shortUrl, transcriptionUrl);
    }

    /**
     * If a message id is supplied it performs an update, otherwise, it inserts the message.
     *
     * @param db the local database connection
     * @param messageId the message id
     * @param chatId the message's chat id
     * @param recipientContactId the message's recipient contact id
     * @param recordingId the message's recording id
     * @param serverId the message's server id
     * @param shortUrl the message's short URL
     * @param canonicalUrl the message's canonical URL
     * @param transcriptionUrl the message's transcription URL
     * @param emailSubject the message's email subject
     * @param emailBody the message's email body
     * @param registrationTimestamp the message's registration timestamp
     * @param sent if the message has been sent
     * @param received if the message has been received
     * @param played if the message has been played
     * @throws SQLException
     */
    public static Message insertOrUpdate(SQLiteDatabase db, long messageId, long chatId, long recipientContactId, long recordingId,
                                      String serverId, String shortUrl, String canonicalUrl, String transcriptionUrl,
                                      String emailSubject, String emailBody,
                                      String registrationTimestamp,
                                      boolean sent, boolean received, boolean played) throws  SQLException {

        Message foundMessage = null;
        if(messageId > 0 || serverId != null) {
            foundMessage = getMessageByIdOrServerId(null, db, messageId, serverId);
        }

        if(foundMessage == null) {
            return insert(db, chatId, recipientContactId, recordingId, serverId, shortUrl, canonicalUrl,
                    transcriptionUrl, emailSubject, emailBody, registrationTimestamp, sent, received, played);
        }

        return update(db, foundMessage.getId(), chatId, recipientContactId, recordingId, serverId, shortUrl, canonicalUrl,
                transcriptionUrl, emailSubject, emailBody, registrationTimestamp, sent, received, played);
    }

    /**
     * Deletes the supplied message data (id must be supplied).
     * An SQLException is thrown if the message id does not exist in the database.
     *
     * @param db the local database connection
     * @param messageId the message id
     * @throws SQLException
     */
    public static long delete(SQLiteDatabase db, long messageId) throws SQLException {
        long retId = db.delete("tbl_message", "message_id = " + messageId, null);
        if(retId < 0) {
            throw new SQLException("Unable to delete message!");
        }
        return retId;
    }

    /**
     * Deletes all messages belonging to the chat with the supplied chat id.
     *
     * @param db the local database connection
     * @param chatId the chat id
     * @throws SQLException
     */
    public static void deleteByChat(SQLiteDatabase db, long chatId) throws SQLException {
        long id = db.delete("tbl_message", "chat_id = " + chatId, null);
        if(id < 0) {
            throw new SQLException("Unable to delete messages!");
        }
    }

}
