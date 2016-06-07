package com.peppermint.app.dal.message;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.dal.DataObjectManager;
import com.peppermint.app.dal.chat.ChatManager;
import com.peppermint.app.dal.recording.RecordingManager;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Database operations for {@link Message}
 */
public class MessageManager extends DataObjectManager<Long, Message> {

    private static MessageManager INSTANCE;

    public static MessageManager getInstance(Context context) {
        if(INSTANCE == null) {
            INSTANCE = new MessageManager(context);
        }
        return INSTANCE;
    }

    protected MessageManager(Context context) {
        super(context);
    }

    // OPERATIONS
    private void deassociateRecipients(SQLiteDatabase db, long messageId) {
        db.delete("tbl_message_recipient", "message_id = " + messageId, null);
    }

    private void associateRecipients(SQLiteDatabase db, long messageId, List<Long> recipientIds, List<Long> confirmedSentRecipientIds) throws SQLException {
        if(recipientIds == null) {
            return;
        }

        for (long recipientId : recipientIds) {
            if(recipientId <= 0) {
                throw new IllegalArgumentException("Recipient Id " + recipientId + " is not valid!");
            }

            ContentValues cv = new ContentValues();
            cv.put("message_id", messageId);
            cv.put("recipient_id", recipientId);
            cv.put("sent", confirmedSentRecipientIds != null && confirmedSentRecipientIds.contains(recipientId) ? 1 : 0);

            long id = db.insert("tbl_message_recipient", null, cv);
            if (id < 0) {
                throw new SQLException("Unable to associate message with recipient!");
            }
        }
    }

