package com.peppermint.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Database operations for {@link Recipient}
 */
public class RecipientManager {

    private static final String TAG = RecipientManager.class.getSimpleName();

    public static Recipient getRecipientFromCursor(Cursor cursor) {
        Recipient recipient = new Recipient();

        recipient.setId(cursor.getLong(cursor.getColumnIndex("recipient_id")));
        recipient.setDroidContactDataId(cursor.getLong(cursor.getColumnIndex("droid_contact_data_id")));
        recipient.setDroidContactRawId(cursor.getLong(cursor.getColumnIndex("droid_contact_raw_id")));
        recipient.setDroidContactId(cursor.getLong(cursor.getColumnIndex("droid_contact_id")));
        recipient.setDisplayName(cursor.getString(cursor.getColumnIndex("display_name")));
        recipient.setVia(cursor.getString(cursor.getColumnIndex("via")));
        recipient.setMimeType(cursor.getString(cursor.getColumnIndex("mimetype")));
        recipient.setPhotoUri(cursor.getString(cursor.getColumnIndex("photo_uri")));
        recipient.setAddedTimestamp(cursor.getString(cursor.getColumnIndex("added_ts")));
        recipient.setPeppermint(cursor.getInt(cursor.getColumnIndex("is_peppermint")) > 0);

        return recipient;
    }

    public static List<Recipient> getByChatId(SQLiteDatabase db, long chatId) {
        List<Recipient> recipients = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT tbl_recipient.* FROM tbl_chat_recipient, tbl_recipient WHERE tbl_chat_recipient.recipient_id = tbl_recipient.recipient_id AND tbl_chat_recipient.chat_id = " + chatId + ";", null);
        while(cursor.moveToNext()) {
            recipients.add(getRecipientFromCursor(cursor));
        }
        cursor.close();

        return recipients;
    }

    public static Cursor getAll(SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM tbl_recipient;", null);
    }

    public static Cursor getByViaAndMimetype(SQLiteDatabase db, String via, String mimetype) {
        return db.rawQuery("SELECT * FROM tbl_recipient WHERE via = " + DatabaseUtils.sqlEscapeString(via) + " AND mimetype = " + DatabaseUtils.sqlEscapeString(mimetype) + ";", null);
    }

    private static Cursor getByDroidIds(SQLiteDatabase db, long droidContactId, long droidContactRawId, long droidContactDataId) {
        return db.rawQuery("SELECT * FROM tbl_recipient WHERE droid_contact_id = " + droidContactId + " AND droid_contact_raw_id = " + droidContactRawId + " AND droid_contact_data_id = " + droidContactDataId + ";", null);
    }

    // OPERATIONS
    public static Recipient insert(Context context, SQLiteDatabase db, Recipient recipient) throws SQLException {
        if(recipient.getVia() == null || recipient.getMimeType() == null) {
            throw new IllegalArgumentException("Invalid recipient! " + recipient.toString());
        }

        String photoUri = recipient.getPhotoUri();
        if(photoUri != null && !photoUri.endsWith(".peppermintAv")) {
            Uri uri = ResourceUtils.copyImageToLocalDir(context, Uri.parse(photoUri), Utils.normalizeAndCleanString(recipient.getVia() + "_" + recipient.getAddedTimestamp()) + ".peppermintAv");
            photoUri = uri == null ? null : uri.toString();
        }

        ContentValues cv = new ContentValues();
        cv.put("droid_contact_data_id", recipient.getDroidContactDataId());
        cv.put("droid_contact_raw_id", recipient.getDroidContactRawId());
        cv.put("droid_contact_id", recipient.getDroidContactId());
        cv.put("display_name", recipient.getDisplayName());
        cv.put("via", recipient.getVia());
        cv.put("mimetype", recipient.getMimeType());
        cv.put("photo_uri", photoUri);
        cv.put("added_ts", recipient.getAddedTimestamp());
        cv.put("is_peppermint", recipient.isPeppermint() ? 1 : 0);

        long id = db.insert("tbl_recipient", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert recipient!");
        }

        recipient.setId(id);

        Log.d(TAG, "INSERTED " + recipient);

        return recipient;
    }

    public static void update(SQLiteDatabase db, Recipient recipient) throws SQLException {
        if(recipient.getVia() == null || recipient.getMimeType() == null) {
            throw new IllegalArgumentException("Invalid recipient! " + recipient.toString());
        }

        ContentValues cv = new ContentValues();
        cv.put("droid_contact_data_id", recipient.getDroidContactDataId());
        cv.put("droid_contact_raw_id", recipient.getDroidContactRawId());
        cv.put("droid_contact_id", recipient.getDroidContactId());
        cv.put("display_name", recipient.getDisplayName());
        cv.put("via", recipient.getVia());
        cv.put("mimetype", recipient.getMimeType());
        cv.put("photo_uri", recipient.getPhotoUri());
        cv.put("added_ts", recipient.getAddedTimestamp());
        cv.put("is_peppermint", recipient.isPeppermint() ? 1 : 0);

        long id = db.update("tbl_recipient", cv, "recipient_id = " + recipient.getId(), null);
        if(id < 0) {
            throw new SQLException("Unable to update recipient!");
        }

        Log.d(TAG, "UPDATED " + recipient);
    }

    public static Recipient insertOrUpdate(Context context, SQLiteDatabase db, Recipient recipient) throws  SQLException {
        if(recipient.getVia() == null || recipient.getMimeType() == null) {
            throw new IllegalArgumentException("Invalid recipient! " + recipient.toString());
        }

        if(recipient.getId() <= 0) {
            Recipient searchedRecipient = null;

            if(recipient.getDroidContactId() > 0 && recipient.getDroidContactRawId() > 0 && recipient.getDroidContactDataId() > 0) {
                Cursor cursor = getByDroidIds(db, recipient.getDroidContactId(), recipient.getDroidContactRawId(), recipient.getDroidContactDataId());
                if(cursor.moveToNext()) {
                    searchedRecipient = getRecipientFromCursor(cursor);
                }
                cursor.close();
            }

            if(searchedRecipient == null) {
                Cursor cursor = getByViaAndMimetype(db, recipient.getVia(), recipient.getMimeType());
                if(cursor.moveToNext()) {
                    searchedRecipient = getRecipientFromCursor(cursor);
                }
                cursor.close();
            }

            if (searchedRecipient == null) {
                return insert(context, db, recipient);
            }

            recipient.setId(searchedRecipient.getId());
        }

        update(db, recipient);

        return recipient;
    }

    public static void printAllData(Context context) {
        SQLiteDatabase db = DatabaseHelper.getInstance(context).getReadableDatabase();

        Cursor cursor = getAll(db);
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
            Log.d(TAG, "RECIPIENT # " + fieldsBuilder.toString());
        }
        cursor.close();
    }
}
