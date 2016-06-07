package com.peppermint.app.dal;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.peppermint.app.R;
import com.peppermint.app.dal.chat.ChatManager;
import com.peppermint.app.dal.contact.ContactManager;
import com.peppermint.app.dal.message.MessageManager;
import com.peppermint.app.dal.recipient.RecipientManager;
import com.peppermint.app.dal.recording.RecordingManager;
import com.peppermint.app.services.messenger.MessagesMarkPlayedTask;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.contact.ContactData;
import com.peppermint.app.dal.contact.ContactRaw;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.dal.recording.Recording;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Nuno Luz on 15-03-2016.
 *
 * Global business logic directly on top of the data access layer.
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

    public static Message insertReceivedMessage(final Context context, final SQLiteDatabase db, String receiverEmail,
                                                String senderName, String senderEmail, String audioUrl,
                                                String serverId, String transcription, String createdTs,
                                                int durationSeconds, String readTimestamp) throws SQLException, ContactManager.InvalidPhoneException, ContactManager.InvalidNameException, ContactManager.InvalidEmailException {

        if(audioUrl == null || senderEmail == null || receiverEmail == null) {
            return null;
        }

        Message message = MessageManager.getInstance(context).getMessageByIdOrServerId(db, 0, serverId, true);
        if(message != null) {
            return message;
        }

        // add contact if necessary
        String rawContactKey = senderEmail + receiverEmail;
        if(!RAW_CONTACT_CACHE.containsKey(rawContactKey)) {
            String[] names = Utils.getFirstAndLastNames(senderName);
            RAW_CONTACT_CACHE.put(rawContactKey, ContactManager.getInstance().insertOrUpdate(context, 0, 0, names[0], names[1], null, senderEmail, null, receiverEmail, true));
        }
        ContactRaw contactRaw = RAW_CONTACT_CACHE.get(rawContactKey);

        db.beginTransaction();

        try {
            // insert chat and recipient
            Chat chat = insertOrUpdateTimestampChatAndRecipient(context, db, createdTs, contactRaw);

            // insert recording
            Recording recording = new Recording(null, durationSeconds * 1000L, 0, false, Recording.CONTENT_TYPE_AUDIO);
            recording.setTranscription(transcription);
            recording.setRecordedTimestamp(createdTs);
            RecordingManager.getInstance().insert(db, recording);

            // insert message
            Recipient recipient = null;
            List<Recipient> recipientList = chat.getRecipientList();
            int recipientAmount = recipientList.size();
            for (int i = 0; i < recipientAmount && recipient == null; i++) {
                if (recipientList.get(i).getVia().compareTo(senderEmail) == 0) {
                    recipient = recipientList.get(i);
                }
            }

            message = new Message(0, chat.getId(), recording.getId(), recipient.getId(), null, null,
                    createdTs, true, false, readTimestamp != null, serverId, audioUrl, null);
            message.setChatParameter(chat);
            message.setRecordingParameter(recording);
            MessageManager.getInstance(context).insert(db, message);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return message;
    }

    public static Message insertSentMessage(final Context context, final SQLiteDatabase db, String receiverName, String receiverEmail,
                                            String senderEmail, String audioUrl,
                                            String serverId, String transcription, String createdTs,
                                            int durationSeconds) throws SQLException, ContactManager.InvalidPhoneException, ContactManager.InvalidNameException, ContactManager.InvalidEmailException {

        if(audioUrl == null || senderEmail == null || receiverEmail == null) {
            return null;
        }

        Message message = MessageManager.getInstance(context).getMessageByIdOrServerId(db, 0, serverId, false);
        if(message != null) {
            return message;
        }

        // add contact if necessary
        String rawContactKey = senderEmail + receiverEmail;
        if(!RAW_CONTACT_CACHE.containsKey(rawContactKey)) {
            String[] names = Utils.getFirstAndLastNames(receiverName);
            RAW_CONTACT_CACHE.put(rawContactKey, ContactManager.getInstance().insertOrUpdate(context, 0, 0, names[0], names[1], null, receiverEmail, null, senderEmail, true));
        }
        ContactRaw contactRaw = RAW_CONTACT_CACHE.get(rawContactKey);

        db.beginTransaction();

        try {
            // insert chat and recipient
            Chat chat = insertOrUpdateTimestampChatAndRecipient(context, db, createdTs, contactRaw);

            // insert recording
            Recording recording = new Recording(null, durationSeconds * 1000L, 0, false, Recording.CONTENT_TYPE_AUDIO);
            recording.setTranscription(transcription);
            recording.setRecordedTimestamp(createdTs);
            RecordingManager.getInstance().insert(db, recording);

            // insert message
            List<Long> recipientIds = new ArrayList<>();
            List<Recipient> recipientList = chat.getRecipientList();
            for (Recipient recipient : recipientList) {
                recipientIds.add(recipient.getId());
            }

            message = new Message(0, chat.getId(), recording.getId(), 0, null, null,
                    createdTs, false, true, false, serverId, audioUrl, null);
            message.setRecipientIds(recipientIds);
            message.setConfirmedSentRecipientIds(recipientIds);
            message.setChatParameter(chat);
            message.setRecordingParameter(recording);
            MessageManager.getInstance(context).insert(db, message);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return message;
    }

    public static Message insertNotSentMessage(final Context context, final Chat chat, final Recording recording) throws SQLException {

        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        Message message = null;

        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        databaseHelper.lock();
        db.beginTransaction();

        try {
            boolean sendingToPeppermintSupport = true;

            // insert recipients
            for(Recipient recipient : chat.getRecipientList()) {
                sendingToPeppermintSupport = sendingToPeppermintSupport && recipient.getVia().compareToIgnoreCase(context.getString(R.string.support_email)) == 0;
                RecipientManager.getInstance(context).insertOrUpdate(db, recipient);
            }

            // insert chat
            chat.setLastMessageTimestamp(recording.getRecordedTimestamp());
            ChatManager.getInstance(context).insertOrUpdate(db, chat);

            // insert recording
            RecordingManager.getInstance().insertOrUpdate(db, recording);

            // insert message
            message = new Message(0, chat.getId(), recording.getId(), 0,
                    sendingToPeppermintSupport ? context.getString(R.string.support_audio_subject) : context.getString(R.string.sender_default_mail_subject),
                    context.getString(R.string.sender_default_message),
                    DateContainer.getCurrentUTCTimestamp(),
                    false, false, false, null, null, null);
            message.setRecipientIds(chat.getRecipientListIds());
            message.setChatParameter(chat);
            message.setRecordingParameter(recording);
            MessageManager.getInstance(context).insert(db, message);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            databaseHelper.unlock();
        }

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
    public static Chat insertOrUpdateTimestampChatAndRecipient(Context context, SQLiteDatabase db, String newTimestamp, ContactRaw... contactRaw) throws SQLException {
        Chat newChat;

        db.beginTransaction();

        try {
            StringBuilder chatTitleBuilder = new StringBuilder();
            List<Recipient> recipientList = new ArrayList<>();

            // update/insert recipient data
            for (int i = 0; i < contactRaw.length; i++) {
                if (i > 0) {
                    chatTitleBuilder.append(", ");
                }

                Recipient recipient = null;
                Cursor cursor = RecipientManager.getInstance(context).getByViaAndMimetype(db, contactRaw[i].getMainDataVia(), contactRaw[i].getMainDataMimetype());
                if (cursor.moveToNext()) {
                    recipient = RecipientManager.getInstance(context).getFromCursor(db, cursor);
                    recipient.setFromDroidContactRaw(contactRaw[i]);
                }
                cursor.close();

                if (recipient == null) {
                    recipient = new Recipient(contactRaw[i], DateContainer.getCurrentUTCTimestamp());
                    RecipientManager.getInstance(context).insert(db, recipient);
                } else {
                    recipient.setPeppermint(contactRaw[i].getPeppermint() != null && recipient.getVia().compareTo(contactRaw[i].getPeppermint().getVia()) == 0);
                    RecipientManager.getInstance(context).update(db, recipient);
                }

                recipientList.add(recipient);

                final String chatTitle = recipient.getDisplayName() == null ? recipient.getVia() : recipient.getDisplayName();
                chatTitleBuilder.append(chatTitle);
            }

            newChat = new Chat(0, chatTitleBuilder.toString(), newTimestamp, recipientList);

            // try to find already existent chat
            Chat foundChat = ChatManager.getInstance(context).getChatByRecipients(db, recipientList);
            if (foundChat != null) {
                newChat.setId(foundChat.getId());
                // if found, update the last message timestamp
                if (foundChat.getLastMessageTimestamp() != null && foundChat.getLastMessageTimestamp().compareToIgnoreCase(newTimestamp) > 0) {
                    newChat.setLastMessageTimestamp(foundChat.getLastMessageTimestamp());
                }
                ChatManager.getInstance(context).update(db, newChat);
            } else {
                // otherwise, just insert
                ChatManager.getInstance(context).insert(db, newChat);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return newChat;
    }

    public static void markAsPeppermint(Context context, Recipient recipient, String account) throws ContactManager.InvalidEmailException, SQLException {
        ContactRaw contactRaw = ContactManager.getInstance().getRawContactByDataId(context, recipient.getDroidContactDataId());
        if(contactRaw == null) {
            /* in case the contact has been deleted... */
            String[] names = Utils.getFirstAndLastNames(recipient.getDisplayName());
            String phone = recipient.getMimeType().compareTo(ContactData.PHONE_MIMETYPE) == 0 ? recipient.getVia() : null;
            String email = phone == null ? recipient.getVia() : null;
            Uri photoUri = recipient.getPhotoUri() != null ? Uri.parse(recipient.getPhotoUri()) : null;
            try {
                contactRaw = ContactManager.getInstance().insertOrUpdate(context, 0, 0, names[0], names[1], phone, email, photoUri, account, true);
                recipient.setDroidContactId(contactRaw.getContactId());
                recipient.setDroidContactRawId(contactRaw.getRawId());
                recipient.setDroidContactDataId(contactRaw.getMainDataId());
            } catch (Exception e) {
                TrackerManager.getInstance(context.getApplicationContext()).log("Unable to insert missing contact while marking as Peppermint!", e);
            }
        } else {
            ContactManager.getInstance().insertPeppermint(context, recipient.getVia(), recipient.getDroidContactRawId(), 0, null);
        }
        recipient.setPeppermint(true);

        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        databaseHelper.lock();
        RecipientManager.getInstance(context).update(databaseHelper.getWritableDatabase(), recipient);
        databaseHelper.unlock();
    }

    public static void unmarkAsPeppermint(Context context, Recipient recipient) throws SQLException {
        ContactManager.getInstance().deletePeppermint(context, recipient.getDroidContactRawId(), null);
        recipient.setPeppermint(false);

        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        databaseHelper.lock();
        RecipientManager.getInstance(context).update(databaseHelper.getWritableDatabase(), recipient);
        databaseHelper.unlock();
    }

    public static void markAsPlayed(Context context, Message message) throws SQLException {
        boolean originalValue = message.isPlayed();
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        databaseHelper.lock();
        try {
            message.setPlayed(true);
            MessageManager.getInstance(context).update(db, message);

            // tell the backend that the message has been played
            MessagesMarkPlayedTask task = new MessagesMarkPlayedTask(context, new Message(message), null);
            task.execute((Void) null);

        } catch (Throwable e) {
            message.setPlayed(originalValue);
            databaseHelper.unlock();
            throw e;
        }
        databaseHelper.unlock();
    }

    public static void unmarkAsPlayed(Context context, Message message) throws SQLException {
        boolean originalValue = message.isPlayed();
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        databaseHelper.lock();
        try {
            message.setPlayed(false);
            MessageManager.getInstance(context).update(db, message);
        } catch (Throwable e) {
            message.setPlayed(originalValue);
            databaseHelper.unlock();
            throw e;
        }
        databaseHelper.unlock();
    }

    public static void deleteMessageAndRecording(final Context context, Message message) throws SQLException {
        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        databaseHelper.lock();
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // delete the local file
            if(message.getRecordingParameter() != null) {
                File file = message.getRecordingParameter().getValidatedFile();
                if(file != null) {
                    if(!file.delete()) {
                        TrackerManager.getInstance(context.getApplicationContext()).log("Unable to delete file " + file.getAbsolutePath());
                    }
                }
            }
            // discard recording as well
            RecordingManager.getInstance().delete(db, message.getRecordingId());

            MessageManager.getInstance(context).delete(db, message.getId());

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            databaseHelper.unlock();
        }
    }

    public static Chat insertOrUpdateChatAndRecipients(Context context, ContactRaw... contactRaws) throws SQLException {
        if(contactRaws == null || contactRaws.length <= 0) {
            throw new IllegalArgumentException("Must supply at least one ContactRaw");
        }

        final String lastTimestamp = DateContainer.getCurrentUTCTimestamp();
        final List<Recipient> recipientList = new ArrayList<>();
        for(ContactRaw contactRaw : contactRaws) {
            recipientList.add(new Recipient(contactRaw, lastTimestamp));
        }

        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);

        Chat tappedChat = ChatManager.getInstance(context).getChatByRecipients(databaseHelper.getReadableDatabase(), recipientList);
        if(tappedChat == null) {
            tappedChat = new Chat(recipientList, lastTimestamp);

            databaseHelper.lock();
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                // create the recipients if non-existent
                for(Recipient recipient : recipientList) {
                    RecipientManager.getInstance(context).insertOrUpdate(db, recipient);
                }
                // create the chat instance if non-existent
                ChatManager.getInstance(context).insert(db, tappedChat);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
                databaseHelper.unlock();
            }
        }

        return tappedChat;
    }
}
