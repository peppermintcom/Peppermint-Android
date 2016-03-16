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
    private static Map<String, ContactRaw> RAW_CONTACT_CACHE = new HashMap<>();
    public static void clearCache() {
        RAW_CONTACT_CACHE.clear();
    }

    public static Message insertReceivedMessage(Context context, SQLiteDatabase db, String receiverEmail,
                                                String senderName, String senderEmail, String audioUrl,
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
            String[] names = Utils.getFirstAndLastNames(senderName);
            RAW_CONTACT_CACHE.put(rawContactKey, ContactManager.insertOrUpdate(context, 0, names[0], names[1], null, senderEmail, null, receiverEmail, true));
        }
        ContactRaw contactRaw = RAW_CONTACT_CACHE.get(rawContactKey);

        Chat chat = null;
        Recording recording = null;

        // insert chat and recipient
        ChatRecipient chatRecipient = new ChatRecipient(0, contactRaw, DateContainer.getCurrentUTCTimestamp());
        // try to find already existent chat
        Chat foundChat = ChatManager.getChatByRecipients(db, chatRecipient);
        if(foundChat != null) {
            // if found, update the last message timestamp
            String chatTimestamp = createdTs;
            if(foundChat.getLastMessageTimestamp() != null && foundChat.getLastMessageTimestamp().compareToIgnoreCase(createdTs) > 0) {
                chatTimestamp = foundChat.getLastMessageTimestamp();
            }
            ChatManager.update(db, foundChat.getId(), chatRecipient.getDisplayName(), chatTimestamp);
            chat = foundChat;
        } else {
            // otherwise, just insert
            chat = ChatManager.insert(db, chatRecipient.getDisplayName(), createdTs, chatRecipient);
        }

        // insert recording
        recording = RecordingManager.insert(db, null, durationSeconds * 1000l, 0, false, createdTs, Recording.CONTENT_TYPE_AUDIO);

        // insert message
        message = MessageManager.insert(db, chat.getId(), chat.getRecipientList().get(0).getId(), recording.getId(),
                serverId, null, audioUrl, transcription, null, null, createdTs, false, true, false);
        message.setRecordingParameter(recording);
        message.setChatParameter(chat);

        return message;
    }

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

        Chat chat = null;
        Recording recording = null;

        // insert chat and recipient
        ChatRecipient chatRecipient = new ChatRecipient(0, contactRaw, DateContainer.getCurrentUTCTimestamp());
        // try to find already existent chat
        Chat foundChat = ChatManager.getChatByRecipients(db, chatRecipient);
        if(foundChat != null) {
            // if found, update the last message timestamp
            String chatTimestamp = createdTs;
            if(foundChat.getLastMessageTimestamp() != null && foundChat.getLastMessageTimestamp().compareToIgnoreCase(createdTs) > 0) {
                chatTimestamp = foundChat.getLastMessageTimestamp();
            }
            ChatManager.update(db, foundChat.getId(), chatRecipient.getDisplayName(), chatTimestamp);
            chat = foundChat;
        } else {
            // otherwise, just insert
            chat = ChatManager.insert(db, chatRecipient.getDisplayName(), createdTs, chatRecipient);
        }

        // insert recording
        recording = RecordingManager.insert(db, null, durationSeconds * 1000l, 0, false, createdTs, Recording.CONTENT_TYPE_AUDIO);

        // insert message
        message = MessageManager.insert(db, chat.getId(), chat.getRecipientList().get(0).getId(), recording.getId(),
                serverId, null, audioUrl, transcription, null, null, createdTs, true, false, false);
        message.setChatParameter(chat);
        message.setRecordingParameter(recording);

        return message;
    }

}
