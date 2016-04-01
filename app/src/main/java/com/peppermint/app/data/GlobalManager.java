package com.peppermint.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz on 15-03-2016.
 */
public class GlobalManager {

    // faster contact lookup
    // must be manually cleared to avoid unnecessary memory consumption
    private static Map<String, ContactRaw> RAW_CONTACT_CACHE = new HashMap<>();

    /**
     * Clears the raw Android contact cache. <br />
     * <strong>This should be invoked once all necessary operations have been performed.
     *  Otherwise the cached contacts will be kept in memory.</strong>
     */
    public static void clearCache() {
        RAW_CONTACT_CACHE.clear();
    }

    /**
     * Inserts a received message in the database, performing all necessary verifications.<br />
     * <strong>This should be performed inside a single transaction.</strong>
     *
     * @param context the app context
     * @param db the database instance
     * @param receiverEmail the receiver's email
     * @param senderName the sender's name
     * @param senderEmail the sender's email
     * @param audioUrl the remote audio URL
     * @param serverId the message ID on the server
     * @param transcription the transcription text
     * @param createdTs the creation date
     * @param durationSeconds the duration in seconds
     * @param readTimestamp the timestamp of when the message was played
     * @return the inserted Message instance
     * @throws SQLException if the database insert fails
     * @throws ContactManager.InvalidPhoneException
     * @throws ContactManager.InvalidNameException
     * @throws ContactManager.InvalidEmailException
     */
    public static Message insertReceivedMessage(Context context, SQLiteDatabase db, String receiverEmail,
                                                String senderName, String senderEmail, String audioUrl,
                                                String serverId, String transcription, String createdTs,
                                                int durationSeconds, String readTimestamp) throws SQLException, ContactManager.InvalidPhoneException, ContactManager.InvalidNameException, ContactManager.InvalidEmailException {

        if(audioUrl == null || senderEmail == null || receiverEmail == null) {
            return null;
        }

        Message message = MessageManager.getMessageByIdOrServerId(db, 0, serverId);
        if(message != null) {
            return message;
        }

        // add contact if necessary
        String rawContactKey = senderEmail + receiverEmail;
        if(!RAW_CONTACT_CACHE.containsKey(rawContactKey)) {
            String[] names = Utils.getFirstAndLastNames(senderName);
            RAW_CONTACT_CACHE.put(rawContactKey, ContactManager.insertOrUpdate(context, 0, names[0], names[1], null, senderEmail, null, receiverEmail, true));
        }
        ContactRaw contactRaw = RAW_CONTACT_CACHE.get(rawContactKey);

        // insert chat and recipient
        Chat chat = insertOrUpdateTimestampChatAndRecipient(context, db, contactRaw, createdTs);

        // insert recording
        Recording recording = RecordingManager.insert(db, null, durationSeconds * 1000l, 0, false, createdTs, Recording.CONTENT_TYPE_AUDIO);

        // insert message
        message = MessageManager.insert(db, chat.getId(), chat.getRecipientList().get(0).getId(), recording.getId(),
                serverId, null, audioUrl, transcription, null, null, createdTs, false, true, readTimestamp != null);
        message.setRecordingParameter(recording);
        message.setChatParameter(chat);

        return message;
    }

    /**
     * Inserts a sent message in the database, performing all necessary verifications.<br />
     * <strong>This should be performed inside a single transaction.</strong>
     *
     * @param context the app context
     * @param db the database instance
     * @param receiverName the receiver's name
     * @param receiverEmail the receiver's email
     * @param senderEmail the sender's email
     * @param audioUrl the remote audio URL
     * @param serverId the message ID on the server
     * @param transcription the transcription text
     * @param createdTs the creation date
     * @param durationSeconds the duration in seconds
     * @return the inserted Message instance
     * @throws SQLException if the database insert fails
     * @throws ContactManager.InvalidPhoneException
     * @throws ContactManager.InvalidNameException
     * @throws ContactManager.InvalidEmailException
     */
    public static Message insertSentMessage(Context context, SQLiteDatabase db, String receiverName, String receiverEmail,
                                            String senderEmail, String audioUrl,
                                            String serverId, String transcription, String createdTs,
                                            int durationSeconds) throws SQLException, ContactManager.InvalidPhoneException, ContactManager.InvalidNameException, ContactManager.InvalidEmailException {

        if(audioUrl == null || senderEmail == null || receiverEmail == null) {
            return null;
        }

        Message message = MessageManager.getMessageByIdOrServerId(db, 0, serverId);
        if(message != null) {
            return message;
        }

        // add contact if necessary
        String rawContactKey = senderEmail + receiverEmail;
        if(!RAW_CONTACT_CACHE.containsKey(rawContactKey)) {
            String[] names = Utils.getFirstAndLastNames(receiverName);
            RAW_CONTACT_CACHE.put(rawContactKey, ContactManager.insertOrUpdate(context, 0, names[0], names[1], null, receiverEmail, null, senderEmail, true));
        }
        ContactRaw contactRaw = RAW_CONTACT_CACHE.get(rawContactKey);

        // insert chat and recipient
        Chat chat = insertOrUpdateTimestampChatAndRecipient(context, db, contactRaw, createdTs);

        // insert recording
        Recording recording = RecordingManager.insert(db, null, durationSeconds * 1000l, 0, false, createdTs, Recording.CONTENT_TYPE_AUDIO);

        // insert message
        message = MessageManager.insert(db, chat.getId(), chat.getRecipientList().get(0).getId(), recording.getId(),
                serverId, null, audioUrl, transcription, null, null, createdTs, true, false, false);
        message.setChatParameter(chat);
        message.setRecordingParameter(recording);

        return message;
    }

    /**
     * Inserts a Chat along with its ChatRecipient in the database.<br />
     * If the Chat along with the specified ChatRecipient already exists, updates its timestamp to
     * the most recent timestamp - either its current timestamp or newTimestamp.
     *
     * @param context the app context
     * @param db the database instance
     * @param contactRaw the raw android contact
     * @param newTimestamp the new timestamp
     * @return the Chat instance
     * @throws SQLException if the database insert fails
     */
    public static Chat insertOrUpdateTimestampChatAndRecipient(Context context, SQLiteDatabase db, ContactRaw contactRaw, String newTimestamp) throws SQLException {
        // insert chat and recipient
        ChatRecipient chatRecipient = new ChatRecipient(0, contactRaw, DateContainer.getCurrentUTCTimestamp());
        String chatTitle = chatRecipient.getDisplayName() == null ? chatRecipient.getVia() : chatRecipient.getDisplayName();

        // try to find already existent chat
        Chat foundChat = ChatManager.getChatByRecipients(db, chatRecipient);
        if(foundChat != null) {
            // if found, update the last message timestamp
            String chatTimestamp = newTimestamp;
            if(foundChat.getLastMessageTimestamp() != null && foundChat.getLastMessageTimestamp().compareToIgnoreCase(newTimestamp) > 0) {
                chatTimestamp = foundChat.getLastMessageTimestamp();
            }
            ChatManager.update(db, foundChat.getId(), chatTitle, chatTimestamp);
            return foundChat;
        }

        // otherwise, just insert
        return ChatManager.insert(context, db, chatTitle, newTimestamp, chatRecipient);
    }

}
