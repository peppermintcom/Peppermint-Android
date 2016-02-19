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
 *
 * Database operations for {@link Chat}
 */
public class ChatManager {

    /**
     * Gets the chat data inside the Cursor's current position and puts it in an instance
     * of the Chat structure.<br />
     * <strong>If context is supplied, it will also load all main recipient data.
     * Accessible through {@link Chat#getMainRecipientParameter()}</strong>
     *
     * @param context the app context (optional)
     * @param cursor the cursor
     * @return the Chat instance
     */
    public static Chat getFromCursor(Context context, Cursor cursor) {
        Chat message = new Chat();
        message.setId(cursor.getLong(cursor.getColumnIndex("chat_id")));
        message.setMainRecipientId(cursor.getLong(cursor.getColumnIndex("main_recipient_id")));
        message.setLastMessageTimestamp(cursor.getString(cursor.getColumnIndex("last_message_ts")));
        if(context != null) {
            message.setMainRecipientParameter(RecipientManager.getRecipientByContactId(context, message.getMainRecipientId()));
        }
        return message;
    }

    /**
     * Obtains the chat cursor with the supplied id from the database.
     *
     * @param db the local database connection
     * @param chatId the chat id
     * @return the cursor instance with all data
     */
    public static Cursor getById(SQLiteDatabase db, long chatId) {
        return db.rawQuery("SELECT * FROM tbl_chat WHERE chat_id = " + chatId + ";", null);
    }

    /**
     * Obtains the chat with the supplied id from the database.
     *
     * @param context the app context (optional)
     * @param db the local database connection
     * @param chatId the chat id
     * @return the chat instance with all data
     */
    public static Chat getChatById(Context context, SQLiteDatabase db, long chatId) {
        Chat chat = null;
        Cursor cursor = getById(db, chatId);
        if(cursor.moveToFirst()) {
            chat = getFromCursor(context, cursor);
        }
        cursor.close();
        return chat;
    }

    /**
     * Obtains the chat cursor with the supplied main recipient id from the database.
     *
     * @param db the local database connection
     * @param mainRecipientId the chat's main recipient id
     * @return the cursor instance with all data
     */
    public static Cursor getMainRecipient(SQLiteDatabase db, long mainRecipientId) {
        return db.rawQuery("SELECT * FROM tbl_chat WHERE main_recipient_id = " + mainRecipientId + ";", null);
    }

    /**
     * Obtains the chat with the supplied main recipient id from the database.
     *
     * @param context the app context (optional)
     * @param db the local database connection
     * @param mainRecipientId the chat's main recipient id
     * @return the chat instance with all data
     */
    public static Chat getChatByMainRecipient(Context context, SQLiteDatabase db, long mainRecipientId) {
        Chat chat = null;
        Cursor cursor = getMainRecipient(db, mainRecipientId);
        if(cursor.moveToFirst()) {
            chat = getFromCursor(context, cursor);
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
    public static Cursor getAll(SQLiteDatabase db) {
        return db.rawQuery("SELECT tbl_chat.*, tbl_chat.chat_id as _id FROM tbl_chat ORDER BY last_message_ts DESC", null);
    }

    /**
     * Obtains a list with all chats stored in the local database, ordered by last message date DESC.
     *
     * @param context the app context (optional)
     * @param db the local database connection
     * @return the list of all chats
     */
    public static List<Chat> getChats(Context context, SQLiteDatabase db) {
        List<Chat> list = new ArrayList<>();
        Cursor cursor = getAll(db);
        if(cursor.moveToFirst()) {
            do {
                list.add(getFromCursor(context, cursor));
            } while(cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * Inserts the supplied chat into the local database.
     *
     * @param db the local database connection
     * @param mainRecipientId the chat main recipient id
     * @param lastMessageTimestamp the chat last message timestamp
     * @throws SQLException
     */
    public static Chat insert(SQLiteDatabase db, long mainRecipientId, String lastMessageTimestamp) throws SQLException {
        ContentValues cv = new ContentValues();

        if(mainRecipientId > 0) {
            cv.put("main_recipient_id", mainRecipientId);
        } else {
            cv.putNull("main_recipient_id");
        }
        cv.put("last_message_ts", lastMessageTimestamp);

        long id = db.insert("tbl_chat", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert chat!");
        }

        Chat chat = new Chat(mainRecipientId, lastMessageTimestamp);
        chat.setId(id);
        return chat;
    }

    /**
     * Updates the supplied chat data (id must be supplied).
     * An SQLException is thrown if the chat id does not exist in the database.
     *
     * @param db the local database connection
     * @param chatId the chat id
     * @param mainRecipientId the main recipient id
     * @param lastMessageTimestamp the chat last message timestamp
     * @throws SQLException
     */
    public static Chat update(SQLiteDatabase db, long chatId, long mainRecipientId, String lastMessageTimestamp) throws SQLException {
        ContentValues cv = new ContentValues();

        if(mainRecipientId > 0) {
            cv.put("main_recipient_id", mainRecipientId);
        } else {
            cv.putNull("main_recipient_id");
        }
        cv.put("last_message_ts", lastMessageTimestamp);

        long id = db.update("tbl_chat", cv, "chat_id = " + chatId, null);
        if(id < 0) {
            throw new SQLException("Unable to update chat!");
        }

        Chat chat = new Chat(mainRecipientId, lastMessageTimestamp);
        chat.setId(chatId);
        return chat;
    }

    /**
     * If a main recipient with an id is supplied, it performs an update, otherwise, it inserts the chat.
     *
     * @param db the local database connection
     * @param chatId the chat id
     * @param mainRecipientId the main recipient id
     * @param lastMessageTimestamp the chat last message timestamp
     * @throws SQLException
     */
    public static Chat insertOrUpdate(SQLiteDatabase db, long chatId, long mainRecipientId, String lastMessageTimestamp) throws  SQLException {
        if(chatId <= 0) {
            Chat searchedChat = null;

            if(mainRecipientId > 0) {
                searchedChat = getChatByMainRecipient(null, db, mainRecipientId);
            }

            if (searchedChat == null) {
                return insert(db, mainRecipientId, lastMessageTimestamp);
            }

            chatId = searchedChat.getId();
        }

        return update(db, chatId, mainRecipientId, lastMessageTimestamp);
    }

    /**
     * Deletes the supplied chat data (id must be supplied).<br />
     * <strong>It also deletes all messages associated with the chat!</strong><br />
     * An SQLException is thrown if the chat id does not exist in the database.
     *
     * @param db the local database connection
     * @param chatId the chat id
     * @throws SQLException
     */
    public static void delete(SQLiteDatabase db, long chatId) throws SQLException {
        MessageManager.deleteByChat(db, chatId);

        long resId = db.delete("tbl_chat", "chat_id = " + chatId, null);
        if(resId < 0) {
            throw new SQLException("Unable to delete chat!");
        }
    }

    /**
     * Search for each chat main recipient and delete those for which no main recipient is found.<br/>
     * This happens, for instance, when the person deletes the recipient from the contact list.
     *
     * @param context the app context (required to run {@link RecipientManager#getRecipientByContactId(Context, long)})
     * @param db the db connection
     * @throws SQLException
     */
    public static void deleteMissingRecipientChats(Context context, SQLiteDatabase db)  throws SQLException {
        Cursor cursor = getAll(db);
        while(cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex("chat_id"));
            long recipientId = cursor.getLong(cursor.getColumnIndex("main_recipient_id"));
            if(RecipientManager.getRecipientByContactId(context, recipientId) == null) {
                delete(db, id);
            }
        }
    }

}
