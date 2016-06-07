package com.peppermint.app.dal.chat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.dal.DataObjectManager;
import com.peppermint.app.dal.message.MessageManager;
import com.peppermint.app.dal.recipient.RecipientManager;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Database operations for {@link Chat}
 */
public class ChatManager extends DataObjectManager<Long, Chat> {

    private static ChatManager INSTANCE;

    public static ChatManager getInstance(Context context) {
        if(INSTANCE == null) {
            INSTANCE = new ChatManager(context);
        }
        return INSTANCE;
    }

    protected ChatManager(Context context) {
        super(context);
    }

    // OPERATIONS
    private void refreshTimestamps(SQLiteDatabase db, long peppermintChatId) throws SQLException {
        db.execSQL("UPDATE tbl_chat SET last_message_ts = (SELECT MAX(last_message_ts) FROM v_chat WHERE chat_id = " + peppermintChatId + " OR peppermint_chat_id = " + peppermintChatId + ") WHERE chat_id = " + peppermintChatId);
    }

    private void deassociateRecipients(SQLiteDatabase db, long chatId) throws SQLException {
        db.delete("tbl_chat_recipient", "chat_id = " + chatId, null);
    }

    private void associateRecipients(SQLiteDatabase db, long chatId, List<Recipient> recipientList) throws SQLException {
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
    }

