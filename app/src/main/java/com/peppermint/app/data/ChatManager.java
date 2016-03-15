package com.peppermint.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
     * <strong>If db is supplied, it will also load all recipient data.
     * Accessible through {@link Chat#getRecipientList()}</strong>
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

        if(db != null) {
            chat.setRecipientList(getChatRecipientsByChat(db, chat.getId()));
        }

        return chat;
    }


    public static ChatRecipient getChatRecipientFromCursor(Cursor cursor) {
        ChatRecipient chatRecipient = new ChatRecipient();

        chatRecipient.setId(cursor.getLong(cursor.getColumnIndex("chat_recipient_id")));
        chatRecipient.setChatId(cursor.getLong(cursor.getColumnIndex("chat_id")));
        chatRecipient.setRawContactId(cursor.getLong(cursor.getColumnIndex("raw_contact_id")));
        chatRecipient.setContactId(cursor.getLong(cursor.getColumnIndex("contact_id")));
        chatRecipient.setDisplayName(cursor.getString(cursor.getColumnIndex("display_name")));
        chatRecipient.setVia(cursor.getString(cursor.getColumnIndex("via")));
        chatRecipient.setMimeType(cursor.getString(cursor.getColumnIndex("mimetype")));
        chatRecipient.setPhotoUri(cursor.getString(cursor.getColumnIndex("photo_uri")));
        chatRecipient.setAddedTimestamp(cursor.getString(cursor.getColumnIndex("added_ts")));

        return chatRecipient;
    }

    public static int getChatCount(SQLiteDatabase db) {
        int count = 0;
        Cursor cursor = db.rawQuery("select count(*) from tbl_chat;", null);
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public static List<ChatRecipient> getChatRecipientsByChat(SQLiteDatabase db, long chatId) {
        List<ChatRecipient> chatRecipients = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT * FROM tbl_chat_recipient WHERE chat_id = " + chatId + ";", null);
        while(cursor.moveToNext()) {
            chatRecipients.add(getChatRecipientFromCursor(cursor));
        }
        cursor.close();

        return chatRecipients;
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

    public static String getOldestChatTimestamp(SQLiteDatabase db) {
        String ts = null;
        Cursor cursor = db.rawQuery("SELECT last_message_ts FROM tbl_chat ORDER BY last_message_ts ASC;", null);
        if(cursor.moveToFirst()) {
            ts = cursor.getString(0);
        }
        cursor.close();
        return ts;
    }

    /**
     * Obtains the chat with the supplied id from the database.
     *
     * @param db the local database connection
     * @param chatId the chat id
     * @return the chat instance with all data
     */
    public static Chat getChatById(SQLiteDatabase db, long chatId) {
        Chat chat = null;
        Cursor cursor = getById(db, chatId);
        if(cursor.moveToFirst()) {
            chat = getChatFromCursor(db, cursor);
        }
        cursor.close();
        return chat;
    }

    /**
     * Obtains the chat with the supplied main recipient id from the database.
     *
     * @param db the local database connection
     * @param recipients the recipients of the chat
     * @return the chat instance with all data
     */
    public static Chat getChatByRecipients(SQLiteDatabase db, ChatRecipient... recipients) {
        String[] conditions = new String[recipients.length];
        for(int i=0; i<recipients.length; i++) {
            conditions[i] = Utils.joinString(" AND ", recipients[i].getContactId() > 0 ? "contact_id = " + recipients[i].getContactId() : null,
                    recipients[i].getVia() != null ? "via = " + DatabaseUtils.sqlEscapeString(recipients[i].getVia()) : null);
        }

        String where = Utils.joinString(" OR ", conditions);
        Cursor cursor = db.rawQuery("SELECT tbl_chat.* FROM tbl_chat, tbl_chat_recipient WHERE tbl_chat.chat_id = tbl_chat_recipient.chat_id AND " + where + ";", null);
        Chat chat = null;
        if(cursor.getCount() == recipients.length && cursor.moveToFirst()) {
            chat = getChatFromCursor(db, cursor);
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

    public static ChatRecipient insertChatRecipient(SQLiteDatabase db, long chatId, long rawContactId, long contactId, String displayName, String via, String mimetype, String photoUri, String addedTimestamp) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("chat_id", chatId);
        cv.put("raw_contact_id", rawContactId);
        cv.put("contact_id", contactId);
        cv.put("display_name", displayName);
        cv.put("via", via);
        cv.put("mimetype", mimetype);
        cv.put("photo_uri", photoUri);
        cv.put("added_ts", addedTimestamp);

        long id = db.insert("tbl_chat_recipient", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert chat recipient!");
        }

        return new ChatRecipient(id, rawContactId, contactId, chatId, displayName, mimetype, via, photoUri, addedTimestamp);
    }

    /**
     * Inserts the supplied chat into the local database.
     *
     * @param db the local database connection
     * @param lastMessageTimestamp the chat last message timestamp
     * @param recipients list of recipients for this chat
     * @throws SQLException
     */
    public static Chat insert(SQLiteDatabase db, String title, String lastMessageTimestamp, ChatRecipient... recipients) throws SQLException {
        /*db.beginTransaction();*/

        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("last_message_ts", lastMessageTimestamp);

        long id = db.insert("tbl_chat", null, cv);
        if(id < 0) {
            /*db.endTransaction();*/
            throw new SQLException("Unable to insert chat!");
        }

        List<ChatRecipient> chatRecipientList = new ArrayList<>();
        for(ChatRecipient chatRecipient : recipients) {
            chatRecipient.setChatId(id);
            ChatRecipient newChatRecipient = insertChatRecipient(db, chatRecipient.getChatId(),
                    chatRecipient.getRawContactId(), chatRecipient.getContactId(),
                    chatRecipient.getDisplayName(), chatRecipient.getVia(), chatRecipient.getMimeType(),
                    chatRecipient.getPhotoUri(), chatRecipient.getAddedTimestamp());
            chatRecipientList.add(newChatRecipient);
            chatRecipient.setId(newChatRecipient.getId());
        }

        /*db.setTransactionSuccessful();
        db.endTransaction();*/

        Chat chat = new Chat(chatRecipientList, lastMessageTimestamp);
        chat.setRecipientList(chatRecipientList);
        chat.setId(id);
        return chat;
    }

    public static void updateChatRecipient(SQLiteDatabase db, long chatRecipientId, long chatId, long rawContactId, long contactId, String displayName, String via, String mimetype, String photoUri, String addedTimestamp) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("chat_id", chatId);
        cv.put("raw_contact_id", rawContactId);
        cv.put("contact_id", contactId);
        cv.put("display_name", displayName);
        cv.put("via", via);
        cv.put("mimetype", mimetype);
        cv.put("photo_uri", photoUri);
        cv.put("added_ts", addedTimestamp);

        long id = db.update("tbl_chat_recipient", cv, "chat_recipient_id = " + chatRecipientId, null);
        if(id < 0) {
            throw new SQLException("Unable to update chat recipient!");
        }
    }

    /**
     * Updates the supplied chat data (id must be supplied).
     * An SQLException is thrown if the chat id does not exist in the database.
     *
     * @param db the local database connection
     * @param chatId the chat id
     * @param lastMessageTimestamp the chat last message timestamp
     * @throws SQLException
     */
    public static void update(SQLiteDatabase db, long chatId, String title, String lastMessageTimestamp) throws SQLException {
        ContentValues cv = new ContentValues();
        if(title != null) {
            cv.put("title", title);
        }
        cv.put("last_message_ts", lastMessageTimestamp);

        long id = db.update("tbl_chat", cv, "chat_id = " + chatId, null);
        if(id < 0) {
            throw new SQLException("Unable to update chat!");
        }
    }

    /**
     * If a main recipient with an id is supplied, it performs an update, otherwise, it inserts the chat.
     *
     * @param db the local database connection
     * @param chatId the chat id
     * @param lastMessageTimestamp the chat last message timestamp
     * @param recipients the recipients of the chat
     * @throws SQLException
     */
    public static Chat insertOrUpdate(SQLiteDatabase db, long chatId, String title, String lastMessageTimestamp, ChatRecipient... recipients) throws  SQLException {
        if(chatId <= 0) {
            Chat searchedChat = null;

            if(recipients != null && recipients.length > 0) {
                searchedChat = getChatByRecipients(db, recipients);
            }

            if (searchedChat == null) {
                return insert(db, title, lastMessageTimestamp, recipients);
            }

            chatId = searchedChat.getId();
        }

        for(ChatRecipient chatRecipient : recipients) {
            updateChatRecipient(db, chatRecipient.getId(), chatRecipient.getChatId(), chatRecipient.getRawContactId(), chatRecipient.getContactId(),
                    chatRecipient.getDisplayName(), chatRecipient.getVia(), chatRecipient.getMimeType(), chatRecipient.getPhotoUri(), chatRecipient.getAddedTimestamp());
        }
        update(db, chatId, title, lastMessageTimestamp);

        Chat chat = new Chat(Arrays.asList(recipients), lastMessageTimestamp);
        chat.setId(chatId);
        return chat;
    }

    public static void deleteChatRecipientsByChat(SQLiteDatabase db, long chatId) throws SQLException {
        long resId = db.delete("tbl_chat_recipient", "chat_id = " + chatId, null);
        if(resId < 0) {
            throw new SQLException("Unable to delete chat recipients!");
        }
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
        db.beginTransaction();

        MessageManager.deleteByChat(db, chatId);
        deleteChatRecipientsByChat(db, chatId);

        long resId = db.delete("tbl_chat", "chat_id = " + chatId, null);
        if(resId < 0) {
            db.endTransaction();
            throw new SQLException("Unable to delete chat!");
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }

}
