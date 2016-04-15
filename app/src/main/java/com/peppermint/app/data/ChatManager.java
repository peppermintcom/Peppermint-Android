package com.peppermint.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Database operations for {@link Chat}
 */
public class ChatManager {

    private static final String TAG = ChatManager.class.getSimpleName();

    /**
     * Gets the {@link Chat} data inside the cursor's current position.<br />
     * <strong>If db is supplied, it will also load all {@link Recipient} data, which will be
     * accessible through {@link Chat#getRecipientList()}</strong>
     *
     * @param db the db connection (optional)
     * @param cursor the cursor
     * @return the Chat instance
     */
    public static Chat getChatFromCursor(SQLiteDatabase db, Cursor cursor) {
        Chat chat = new Chat();
        chat.setId(cursor.getLong(cursor.getColumnIndex("chat_id")));
        chat.setTitle(cursor.getString(cursor.getColumnIndex("title")));
        chat.setLastMessageTimestamp(cursor.getString(cursor.getColumnIndex("last_message_ts")));
        chat.setPeppermintChatId(cursor.getLong(cursor.getColumnIndex("peppermint_chat_id")));
        chat.setRecipientList(RecipientManager.getByChatId(db, chat.getId()));
        return chat;
    }

    private static Cursor getById(SQLiteDatabase db, long chatId) {
        return db.rawQuery("SELECT * FROM v_chat WHERE v_chat.chat_id = " + chatId + ";", null);
    }

