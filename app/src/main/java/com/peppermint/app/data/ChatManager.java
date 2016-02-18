package com.peppermint.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 18-02-2016.
 */
public class ChatManager {

    public static void deleteMissingRecipientChats(Context context, SQLiteDatabase db)  throws SQLException {
        Cursor cursor = getAllCursor(db);
        while(cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex("chat_id"));
            long recipientId = cursor.getLong(cursor.getColumnIndex("main_recipient_id"));
            if(RecipientManager.getRecipientById(context, recipientId) == null) {
                delete(db, id);
            }
        }
    }

    /**
     * Gets the chat data inside the Cursor's current position and puts it in an instance
     * of the Chat structure.
     *
     * @param cursor the cursor
     * @return the Chat instance
     */
    public static Chat getFromCursor(Cursor cursor) {
        Chat message = new Chat();
        message.setId(cursor.getLong(cursor.getColumnIndex("chat_id")));
        message.setMainRecipientId(cursor.getLong(cursor.getColumnIndex("main_recipient_id")));
        message.setLastMessageTimestamp(cursor.getString(cursor.getColumnIndex("last_message_ts")));
        return message;
    }

    /**
     * Inserts the supplied chat into the local database.
     *
     * @param db the local database connection
     * @param chat the Chat instance
     * @throws SQLException
     */
    public static long insert(SQLiteDatabase db, Chat chat) throws SQLException {
        ContentValues cv = new ContentValues();

        if(chat.getMainRecipientId() > 0) {
            cv.put("main_recipient_id", chat.getMainRecipientId());
        } else {
            cv.putNull("main_recipient_id");
        }
        cv.put("last_message_ts", chat.getLastMessageTimestamp());

        long id = db.insert("tbl_chat", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert chat!");
        }
        chat.setId(id);
        return id;
    }

    /**
     * Updates the supplied chat data (id must be supplied).
     * An SQLException is thrown if the chat id does not exist in the database.
     *
     * @param db the local database connection
     * @param chat the Chat instance
     * @throws SQLException
     */
    public static void update(SQLiteDatabase db, Chat chat) throws SQLException {
        ContentValues cv = new ContentValues();

        if(chat.getMainRecipientId() > 0) {
            cv.put("main_recipient_id", chat.getMainRecipientId());
        } else {
            cv.putNull("main_recipient_id");
        }
        cv.put("last_message_ts", chat.getLastMessageTimestamp());

        long id = db.update("tbl_chat", cv, "chat_id = " + chat.getId(), null);
        if(id < 0) {
            throw new SQLException("Unable to update chat!");
        }
    }

    /**
     * If a main recipient with an id is supplied, it performs an update, otherwise, it inserts the chat.
     *
     * @param db the local database connection
     * @param chat the Chat instance
     * @throws SQLException
     */
    public static void insertOrUpdate(SQLiteDatabase db, Chat chat) throws  SQLException {
        Chat searchedChat = null;

        if(chat.getMainRecipientId() > 0) {
            searchedChat = getByMainRecipient(db, chat.getMainRecipientId());
        }

        if (searchedChat == null) {
            insert(db, chat);
            return;

        }
        chat.setId(searchedChat.getId());
        update(db, chat);
    }

    /**
     * Deletes the supplied chat data (id must be supplied).
     * An SQLException is thrown if the chat id does not exist in the database.
     *
     * @param db the local database connection
     * @param id the Chat id
     * @throws SQLException
     */
    public static void delete(SQLiteDatabase db, long id) throws SQLException {
        MessageManager.deleteByChat(db, id);

        long resId = db.delete("tbl_chat", "chat_id = " + id, null);
        if(resId < 0) {
            throw new SQLException("Unable to delete chat!");
        }
    }

    /**
     * Obtains the chat cursor with the supplied id from the database.
     *
     * @param db the local database connection
     * @param id the Chat id
     * @return the cursor instance with all data
     */
    public static Cursor getCursor(SQLiteDatabase db, long id) {
        return db.rawQuery("SELECT * FROM tbl_chat WHERE chat_id = " + id + ";", null);
    }

    /**
     * Obtains the chat with the supplied id from the database.
     *
     * @param db the local database connection
     * @param id the Chat id
     * @return the chat instance with all data
     */
    public static Chat get(SQLiteDatabase db, long id) {
        Chat chat = null;
        Cursor cursor = getCursor(db, id);
        if(cursor.moveToFirst()) {
            chat = getFromCursor(cursor);
        }
        cursor.close();
        return chat;
    }

    public static Cursor getMainRecipientCursor(SQLiteDatabase db, long mainRecipientId) {
        return db.rawQuery("SELECT * FROM tbl_chat WHERE main_recipient_id = " + mainRecipientId + ";", null);
    }

    public static Chat getByMainRecipient(SQLiteDatabase db, long mainRecipientId) {
        Chat chat = null;
        Cursor cursor = getMainRecipientCursor(db, mainRecipientId);
        if(cursor.moveToFirst()) {
            chat = getFromCursor(cursor);
        }
        cursor.close();
        return chat;
    }

    /**
     * Obtains the cursor with all chats stored in the local database.
     *
     * @param db the local database connection
     * @return the cursor with all chats
     */
    public static Cursor getAllCursor(SQLiteDatabase db) {
        return db.rawQuery("SELECT tbl_chat.*, tbl_chat.chat_id as _id FROM tbl_chat ORDER BY last_message_ts DESC", null);
    }

    /**
     * Obtains a list with all chats stored in the local database, ordered by last message date DESC.
     *
     * @param db the local database connection
     * @return the list of all chats
     */
    public static List<Chat> getAll(SQLiteDatabase db) {
        List<Chat> list = new ArrayList<>();
        Cursor cursor = getAllCursor(db);
        if(cursor.moveToFirst()) {
            do {
                list.add(getFromCursor(cursor));
            } while(cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

}
