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
 */
public class MessageManager {

    /**
     * Gets the message data inside the Cursor's current position and puts it in an instance
     * of the Message structure.
     *
     * @param cursor the cursor
     * @return the Message instance
     */
    public static Message getFromCursor(Cursor cursor) {
        Message message = new Message();
        message.setId(cursor.getLong(cursor.getColumnIndex("message_id")));

        message.setEmailSubject(cursor.getString(cursor.getColumnIndex("email_subject")));
        message.setEmailBody(cursor.getString(cursor.getColumnIndex("email_body")));

        message.setChatId(cursor.getLong(cursor.getColumnIndex("chat_id")));
        message.setRecipientId(cursor.getLong(cursor.getColumnIndex("recipient_id")));
        message.setRecordingId(cursor.getLong(cursor.getColumnIndex("recording_id")));

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

        if(message.getChatId() > 0) {
            cv.put("chat_id", message.getChatId());
        } else {
            cv.putNull("chat_id");
        }
        if(message.getRecipientId() > 0) {
            cv.put("recipient_id", message.getRecipientId());
        } else {
            cv.putNull("recipient_id");
        }
        if(message.getRecordingId() > 0) {
            cv.put("recording_id", message.getRecordingId());
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

        if(message.getChatId() > 0) {
            cv.put("chat_id", message.getChatId());
        } else {
            cv.putNull("chat_id");
        }
        if(message.getRecipientId() > 0) {
            cv.put("recipient_id", message.getRecipientId());
        } else {
            cv.putNull("recipient_id");
        }
        if(message.getRecordingId() > 0) {
            cv.put("recording_id", message.getRecordingId());
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
    public static long insertOrUpdate(Context context, SQLiteDatabase db, Message message) throws  SQLException {

        Message foundMessage = null;
        if(message.getId() > 0 || message.getServerId() != null) {
            foundMessage = getByIdOrServerId(context, db, message.getId(), message.getServerId());
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
    public static Message getByIdOrServerId(Context context, SQLiteDatabase db, long id, String serverId) {
        Message message = null;
        Cursor cursor = getCursorByIdOrServerId(db, id, serverId);
        if(cursor.moveToFirst()) {
            message = getFromCursor(cursor);
            message.setRecordingParameter(RecordingManager.get(db, message.getRecordingId()));
            message.setRecipientParameter(RecipientManager.getRecipientById(context, message.getRecipientId()));
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
    public static List<Message> getQueued(Context context, SQLiteDatabase db) {
        List<Message> list = new ArrayList<>();
        Cursor cursor = getQueuedCursor(db);
        if(cursor.moveToFirst()) {
            do {
                Message message = getFromCursor(cursor);
                message.setRecordingParameter(RecordingManager.get(db, message.getRecordingId()));
                message.setRecipientParameter(RecipientManager.getRecipientById(context, message.getRecipientId()));
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

}