    public static int getChatCount(SQLiteDatabase db, boolean avoidThoseWithRelatedPeppermintChat) {
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM v_chat" + (avoidThoseWithRelatedPeppermintChat ? " WHERE peppermint_chat_id = 0" : "") + ";", null);
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public static Cursor getAll(SQLiteDatabase db, boolean avoidThoseWithRelatedPeppermintChat) {
        return db.rawQuery("SELECT v_chat.*, v_chat.chat_id as _id FROM v_chat" + (avoidThoseWithRelatedPeppermintChat ? " WHERE peppermint_chat_id = 0" : "") +
                " ORDER BY last_message_ts DESC", null);
    }

    public static String getOldestChatTimestamp(SQLiteDatabase db) {
        String ts = null;
        Cursor cursor = db.rawQuery("SELECT last_message_ts FROM tbl_chat ORDER BY last_message_ts ASC;", null);
        if(cursor.moveToFirst()) {
            ts = cursor.getString(0);
        }
        cursor.close();
        return ts;
    }

    public static Chat getChatById(SQLiteDatabase db, long chatId) {
        Chat chat = null;
        Cursor cursor = getById(db, chatId);
        if(cursor.moveToFirst()) {
            chat = getChatFromCursor(db, cursor);
        }
        cursor.close();
        return chat;
    }

    public static Chat getMainChatByDroidContactId(SQLiteDatabase db, long droidContactId) {
        Cursor cursor = db.rawQuery("SELECT v_chat.* FROM v_chat, tbl_chat_recipient, tbl_recipient WHERE " +
                "v_chat.chat_id = tbl_chat_recipient.chat_id AND tbl_chat_recipient.recipient_id = tbl_recipient.recipient_id AND tbl_recipient.droid_contact_id = " + droidContactId +
                " ORDER BY v_chat.peppermint_chat_id ASC LIMIT 1;", null);
        Chat chat = null;
        if(cursor.moveToNext()) {
            chat = getChatFromCursor(db, cursor);
        }
        cursor.close();
        return chat;
    }

    public static Chat getChatByRecipients(SQLiteDatabase db, Recipient... recipients) {
        List<Recipient> recipientList = new ArrayList<>();
        Collections.addAll(recipientList, recipients);
        return getChatByRecipients(db, recipientList);
    }

    public static Chat getChatByRecipients(SQLiteDatabase db, List<Recipient> recipientList) {
        int recipientAmount = recipientList.size();
        String[] conditions = new String[recipientAmount];
        for(int i=0; i<recipientAmount; i++) {
            conditions[i] = Utils.joinString(" AND ", recipientList.get(i).getMimeType() != null ? "mimetype = " + DatabaseUtils.sqlEscapeString(recipientList.get(i).getMimeType()) : null,
                    recipientList.get(i).getVia() != null ? "via = " + DatabaseUtils.sqlEscapeString(recipientList.get(i).getVia()) : null);
        }

        String where = Utils.joinString(" OR ", conditions);
        Cursor cursor = db.rawQuery("SELECT v_chat.* FROM v_chat, tbl_chat_recipient, tbl_recipient WHERE " +
                "v_chat.chat_id = tbl_chat_recipient.chat_id AND tbl_chat_recipient.recipient_id = tbl_recipient.recipient_id AND " + where + ";", null);
        Chat chat = null;
        if(cursor.getCount() == recipientAmount && cursor.moveToFirst()) {
            chat = getChatFromCursor(db, cursor);
        }
        cursor.close();
        return chat;
    }

    // OPERATIONS
    private static void refreshTimestamps(SQLiteDatabase db, long peppermintChatId) throws SQLException {
        db.execSQL("UPDATE tbl_chat SET last_message_ts = (SELECT MAX(last_message_ts) FROM v_chat WHERE chat_id = " + peppermintChatId + " OR peppermint_chat_id = " + peppermintChatId + ") WHERE chat_id = " + peppermintChatId);
    }

    private static void deassociateRecipients(SQLiteDatabase db, long chatId) throws SQLException {
        db.delete("tbl_chat_recipient", "chat_id = " + chatId, null);
    }

    private static void associateRecipients(SQLiteDatabase db, long chatId, List<Recipient> recipientList) throws SQLException {
        db.beginTransaction();

        try {
            for (Recipient recipient : recipientList) {
                if(recipient.getId() <= 0) {
                    throw new IllegalArgumentException("Must register the recipient first!");
                }

                ContentValues cv = new ContentValues();
                cv.put("chat_id", chatId);
                cv.put("recipient_id", recipient.getId());
                cv.put("registration_ts", DateContainer.getCurrentUTCTimestamp());

                long id = db.insert("tbl_chat_recipient", null, cv);
                if (id < 0) {
                    throw new SQLException("Unable to associate chat with recipient!");
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static Chat insert(SQLiteDatabase db, Chat chat) throws SQLException {
        db.beginTransaction();

        long id = 0;

        try {
            ContentValues cv = new ContentValues();
            cv.put("title", chat.getTitle());
            cv.put("last_message_ts", chat.getLastMessageTimestamp());

            id = db.insert("tbl_chat", null, cv);
            if (id < 0) {
                throw new SQLException("Unable to insert chat!");
            }

            associateRecipients(db, id, chat.getRecipientList());
            chat.setId(id);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // get additional data only visible through the v_chat view
        Cursor autoCursor = db.rawQuery("SELECT peppermint_chat_id FROM v_chat WHERE chat_id = " + id, null);
        try {
            if (autoCursor.moveToNext()) {
                chat.setPeppermintChatId(autoCursor.getLong(autoCursor.getColumnIndex("peppermint_chat_id")));
            }
        } finally {
            autoCursor.close();
        }

        if(chat.getPeppermintChatId() > 0) {
            refreshTimestamps(db, chat.getPeppermintChatId());
        }

        Log.d(TAG, "INSERTED " + chat);

        return chat;
    }

    public static void update(SQLiteDatabase db, Chat chat) throws SQLException {
        db.beginTransaction();
        try {
            deassociateRecipients(db, chat.getId());
            associateRecipients(db, chat.getId(), chat.getRecipientList());

            ContentValues cv = new ContentValues();
            if(chat.getTitle() != null) {
                cv.put("title", chat.getTitle());
            }
            cv.put("last_message_ts", chat.getLastMessageTimestamp());

            long id = db.update("tbl_chat", cv, "chat_id = " + chat.getId(), null);
            if(id < 0) {
                throw new SQLException("Unable to update chat!");
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // get additional data only visible through the v_chat view
        Cursor autoCursor = db.rawQuery("SELECT peppermint_chat_id FROM v_chat WHERE chat_id = " + chat.getId(), null);
        try {
            if (autoCursor.moveToNext()) {
                chat.setPeppermintChatId(autoCursor.getLong(autoCursor.getColumnIndex("peppermint_chat_id")));
            }
        } finally {
            autoCursor.close();
        }

        if(chat.getPeppermintChatId() > 0) {
            refreshTimestamps(db, chat.getPeppermintChatId());
        }

        Log.d(TAG, "UPDATED " + chat);
    }

    public static Chat insertOrUpdate(SQLiteDatabase db, Chat chat) throws  SQLException {
        if(chat.getId() <= 0) {
            Chat searchedChat = null;

            if(chat.getRecipientList() != null && chat.getRecipientList().size() > 0) {
                searchedChat = getChatByRecipients(db, chat.getRecipientList());
            }

            if (searchedChat == null) {
                return insert(db, chat);
            }

            chat.setId(searchedChat.getId());
        }

        update(db, chat);

        return chat;
    }

    public static void delete(SQLiteDatabase db, long chatId) throws SQLException {
        db.beginTransaction();

        try {
            MessageManager.deleteByChat(db, chatId);
            deassociateRecipients(db, chatId);

            long resId = db.delete("tbl_chat", "chat_id = " + chatId, null);
            if (resId < 0) {
                throw new SQLException("Unable to delete chat!");
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        Log.d(TAG, "DELETED ChatId " + chatId);
    }

    public static void printAllData(Context context) {
        SQLiteDatabase db = DatabaseHelper.getInstance(context).getReadableDatabase();

        Cursor cursor = getAll(db, false);
        while(cursor.moveToNext()) {
            StringBuilder fieldsBuilder = new StringBuilder();
            for(int i=0; i<cursor.getColumnCount(); i++) {
                if(i > 0) {
                    fieldsBuilder.append(", ");
                }
                fieldsBuilder.append(cursor.getColumnName(i));
                fieldsBuilder.append("=");
                fieldsBuilder.append(cursor.getString(i));
            }
            Log.d("ChatManager", "CHAT # " + fieldsBuilder.toString());
        }
        cursor.close();
    }
}
