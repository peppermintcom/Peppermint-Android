package com.peppermint.app.data;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;

import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Created by Nuno Luz on 17-02-2016.
 */
public class RecipientManager {

    public static class InvalidNameException extends Exception {
    }

    public static class InvalidViaException extends Exception {
    }

    public static class InvalidEmailException extends InvalidViaException {
    }

    public static class InvalidPhoneException extends InvalidViaException {
    }

    private static final String FILE_SCHEME = "file:/";
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";

    private static final String FIELD_DISPLAY_NAME = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
            ContactsContract.Contacts.DISPLAY_NAME;

    private static final String[] PROJECTION = {
            ContactsContract.Data._ID,
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.Data.STARRED,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
            FIELD_DISPLAY_NAME,
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE
    };

    private static final String PEPPERMINT_GROUP_TITLE = "Peppermint";

    public static final String CONTENT_TYPE = "com.peppermint.app.cursor.item/contact_v1";

    private static Map<Long, WeakReference<Recipient>> mRecipientCache = new WeakHashMap<>();

    /**
     * Gets the data inside the Cursor's current position and puts it in an instance of the
     * Recipient structure.
     *
     * @param cursor the cursor
     * @return the Recipient instance
     */
    public static Recipient getRecipientFromCursor(Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndex(FIELD_DISPLAY_NAME));
        String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_URI));

        String accountName = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME));
        String accountType = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));

        Contact contact = getContactFromCursor(cursor);
        Recipient recipient = new Recipient(contact.getRawId(), false, accountType, accountName, name, photoUri);

        if(contact.isEmail()) {
            recipient.setEmail(contact);
            return recipient;
        }
        if(contact.isPhone()) {
            recipient.setPhone(contact);
            return recipient;
        }
        if(contact.isPeppermint()) {
            recipient.setPeppermint(contact);
            return recipient;
        }

        return recipient;
    }

    public static Contact getContactFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID));
        long rawId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
        boolean starred = cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.STARRED)) != 0;
        String mime = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
        String via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1));
        return new Contact(id, rawId, starred, mime, via);
    }

    /**
     * Get the recipient with the supplied "via" value from the local database.
     * @param context the app context
     * @param via the via value
     * @return the recipient instance with all data
     */
    public static Recipient getRecipientByViaOrId(Context context, String via, long contactId) {
        if(contactId > 0 && mRecipientCache.containsKey(contactId)) {
            WeakReference<Recipient> ref = mRecipientCache.get(contactId);
            Recipient recipient = ref.get();
            if(recipient == null) {
                mRecipientCache.remove(contactId);
            } else {
                return recipient;
            }
        }

        Recipient result = null;

        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null,
                Utils.joinString(" OR ", via != null ?
                        ContactsContract.Data.DATA1 + " = " + DatabaseUtils.sqlEscapeString(via) : null, contactId > 0 ?
                        ContactsContract.Data._ID + " = " + contactId : null),
                null, null);

        if(cursor.moveToNext()) {
            result = getRecipientFromCursor(cursor);
        }
        cursor.close();

        if(result != null) {
            mRecipientCache.put(result.getContactId(), new WeakReference<>(result));
        }

        return result;
    }

    /**
     * Obtains the recipient with the supplied ID from the database.
     *
     * @param context the app context
     * @param contactId the recipient ID
     * @return the recipient instance with all data
     */
    public static Recipient getRecipientById(Context context, long contactId) {
        if(mRecipientCache.containsKey(contactId)) {
            WeakReference<Recipient> ref = mRecipientCache.get(contactId);
            Recipient recipient = ref.get();
            if(recipient == null) {
                mRecipientCache.remove(contactId);
            } else {
                return recipient;
            }
        }

        Recipient result = null;

        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null,
                ContactsContract.Data._ID + " = " + contactId,
                null, null);

        if(cursor.moveToNext()) {
            result = getRecipientFromCursor(cursor);
        }
        cursor.close();

        if(result != null) {
            mRecipientCache.put(result.getContactId(), new WeakReference<>(result));
        }

        return result;
    }

    public static Recipient getRecipientMainEmail(Context context, long rawId) {
        Recipient result = null;

        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null,
                ContactsContract.Data.MIMETYPE + "=" + DatabaseUtils.sqlEscapeString(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE) +
                        " AND " + ContactsContract.Data.RAW_CONTACT_ID + "=" + rawId,
                null, ContactsContract.Data.IS_PRIMARY + " DESC");

        if(cursor.moveToNext()) {
            result = getRecipientFromCursor(cursor);
        }
        cursor.close();

        if(result != null) {
            mRecipientCache.put(result.getContactId(), new WeakReference<>(result));
        }

        return result;
    }

    public static Cursor getPhone(Context context, long rawId, String phone) {
        List<Long> rawIds = new ArrayList<>();
        rawIds.add(rawId);
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        return get(context, rawIds, mimeTypes, phone);
    }

    public static Cursor getEmail(Context context, long rawId, String email) {
        List<Long> rawIds = new ArrayList<>();
        rawIds.add(rawId);
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        return get(context, rawIds, mimeTypes, email);
    }

    public static Cursor getPeppermint(Context context, long rawId) {
        List<Long> rawIds = new ArrayList<>();
        rawIds.add(rawId);
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        return get(context, rawIds, mimeTypes, null);
    }

    public static Cursor get(Context context, List<Long> allowedRawIds, List<String> allowedMimeTypes, String viaSearch) {
        List<String> args = new ArrayList<>();
        String condViaSearch = (viaSearch == null ? "" : " AND (LOWER(REPLACE(" + ContactsContract.Data.DATA1 + ", ' ', '')) LIKE " +
                DatabaseUtils.sqlEscapeString(viaSearch + "%") + ")");
        String condMimeTypes = Utils.getSQLConditions(ContactsContract.Data.MIMETYPE, allowedMimeTypes, args, false);
        String condIds = Utils.getSQLConditions(ContactsContract.Data.RAW_CONTACT_ID, allowedRawIds, null, false);

        return context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION,
                "1" + condViaSearch + " AND (" + condMimeTypes + ")" + " AND (" + condIds + ")",
                args.toArray(new String[args.size()]), FIELD_DISPLAY_NAME + " COLLATE NOCASE");
    }

    /**
     * Obtains a list of all recipients found in the Android contacts database according to
     * the given restrictions.
     *
     * @param context the context
     * @param allowedIds the allowed ids filter
     * @param freeTextSearch the via/display name free text filter
     * @param areStarred the starred/favorite filter
     * @param allowedMimeTypes the allowed mime types
     * @return the result cursor
     */
    public static Cursor get(final Context context, List<Long> allowedIds, String freeTextSearch, Boolean areStarred, List<String> allowedMimeTypes, String enforcedViaSearch) {
        List<String> args = new ArrayList<>();
        String condStarred = (areStarred == null ? "" : " AND " + ContactsContract.Contacts.STARRED + "=" + (areStarred ? "1" : "0"));
        String condFreeSearch = (freeTextSearch == null ? "" : " AND (LOWER(" + FIELD_DISPLAY_NAME + ") LIKE " + DatabaseUtils.sqlEscapeString(freeTextSearch + "%") + " OR LOWER(" + FIELD_DISPLAY_NAME + ") LIKE " + DatabaseUtils.sqlEscapeString("% " + freeTextSearch + "%") + " OR LOWER(REPLACE(" + ContactsContract.Data.DATA1 + ", ' ', '')) LIKE " + DatabaseUtils.sqlEscapeString(freeTextSearch + "%") + ")");
        String condViaSearch = (enforcedViaSearch == null ? "" : " AND (LOWER(REPLACE(" + ContactsContract.Data.DATA1 + ", ' ', '')) LIKE " + DatabaseUtils.sqlEscapeString(enforcedViaSearch + "%") + ")");
        String condMimeTypes = Utils.getSQLConditions(ContactsContract.Data.MIMETYPE, allowedMimeTypes, args, false);
        String condIds = Utils.getSQLConditions(ContactsContract.Data._ID, allowedIds, null, false);

        Cursor rootCursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION,
                "1" + condStarred + condFreeSearch + condViaSearch + " AND (" + condMimeTypes + ")" +
                        " AND (" + condIds + ")",
                args.toArray(new String[args.size()]), FIELD_DISPLAY_NAME + " COLLATE NOCASE");

        if(allowedIds != null) {
            return rootCursor;
        }

        return new FilteredCursor(rootCursor, new FilteredCursor.Filter() {
            private Set<String> mViaSet = new HashSet<>();

            @Override
            public boolean isValid(Cursor cursor) {
                // removes duplicate contacts
                String via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)).trim().toLowerCase() +
                        cursor.getString(cursor.getColumnIndex(FIELD_DISPLAY_NAME)).replaceAll("\\s+", "").toLowerCase();

                if (!mViaSet.contains(via)) {
                    mViaSet.add(via);
                    return true;
                }

                return false;
            }
        });
    }

    public static long insertOrUpdatePhoto(Context context, Uri photoUri, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) {
        int dp200 = Utils.dpToPx(context, 200);
        int dp100 = Utils.dpToPx(context, 100);

        // scale image to 200dp to save memory
        Bitmap realImage = Utils.getScaledBitmap(context, photoUri, dp200, dp200);

        if(realImage != null) {
            ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

            // cut a square thumbnail
            Bitmap rotatedImage = ThumbnailUtils.extractThumbnail(realImage, dp100, dp100, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            if(FILE_SCHEME.startsWith(photoUri.getScheme())) {
                // rotate image according to the photo params
                rotatedImage = Utils.getRotatedBitmapFromFileAttributes(rotatedImage, photoUri.toString().substring(6));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            rotatedImage.compress(Bitmap.CompressFormat.PNG, 100, baos);

            ContentProviderOperation.Builder op = null;
            if (rawId <= 0) {
                op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIdBackRef);
            } else {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                                new String[]{String.valueOf(rawId), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE})
                        .build());

                op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
            }

            ops.add(op.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, baos.toByteArray()).build());

            if(operationsList == null) {
                // no caller operations list, so execute the operations right away
                ContentProviderResult[] res = executeOperations(context, ops);
                return ContentUris.parseId(res[res.length-1].uri);
            }

            return 0;
        }

        return -1;
    }

    public static long insertPhone(Context context, String phone, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) throws InvalidPhoneException {
        if(!Utils.isValidPhoneNumber(phone)) {
            throw new InvalidPhoneException();
        }

        boolean alreadyHasPhone = false;

        if(rawId > 0) {
            Cursor checkCursor = getPhone(context, rawId, phone);
            alreadyHasPhone = checkCursor != null && checkCursor.getCount() > 0;
        }

        if(!alreadyHasPhone) {
            ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

            ContentProviderOperation.Builder op = null;
            if (rawId <= 0) {
                op = ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIdBackRef);
            } else {
                op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
            }

            // not including phone type will crash on HTC devices
            ops.add(op.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.Data.DATA1, phone).build());

            if(operationsList == null) {
                // no caller operations list, so execute the operations right away
                ContentProviderResult[] res = executeOperations(context, ops);
                return ContentUris.parseId(res[0].uri);
            }

            return 0;
        }

        return -1;
    }

    public static long insertEmail(Context context, String email, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) throws InvalidEmailException {
        if(!Utils.isValidEmail(email)) {
            throw new InvalidEmailException();
        }

        boolean alreadyHasEmail = false;

        if(rawId > 0) {
            Cursor checkCursor = getEmail(context, rawId, email);
            alreadyHasEmail = checkCursor != null && checkCursor.getCount() > 0;
        }

        if(!alreadyHasEmail) {
            ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

            ContentProviderOperation.Builder op = null;
            if (rawId <= 0) {
                op = ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIdBackRef);
            } else {
                op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
            }

            ops.add(op.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.Data.DATA1, email).build());

            if(operationsList == null) {
                // no caller operations list, so execute the operations right away
                ContentProviderResult[] res = executeOperations(context, ops);
                return ContentUris.parseId(res[0].uri);
            }

            return 0;
        }

        return -1;
    }

    public static long insertOrUpdateName(Context context, String firstName, String lastName, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) throws InvalidNameException {
        // validate display name
        String fullName = (firstName + " " + lastName).trim();
        if(!Utils.isValidName(fullName)) {
            throw new InvalidNameException();
        }

        ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

        ContentProviderOperation.Builder op = null;
        if(rawId <= 0) {
            op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIdBackRef);
        } else {
            ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                            new String[]{String.valueOf(rawId), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
                    .build());

            op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
        }
        ops.add(op.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName).build());

        if(operationsList == null) {
            // no caller operations list, so execute the operations right away
            ContentProviderResult[] res = executeOperations(context, ops);
            return ContentUris.parseId(res[res.length-1].uri);
        }

        return 0;
    }

    public static long insertPeppermint(Context context, String email, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) throws InvalidEmailException {
        boolean alreadyHasPeppermint = false;

        if(rawId > 0) {
            Cursor checkCursor = getPeppermint(context, rawId);
            alreadyHasPeppermint = checkCursor != null && checkCursor.getCount() > 0;
        }

        if(!alreadyHasPeppermint) {
            ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

            ContentProviderOperation.Builder op = null;
            if (rawId <= 0) {
                op = ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIdBackRef);
            } else {
                op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
            }

            ops.add(op.withValue(ContactsContract.Data.MIMETYPE, CONTENT_TYPE).withValue(ContactsContract.Data.DATA1, email).build());

            if(operationsList == null) {
                // no caller operations list, so execute the operations right away
                ContentProviderResult[] res = executeOperations(context, ops);
                return ContentUris.parseId(res[0].uri);
            }

            return 0;
        }

        return -1;
    }

    public static long insertRaw(Context context, String googleAccountName, ArrayList<ContentProviderOperation> operationsList) {
        ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, GOOGLE_ACCOUNT_TYPE)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, googleAccountName).build());

        // check if Peppermint group exists; create it if not
        // we could add to the default group "My Contacts" but it might not exist
        // also, the default operation doesn't necessarily add the contact to a group
        // causing the GROUP_VISIBLE flag to be 0
        Long groupId = null;
        Cursor groupCursor = context.getContentResolver().query(
                ContactsContract.Groups.CONTENT_URI,
                new String[]{ContactsContract.Groups._ID},
                ContactsContract.Groups.GROUP_VISIBLE + "=1 AND " + ContactsContract.Groups.ACCOUNT_NAME + "=" + DatabaseUtils.sqlEscapeString(googleAccountName) +
                        " AND " + ContactsContract.Groups.TITLE + "=" + DatabaseUtils.sqlEscapeString(PEPPERMINT_GROUP_TITLE), null,
                null
        );
        if(groupCursor.moveToNext()) {
            groupId = groupCursor.getLong(groupCursor.getColumnIndex(ContactsContract.Groups._ID));
        } else {
            ContentValues groupValues = new ContentValues();
            groupValues.put(ContactsContract.Groups.TITLE, PEPPERMINT_GROUP_TITLE);
            groupValues.put(ContactsContract.Groups.GROUP_VISIBLE, 1);
            groupValues.put(ContactsContract.Groups.ACCOUNT_NAME, googleAccountName);
            groupValues.put(ContactsContract.Groups.ACCOUNT_TYPE, GOOGLE_ACCOUNT_TYPE);
            Uri groupUri = context.getContentResolver().insert(ContactsContract.Groups.CONTENT_URI, groupValues);
            groupId = ContentUris.parseId(groupUri);
        }
        groupCursor.close();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.DATA1, groupId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE).build());

        if(operationsList == null) {
            // no caller operations list, so execute the operations right away
            ContentProviderResult[] res = executeOperations(context, ops);
            return ContentUris.parseId(res[0].uri);
        }

        return 0;
    }

    /**
     * Inserts a new contact using the global contacts content provider.<br />
     * If rawId is supplied, will update the contact with the supplied information.<br />
     * One of email or phone is mandatory.
     *
     * @param context the context
     * @param rawId the already existent rawId (0 for a new contact)
     * @param firstName the contact given name (mandatory one of firstName or lastName)
     * @param lastName the contact family name
     * @param phone the phone number
     * @param email the email address
     * @param photoUri the contact photo URI
     * @param googleAccountName the google account name to insert the contact
     * @return a {@link Bundle} with results (can be passed on to an {@link Intent}
     */
    public static Recipient insert(Context context, long rawId, String firstName, String lastName, String phone, String email, Uri photoUri, String googleAccountName, boolean hasPeppermint) throws InvalidNameException, InvalidEmailException, InvalidPhoneException {
        firstName = firstName == null ? "" : Utils.capitalizeFully(firstName.trim());
        lastName = lastName == null ? "" : Utils.capitalizeFully(lastName.trim());
        phone = phone == null ? "" : phone.trim();
        email = email == null ? "" : email.trim();

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // create raw contact
        if(rawId <= 0) {
            insertRaw(context, googleAccountName, ops);
        }

        insertOrUpdateName(context, firstName, lastName, rawId, 0, ops);

        if(email != null && email.length() > 0) {
            insertEmail(context, email, rawId, 0, ops);
        }

        if(phone != null && phone.length() > 0) {
            insertPhone(context, phone, rawId, 0, ops);
        }

        if(photoUri != null) {
            insertOrUpdatePhoto(context, photoUri, rawId, 0, ops);
        }

        if(hasPeppermint) {
            insertPeppermint(context, email, rawId, 0, ops);
        }

        executeOperations(context, ops);

        Recipient recipient = null;
        Cursor cursor = getByEmailOrPhone(context, email, phone, googleAccountName);
        while(cursor.moveToNext() && (recipient == null || recipient.getRawId() != rawId)) {
            recipient = getRecipientFromCursor(cursor);
        }
        cursor.close();

        return recipient;
    }

    /**
     * Inserts a new contact using the global contacts content provider.<br />
     * If rawId is supplied, will update the contact with the supplied information.<br />
     * One of email or phone is mandatory.
     *
     * @param context the context
     * @param rawId the already existent rawId (0 for a new contact)
     * @param firstName the contact given name (mandatory one of firstName or lastName)
     * @param lastName the contact family name
     * @param phone the phone number
     * @param email the email address
     * @param photoUri the contact photo URI
     * @param googleAccountName the google account name to insert the contact
     * @return a {@link Bundle} with results (can be passed on to an {@link Intent}
     */
    public static Recipient insertOrUpdateRecipient(Context context, long rawId, String firstName, String lastName, String phone, String email, Uri photoUri, String googleAccountName, boolean hasPeppermint) throws InvalidPhoneException, InvalidNameException, InvalidEmailException {
        // try to find the rawId by email or phone
        if(rawId <= 0) {
            Cursor cursor = getByEmailOrPhone(context, email, phone, googleAccountName);
            if(cursor.moveToNext()) {
                rawId = getRecipientFromCursor(cursor).getRawId();
            }
            cursor.close();
        }

        return insert(context, rawId, firstName, lastName, phone, email, photoUri, googleAccountName, hasPeppermint);
    }

    public static Cursor getByEmailOrPhone(Context context, String email, String phone, String googleAccountName) {
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                PROJECTION,
                Utils.joinString(" AND ",
                    Utils.joinString(" OR ",
                            email != null ? ContactsContract.Data.DATA1 + " = " + DatabaseUtils.sqlEscapeString(email) : null,
                            phone != null ? ContactsContract.Data.DATA1 + " = " + DatabaseUtils.sqlEscapeString(phone) : null),
                    Utils.joinString(" OR ",
                            ContactsContract.Data.MIMETYPE + "=" + DatabaseUtils.sqlEscapeString(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),
                            ContactsContract.Data.MIMETYPE + "=" + DatabaseUtils.sqlEscapeString(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)),
                    ContactsContract.RawContacts.ACCOUNT_TYPE + "=" + DatabaseUtils.sqlEscapeString(GOOGLE_ACCOUNT_TYPE),
                    ContactsContract.RawContacts.ACCOUNT_NAME + "=" + DatabaseUtils.sqlEscapeString(googleAccountName)),
                null, null);
        return cursor;
    }

    private static ContentProviderResult[] executeOperations(Context context, ArrayList<ContentProviderOperation> operations) {
        try {
            ContentProviderResult[] res = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
            if(res.length < operations.size()) {
                throw new RuntimeException("Not all contact operations were performed: Total Ops = " + operations.size() + "; Performed = " + res.length);
            }
            return res;
        } catch (Throwable e) {
            TrackerManager.getInstance(context.getApplicationContext()).logException(e);
        }
        return null;
    }

}