    @Override
    protected Chat doInsert(SQLiteDatabase db, Chat chat) throws SQLException {

        ContentValues cv = new ContentValues();
        cv.put("title", chat.getTitle());
        cv.put("last_message_ts", chat.getLastMessageTimestamp());

        long id = -1;
        db.beginTransaction();
        try {
            id = db.insert("tbl_chat", null, cv);
            if (id < 0) {
                throw new SQLException("Unable to insert chat!");
            }

            associateRecipients(db, id, chat.getRecipientList());

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        chat.setId(id);

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

        return chat;
    }

    @Override
    protected void doUpdate(SQLiteDatabase db, Chat chat) throws SQLException {
        ContentValues cv = new ContentValues();
        if(chat.getTitle() != null) {
            cv.put("title", chat.getTitle());
        }
        cv.put("last_message_ts", chat.getLastMessageTimestamp());

        db.beginTransaction();
        try {
            deassociateRecipients(db, chat.getId());
            associateRecipients(db, chat.getId(), chat.getRecipientList());

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
    }

    @Override
    protected void doDelete(SQLiteDatabase db, Long chatId) throws SQLException {
        MessageManager.getInstance(mContext).deleteByChat(db, chatId);

        db.beginTransaction();
        try {
            deassociateRecipients(db, chatId);

            long resId = db.delete("tbl_chat", "chat_id = " + chatId, null);
            if (resId < 0) {
                throw new SQLException("Unable to delete chat!");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    protected Chat newDataObjectInstance(Long id) {
        final Chat chat = new Chat();
        chat.setId(id);
        return chat;
    }

    @Override
    protected Chat doGetFromCursor(SQLiteDatabase db, Cursor cursor) {
        final Chat chat = obtainCacheDataObject(cursor.getLong(cursor.getColumnIndex("chat_id")));
        chat.setTitle(cursor.getString(cursor.getColumnIndex("title")));
        chat.setLastMessageTimestamp(cursor.getString(cursor.getColumnIndex("last_message_ts")));
        chat.setPeppermintChatId(cursor.getLong(cursor.getColumnIndex("peppermint_chat_id")));
        if(db != null) {
            chat.setRecipientList(RecipientManager.getInstance(mContext).getByChatId(db, chat.getId()));
        }
        return chat;
    }

    @Override
    public boolean exists(SQLiteDatabase db, Chat chat) throws SQLException {
        if(chat.getId() <= 0) {
            Chat searchedChat = null;

            if(chat.getRecipientList() != null && chat.getRecipientList().size() > 0) {
                searchedChat = getChatByRecipients(db, chat.getRecipientList());
            }

            if (searchedChat == null) {
                return false;
            }

            chat.setId(searchedChat.getId());
        }
        return true;
    }

    private Cursor getById(SQLiteDatabase db, long chatId) {
        return db.rawQuery("SELECT * FROM v_chat WHERE v_chat.chat_id = " + chatId + ";", null);
    }

    public int getChatCount(SQLiteDatabase db, boolean avoidThoseWithRelatedPeppermintChat) {
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM v_chat" + (avoidThoseWithRelatedPeppermintChat ? " WHERE peppermint_chat_id = 0" : "") + ";", null);
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public Cursor getAll(SQLiteDatabase db, boolean avoidThoseWithRelatedPeppermintChat) {
        return db.rawQuery("SELECT v_chat.*, v_chat.chat_id as _id FROM v_chat" + (avoidThoseWithRelatedPeppermintChat ? " WHERE peppermint_chat_id = 0" : "") +
                " ORDER BY last_message_ts DESC", null);
    }

    public String getOldestChatTimestamp(SQLiteDatabase db) {
        String ts = null;
        Cursor cursor = db.rawQuery("SELECT last_message_ts FROM tbl_chat ORDER BY last_message_ts ASC;", null);
        if(cursor.moveToFirst()) {
            ts = cursor.getString(0);
        }
        cursor.close();
        return ts;
    }

    public Chat getChatById(SQLiteDatabase db, long chatId) {
        Chat chat = null;
        Cursor cursor = getById(db, chatId);
        if(cursor.moveToFirst()) {
            chat = getFromCursor(db, cursor);
        }
        cursor.close();
        return chat;
    }

    public Chat getMainChatByDroidContactId(SQLiteDatabase db, long droidContactId) {
        Cursor cursor = db.rawQuery("SELECT v_chat.* FROM v_chat, tbl_chat_recipient, tbl_recipient WHERE " +
                "v_chat.chat_id = tbl_chat_recipient.chat_id AND tbl_chat_recipient.recipient_id = tbl_recipient.recipient_id AND tbl_recipient.droid_contact_id = " + droidContactId +
                " ORDER BY v_chat.peppermint_chat_id ASC LIMIT 1;", null);
        Chat chat = null;
        if(cursor.moveToNext()) {
            chat = getFromCursor(db, cursor);
        }
        cursor.close();
        return chat;
    }

    public Chat getChatByRecipients(SQLiteDatabase db, List<Recipient> recipientList) {
        int recipientAmount = recipientList.size();
        String[] conditions = new String[recipientAmount];
        for(int i=0; i<recipientAmount; i++) {
            conditions[i] = Utils.joinString(" AND ", recipientList.get(i).getMimeType() != null ? "mimetype = " + DatabaseUtils.sqlEscapeString(recipientList.get(i).getMimeType()) : null,
                    recipientList.get(i).getVia() != null ? "via = " + DatabaseUtils.sqlEscapeString(recipientList.get(i).getVia()) : null);
        }

        String where = Utils.joinString(" OR ", conditions);
        Cursor cursor = db.rawQuery("SELECT v_chat.*, tbl_recipient.via AS via, tbl_recipient.mimetype AS mimetype FROM v_chat, tbl_chat_recipient, tbl_recipient WHERE " +
                "v_chat.chat_id = tbl_chat_recipient.chat_id AND tbl_chat_recipient.recipient_id = tbl_recipient.recipient_id AND " + where + ";", null);
        Chat chat = null;
        Set<String> uniqueMimeVia = new HashSet<>();
        while(cursor.moveToNext()) {
            String mimetype = cursor.getString(cursor.getColumnIndex("mimetype"));
            String via = cursor.getString(cursor.getColumnIndex("via"));
            String key = mimetype + via;

            if(!uniqueMimeVia.contains(key)) {
                uniqueMimeVia.add(key);
            }

            if(chat == null) {
                chat = getFromCursor(db, cursor);
            }
        }
        cursor.close();

        if(uniqueMimeVia.size() != recipientAmount) {
            return null;
        }

        return chat;
    }

}