    @Override
    protected Message doInsert(SQLiteDatabase db, Message message) throws SQLException {
        // setup content values
        ContentValues cv = new ContentValues();

        cv.put("email_subject", message.getEmailSubject());
        cv.put("email_body", message.getEmailBody());

        if(message.getChatId() > 0) {
            cv.put("chat_id", message.getChatId());
        }
        if(message.getAuthorId() > 0) {
            cv.put("author_id", message.getAuthorId());
        }
        if(message.getRecordingId() > 0) {
            cv.put("recording_id", message.getRecordingId());
        }

        cv.put("server_message_id", message.getServerId());
        cv.put("server_short_url", message.getServerShortUrl());
        cv.put("server_canonical_url", message.getServerCanonicalUrl());
        cv.put("registration_ts", message.getRegistrationTimestamp());

        cv.put("sent", message.isSent() ? 1 : 0);
        cv.put("received", message.isReceived() ? 1 : 0);
        cv.put("played", message.isPlayed() ? 1 : 0);

        long id = -1;
        db.beginTransaction();
        try {

            id = db.insert("tbl_message", null, cv);
            if (id < 0) {
                throw new SQLException("Unable to insert message!");
            }

            associateRecipients(db, id, message.getRecipientIds(), message.getConfirmedSentRecipientIds());

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        message.setId(id);
        message.setParameter(Message.PARAM_INSERTED, true);

        return message;
    }

    @Override
    protected void doUpdate(SQLiteDatabase db, Message message) throws SQLException {
        if(message.getId() <= 0) {
            throw new IllegalArgumentException("Message Id must be supplied!");
        }

        ContentValues cv = new ContentValues();

        cv.put("email_subject", message.getEmailSubject());
        cv.put("email_body", message.getEmailBody());

        if(message.getChatId() > 0) {
            cv.put("chat_id", message.getChatId());
        }
        if(message.getAuthorId() > 0) {
            cv.put("author_id", message.getAuthorId());
        }
        if(message.getRecordingId() > 0) {
            cv.put("recording_id", message.getRecordingId());
        }

        cv.put("server_message_id", message.getServerId());
        cv.put("server_short_url", message.getServerShortUrl());
        cv.put("server_canonical_url", message.getServerCanonicalUrl());
        cv.put("registration_ts", message.getRegistrationTimestamp());

        cv.put("sent", message.isSent() ? 1 : 0);
        cv.put("received", message.isReceived() ? 1 : 0);
        cv.put("played", message.isPlayed() ? 1 : 0);

        db.beginTransaction();
        try {
            long id = db.update("tbl_message", cv, "message_id = " + message.getId(), null);
            if (id < 0) {
                throw new SQLException("Unable to update message!");
            }

            deassociateRecipients(db, id);
            associateRecipients(db, id, message.getRecipientIds(), message.getConfirmedSentRecipientIds());

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    protected void doDelete(SQLiteDatabase db, Long messageId) throws SQLException {
        db.beginTransaction();
        try {
            long retId = db.delete("tbl_message", "message_id = " + messageId, null);
            if (retId < 0) {
                throw new SQLException("Unable to delete message!");
            }

            deassociateRecipients(db, messageId);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean exists(SQLiteDatabase db, Message dataObject) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Message newDataObjectInstance(Long id) {
        final Message message = new Message();
        message.setId(id);
        return message;
    }

    @Override
    protected Message doGetFromCursor(SQLiteDatabase db, Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex("message_id"));
        Message message = obtainCacheDataObject(id);

        message.setEmailSubject(cursor.getString(cursor.getColumnIndex("email_subject")));
        message.setEmailBody(cursor.getString(cursor.getColumnIndex("email_body")));

        message.setRecordingId(cursor.getLong(cursor.getColumnIndex("recording_id")));
        message.setAuthorId(cursor.getLong(cursor.getColumnIndex("author_id")));
        message.setChatId(cursor.getLong(cursor.getColumnIndex("chat_id")));

        message.setServerId(cursor.getString(cursor.getColumnIndex("server_message_id")));
        message.setServerShortUrl(cursor.getString(cursor.getColumnIndex("server_short_url")));
        message.setServerCanonicalUrl(cursor.getString(cursor.getColumnIndex("server_canonical_url")));

        message.setRegistrationTimestamp(cursor.getString(cursor.getColumnIndex("registration_ts")));
        message.setSent(cursor.getInt(cursor.getColumnIndex("sent")) > 0);
        message.setReceived(cursor.getInt(cursor.getColumnIndex("received")) > 0);
        message.setPlayed(cursor.getInt(cursor.getColumnIndex("played")) > 0);

        if(db != null) {
            message.setRecordingParameter(RecordingManager.getInstance().getRecordingById(db, message.getRecordingId()));
            message.setChatParameter(ChatManager.getInstance(mContext).getChatById(db, message.getChatId()));

            Cursor recipientIdCursor = db.rawQuery("SELECT * FROM tbl_message_recipient WHERE message_id = " + message.getId() + ";", null);
            while(recipientIdCursor.moveToNext()) {
                long recipientId = recipientIdCursor.getLong(recipientIdCursor.getColumnIndex("recipient_id"));
                boolean sent = recipientIdCursor.getInt(recipientIdCursor.getColumnIndex("sent")) > 0;
                message.addRecipientId(recipientId);
                if(sent) {
                    message.addConfirmedSentRecipientId(recipientId);
                }
            }
            recipientIdCursor.close();
        }

        return message;
    }

    public Cursor getByChatId(SQLiteDatabase db, long chatId) {
        return db.rawQuery("SELECT v_message.*, v_message.message_id AS _id FROM v_message WHERE v_message.merged_chat_id = " + chatId +
                " ORDER BY registration_ts ASC", null);
    }

    public List<Message> getMessagesQueued(SQLiteDatabase db) {
        List<Message> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM tbl_message WHERE sent <= 0 AND received <= 0 ORDER BY registration_ts ASC", null);
        if(cursor.moveToFirst()) {
            do {
                Message message = getFromCursor(db, cursor);
                list.add(message);
            } while(cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public Message getMessageByIdOrServerId(SQLiteDatabase db, long messageId, String serverId, Boolean isReceived) {
        Message message = null;

        String where = Utils.joinString(" OR ", messageId > 0 ? "message_id = " + messageId : null, serverId != null ? "server_message_id = ?" : null);
        where = Utils.joinString(" AND ", where, isReceived != null ? "received = " + (isReceived ? 1 : 0) : null);
        Cursor cursor = db.rawQuery("SELECT * FROM tbl_message WHERE " + where + ";", serverId != null ? new String[]{serverId} : null);
        if(cursor.moveToFirst()) {
            message = getFromCursor(db, cursor);
        }
        cursor.close();

        return message;
    }

    public int getUnopenedCountByChat(SQLiteDatabase db, long chatId) {
        int count = 0;

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM v_message WHERE (v_message.chat_id = " + chatId + " OR v_message.merged_chat_id = " + chatId +
            ") AND played <= 0 AND received >= 1", null);
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getUnopenedCount(SQLiteDatabase db) {
        int count = 0;

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM v_message WHERE " +
                "played <= 0 AND received >= 1", null);
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public void deleteByChat(SQLiteDatabase db, long chatId) throws SQLException {
        db.beginTransaction();

        try {
            db.execSQL("DELETE FROM tbl_message_recipient WHERE message_id IN (SELECT message_id FROM tbl_message WHERE chat_id = " + chatId + ")");

            long id = db.delete("tbl_message", "chat_id = " + chatId, null);
            if(id < 0) {
                throw new SQLException("Unable to delete messages!");
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

}
