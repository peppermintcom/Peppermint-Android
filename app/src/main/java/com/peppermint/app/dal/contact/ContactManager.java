package com.peppermint.app.dal.contact;

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
import android.text.TextUtils;

import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nuno Luz on 17-02-2016.
 *
 * Database operations for {@link ContactRaw} and {@link ContactData}.
 * Data is obtained and stored through the {@link ContactsContract}.
 */
public class ContactManager {

    private static ContactManager INSTANCE;

    public static ContactManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ContactManager();
        }
        return INSTANCE;
    }

    protected ContactManager() {
        super();
    }

    private static final String FILE_SCHEME = "file:/";
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";

    public static final String FIELD_DISPLAY_NAME = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
            ContactsContract.Contacts.DISPLAY_NAME;

    private static final String[] PROJECTION = {
            ContactsContract.Data._ID,
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.STARRED,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
            FIELD_DISPLAY_NAME,
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
            ContactsContract.Data.IS_PRIMARY,
            ContactsContract.Data.IS_SUPER_PRIMARY,
            ContactsContract.Data.IN_VISIBLE_GROUP
    };

    private static final String SQL_VIA_CONDITION = "(LOWER(REPLACE(" + ContactsContract.Data.DATA1 + ", ' ', '')) LIKE %1$s AND " + ContactsContract.Data.MIMETYPE + " = " + DatabaseUtils.sqlEscapeString(ContactData.EMAIL_MIMETYPE) + ")";

    // prioritize visible and primary entries
    private static final String SQL_GENERAL_ORDER = ContactsContract.Data.IN_VISIBLE_GROUP + " DESC, " + ContactsContract.Data.IS_SUPER_PRIMARY + " DESC, " + ContactsContract.Data.IS_PRIMARY + " DESC";
    // prioritize peppermint contacts
    private static final String SQL_PEPPERMINT_ORDER = "(CASE WHEN " + ContactsContract.Data.RAW_CONTACT_ID + " IN (SELECT " + ContactsContract.Data.RAW_CONTACT_ID + " FROM view_data WHERE " + ContactsContract.Data.MIMETYPE + " = " + DatabaseUtils.sqlEscapeString(ContactData.PEPPERMINT_MIMETYPE) + ") THEN 1 ELSE 0 END) DESC, ";

    private static final String SQL_EMAIL_FIRST_ORDER = "(CASE WHEN " + ContactsContract.Data.MIMETYPE + " = " + DatabaseUtils.sqlEscapeString(ContactData.EMAIL_MIMETYPE) + " THEN 1 ELSE 0 END) DESC, ";

    private static final String PEPPERMINT_GROUP_TITLE = "Peppermint";


    /**
     * Gets the data inside the Cursor's current position and puts it in an instance of the
     * ContactRaw structure.
     * <strong>If context is not null, also retrieves the Peppermint contact data.</strong>
     *
     * @param cursor the cursor
     * @return the ContactRaw instance
     */
    public ContactRaw getRawContactFromCursor(Context context, Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndex(FIELD_DISPLAY_NAME));
        String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_URI));

        String accountName = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME));
        String accountType = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));

        ContactData contactData = getContactFromCursor(cursor);
        ContactRaw contactRaw = new ContactRaw(contactData.getRawId(), contactData.getContactId(), false, accountType, accountName, name, photoUri);
        contactRaw.setContactData(contactData);

        if(context != null) {
            contactRaw.setContactData(getPeppermintContactByContactIdAndVia(context, contactRaw.getContactId(), null));
        }

        return contactRaw;
    }

    public ContactData getContactFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID));
        long rawId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
        long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
        boolean starred = cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.STARRED)) != 0;
        String mime = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
        String via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1));
        return new ContactData(id, rawId, contactId, starred, mime, via);
    }

    public HashMap<Long, ContactData> getPeppermintContacts(Context context) {
        HashMap<Long, ContactData> map = new HashMap<>();

        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add(ContactData.PEPPERMINT_MIMETYPE);

        Cursor cursor = getRaw(context, null, null, mimeTypes, null);
        while(cursor.moveToNext()) {
            ContactData contact = getContactFromCursor(cursor);
            map.put(contact.getContactId(), contact);
        }
        cursor.close();

        return map;
    }

    public ContactData getPeppermintContactByContactIdAndVia(Context context, long contactId, String via) {
        List<Long> contactIds = new ArrayList<>();
        contactIds.add(contactId);
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add(ContactData.PEPPERMINT_MIMETYPE);

        ContactData data = null;
        Cursor cursor = getRaw(context, contactIds, null, mimeTypes, via);
        if(cursor != null) {
            if (cursor.moveToNext()) {
                data = getContactFromCursor(cursor);
            }
            cursor.close();
        }

        return data;
    }

    public ContactRaw getRawContactByVia(Context context, String via) {
        ContactRaw result = null;

        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null,
                ContactsContract.Data.DATA1 + " = " + DatabaseUtils.sqlEscapeString(via),
                null,  SQL_GENERAL_ORDER);

        if(cursor != null) {
            if (cursor.moveToNext()) {
                result = getRawContactFromCursor(context, cursor);
            }
            cursor.close();
        }

        return result;
    }

    public ContactRaw getRawContactByDataId(Context context, long contactDataId) {
        ContactRaw result = null;

        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null,
                ContactsContract.Data._ID + " = " + contactDataId,
                null, null);

        if(cursor != null) {
            if (cursor.moveToNext()) {
                result = getRawContactFromCursor(context, cursor);
            }
            cursor.close();
        }

        return result;
    }

    public Cursor getPhone(Context context, long rawId, String phone) {
        List<Long> rawIds = new ArrayList<>();
        rawIds.add(rawId);
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        return getRaw(context, null, rawIds, mimeTypes, phone);
    }

    public Cursor getEmail(Context context, long rawId, String email) {
        List<Long> rawIds = new ArrayList<>();
        rawIds.add(rawId);
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        return getRaw(context, null, rawIds, mimeTypes, email);
    }

    public Cursor getPeppermintByRawId(Context context, long rawId) {
        List<Long> rawIds = new ArrayList<>();
        rawIds.add(rawId);
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add(ContactData.PEPPERMINT_MIMETYPE);
        return getRaw(context, null, rawIds, mimeTypes, null);
    }

    public Cursor getByEmailOrPhone(Context context, String email, String phone/*, String googleAccountName*/) {
        return context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                PROJECTION,
                Utils.joinString(" AND ",
                        Utils.joinString(" OR ",
                                email != null ? ContactsContract.Data.DATA1 + " = " + DatabaseUtils.sqlEscapeString(email) : null,
                                phone != null ? ContactsContract.Data.DATA1 + " = " + DatabaseUtils.sqlEscapeString(phone) : null),
                        Utils.joinString(" OR ",
                                ContactsContract.Data.MIMETYPE + "=" + DatabaseUtils.sqlEscapeString(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),
                                ContactsContract.Data.MIMETYPE + "=" + DatabaseUtils.sqlEscapeString(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE))),
                null, SQL_GENERAL_ORDER);
    }

    private Cursor getRaw(Context context, List<Long> allowedContactIds, List<Long> allowedRawIds, List<String> allowedMimeTypes, String viaSearch) {
        List<String> args = new ArrayList<>();
        String condViaSearch = (viaSearch == null ? "" : " AND (LOWER(REPLACE(" + ContactsContract.Data.DATA1 + ", ' ', '')) LIKE " +
                DatabaseUtils.sqlEscapeString(viaSearch + "%") + ")");
        String condMimeTypes = Utils.getSQLConditions(ContactsContract.Data.MIMETYPE, allowedMimeTypes, args, false);
        String condIds = Utils.getSQLConditions(ContactsContract.Data.CONTACT_ID, allowedContactIds, null, false);
        String condRawIds = Utils.getSQLConditions(ContactsContract.Data.RAW_CONTACT_ID, allowedRawIds, null, false);

        return context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION,
                "1" + condViaSearch + " AND (" + condMimeTypes + ")" + " AND (" + condRawIds + ") AND (" + condIds + ")",
                args.toArray(new String[args.size()]), FIELD_DISPLAY_NAME + " COLLATE NOCASE, " + SQL_GENERAL_ORDER);
    }

    /**
     * Obtains a list of all contact data found in the Android contacts database according to
     * the given restrictions.
     * <strong>If allowedIds is null, will returns a {@link ContactFilteredCursor}, which will merge duplicate
     * contacts and Peppermint contacts.</strong>
     *
     * @param context the context
     * @param allowedIds the allowed ids filter
     * @param freeTextSearch the via/display name free text filter
     * @param allowedMimeTypes the allowed mime types
     * @return the result cursor
     */
    public Cursor get(final Context context, final List<Long> allowedIds, String freeTextSearch, List<String> allowedMimeTypes, String enforcedViaSearch) {
        List<String> args = new ArrayList<>();

        String condFreeSearch = (freeTextSearch == null ? null : "(LOWER(" + FIELD_DISPLAY_NAME + ") LIKE " + DatabaseUtils.sqlEscapeString(freeTextSearch + "%") + " OR LOWER(" + FIELD_DISPLAY_NAME + ") LIKE " + DatabaseUtils.sqlEscapeString("% " + freeTextSearch + "%") + " OR " + String.format(SQL_VIA_CONDITION, DatabaseUtils.sqlEscapeString(freeTextSearch + "%")) + ")");
        String condViaSearch = (enforcedViaSearch == null ? null : String.format(SQL_VIA_CONDITION, DatabaseUtils.sqlEscapeString(freeTextSearch + "%")));

        String condMimeTypes = Utils.getSQLConditions(ContactsContract.Data.MIMETYPE, allowedMimeTypes, args, false);
        String condIds = Utils.getSQLConditions(ContactsContract.Data._ID, allowedIds, null, false);

        Cursor rootCursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION,
                Utils.joinString(" AND ", condFreeSearch, condViaSearch, "(" + condMimeTypes + ")", "(" + condIds + ")"),
                args.toArray(new String[args.size()]), SQL_PEPPERMINT_ORDER + FIELD_DISPLAY_NAME + " COLLATE NOCASE, " + SQL_EMAIL_FIRST_ORDER + SQL_GENERAL_ORDER);

        if(allowedIds != null) {
            return rootCursor;
        }

        return new ContactFilteredCursor(context, rootCursor);
    }

    // OPERATIONS

    private long insertOrUpdatePhoto(Context context, Uri photoUri, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) {
        int dp200 = Utils.dpToPx(context, 200);
        int dp100 = Utils.dpToPx(context, 100);

        // scale image to 200dp to save memory
        Bitmap realImage = ResourceUtils.getScaledBitmap(context, photoUri, dp200, dp200);

        if(realImage != null) {
            ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

            // cut a square thumbnail
            Bitmap rotatedImage = ThumbnailUtils.extractThumbnail(realImage, dp100, dp100, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            if(FILE_SCHEME.startsWith(photoUri.getScheme())) {
                // rotate image according to the photo params
                rotatedImage = ResourceUtils.getRotatedBitmapFromFileAttributes(rotatedImage, photoUri.toString().substring(6));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            rotatedImage.compress(Bitmap.CompressFormat.PNG, 100, baos);

            ContentProviderOperation.Builder op;
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
                return res != null && res.length > 0 ? ContentUris.parseId(res[res.length-1].uri) : 0;
            }

            return 0;
        }

        return -1;
    }

    private long insertPhone(Context context, String phone, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) throws InvalidPhoneException {
        if(!Utils.isValidPhoneNumber(phone)) {
            throw new InvalidPhoneException();
        }

        boolean alreadyHasPhone = false;

        if(rawId > 0) {
            Cursor checkCursor = getPhone(context, rawId, phone);
            if(checkCursor != null) {
                alreadyHasPhone = checkCursor.getCount() > 0;
                checkCursor.close();
            }
        }

        if(!alreadyHasPhone) {
            ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

            ContentProviderOperation.Builder op;
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
                return res != null && res.length > 0 ? ContentUris.parseId(res[0].uri) : 0;
            }

            return 0;
        }

        return -1;
    }

    public long insertEmail(Context context, String email, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) throws InvalidEmailException {
        if(!Utils.isValidEmail(email)) {
            throw new InvalidEmailException();
        }

        boolean alreadyHasEmail = false;

        if(rawId > 0) {
            Cursor checkCursor = getEmail(context, rawId, email);
            if(checkCursor != null) {
                alreadyHasEmail = checkCursor.getCount() > 0;
                checkCursor.close();
            }
        }

        if(!alreadyHasEmail) {
            ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

            ContentProviderOperation.Builder op;
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
                return res != null && res.length > 0 ? ContentUris.parseId(res[0].uri) : 0;
            }

            return 0;
        }

        return -1;
    }

    private long insertOrUpdateName(Context context, String firstName, String lastName, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) throws InvalidNameException {
        // validate display name
        String fullName = (firstName + " " + lastName).trim();
        if(!Utils.isValidName(fullName)) {
            throw new InvalidNameException();
        }

        ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

        ContentProviderOperation.Builder op;
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
            return res != null && res.length > 0 ? ContentUris.parseId(res[res.length-1].uri) : 0;
        }

        return 0;
    }

    public void deletePeppermint(Context context, long rawId, ArrayList<ContentProviderOperation> operationsList) {
        Cursor checkCursor = getPeppermintByRawId(context, rawId);
        if(checkCursor != null) {
            if(checkCursor.getCount() > 0) {
                ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                                new String[]{String.valueOf(rawId), ContactData.PEPPERMINT_MIMETYPE})
                        .build());

                if (operationsList == null) {
                    // no caller operations list, so execute the operations right away
                    executeOperations(context, ops);
                }
            }
            checkCursor.close();
        }
    }

    public long insertPeppermint(Context context, String email, long rawId, int rawIdBackRef, ArrayList<ContentProviderOperation> operationsList) throws InvalidEmailException {
        boolean alreadyHasPeppermint = false;

        if(rawId > 0) {
            Cursor checkCursor = getPeppermintByRawId(context, rawId);
            if(checkCursor != null) {
                alreadyHasPeppermint = checkCursor.getCount() > 0;
                checkCursor.close();
            }
        }

        if(!alreadyHasPeppermint) {
            ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

            ContentProviderOperation.Builder op;
            if (rawId <= 0) {
                op = ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIdBackRef);
            } else {
                op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
            }

            ops.add(op.withValue(ContactsContract.Data.MIMETYPE, ContactData.PEPPERMINT_MIMETYPE).withValue(ContactsContract.Data.DATA1, email).build());

            if(operationsList == null) {
                // no caller operations list, so execute the operations right away
                ContentProviderResult[] res = executeOperations(context, ops);
                return res != null && res.length > 0 ? ContentUris.parseId(res[0].uri) : 0;
            }

            return 0;
        }

        return -1;
    }

    private long insertRaw(Context context, long contactId, String googleAccountName, ArrayList<ContentProviderOperation> operationsList) {
        ArrayList<ContentProviderOperation> ops = operationsList == null ? new ArrayList<ContentProviderOperation>() : operationsList;

        ContentProviderOperation.Builder insertOp = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, GOOGLE_ACCOUNT_TYPE)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, googleAccountName);

        if(contactId > 0) {
            insertOp.withValue(ContactsContract.RawContacts.CONTACT_ID, contactId);
        }

        ops.add(insertOp.build());

        // check if Peppermint group exists; create it if not
        // we could add to the default group "My Contacts" but it might not exist
        // also, the default operation doesn't necessarily add the contact to a group
        // causing the GROUP_VISIBLE flag to be 0
        Long groupId = null;
        Cursor groupCursor = context.getContentResolver().query(
                ContactsContract.Groups.CONTENT_URI,
                new String[]{ContactsContract.Groups._ID},
                ContactsContract.Groups.GROUP_VISIBLE + "=1 AND " + ContactsContract.Groups.ACCOUNT_NAME + "=" + DatabaseUtils.sqlEscapeString(googleAccountName) +
                        " AND " + ContactsContract.Groups.TITLE + "=" + DatabaseUtils.sqlEscapeString(PEPPERMINT_GROUP_TITLE), null, null);
        try {
            if (groupCursor != null && groupCursor.moveToNext()) {
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
        } finally {
            if (groupCursor != null) {
                groupCursor.close();
            }
        }

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.DATA1, groupId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE).build());

        if(operationsList == null) {
            // no caller operations list, so execute the operations right away
            ContentProviderResult[] res = executeOperations(context, ops);
            return res != null && res.length > 0 ? ContentUris.parseId(res[0].uri) : 0;
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
    public ContactRaw insert(Context context, long contactId, long rawId, String firstName, String lastName, String phone, String email, Uri photoUri, String googleAccountName, boolean hasPeppermint) throws InvalidNameException, InvalidEmailException, InvalidPhoneException {
        firstName = firstName == null ? "" : Utils.capitalizeFully(firstName.trim());
        lastName = lastName == null ? "" : Utils.capitalizeFully(lastName.trim());
        phone = phone == null ? "" : phone.trim();
        email = email == null ? "" : email.trim();

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // create raw contact
        if(rawId <= 0) {
            insertRaw(context, contactId, googleAccountName, ops);
        }

        if(!TextUtils.isEmpty(firstName) || !TextUtils.isEmpty(lastName)) {
            insertOrUpdateName(context, firstName, lastName, rawId, 0, ops);
        }

        if(email.length() > 0) {
            insertEmail(context, email, rawId, 0, ops);
        }

        if(phone.length() > 0) {
            insertPhone(context, phone, rawId, 0, ops);
        }

        if(photoUri != null) {
            insertOrUpdatePhoto(context, photoUri, rawId, 0, ops);
        }

        if(hasPeppermint) {
            insertPeppermint(context, email, rawId, 0, ops);
        }

        executeOperations(context, ops);

        ContactRaw recipient = null;
        Cursor cursor = getByEmailOrPhone(context, email, phone/*, googleAccountName*/);
        while(cursor.moveToNext() && (recipient == null || recipient.getRawId() != rawId)) {
            recipient = getRawContactFromCursor(context, cursor);
        }
        cursor.close();

        return recipient;
    }

    public ContactRaw insertOrUpdate(Context context, long contactId, long rawId, String firstName, String lastName, String phone, String email, Uri photoUri, String googleAccountName, boolean hasPeppermint) throws InvalidPhoneException, InvalidNameException, InvalidEmailException {
        // try to find the rawId by email or phone
        if(rawId <= 0 || contactId <= 0) {
            Cursor cursor = getByEmailOrPhone(context, email, phone/*, googleAccountName*/);
            if(cursor.moveToNext()) {
                contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                rawId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
            }
            cursor.close();
        }

        if(rawId > 0 || contactId > 0) {
            if(Utils.isValidEmail(firstName)) {
                firstName = null;
                lastName = null;
            }
        }

        return insert(context, contactId, rawId, firstName, lastName, phone, email, photoUri, googleAccountName, hasPeppermint);
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

    public static class InvalidNameException extends Exception {
        /* nothing to add here */
    }

    public static class InvalidViaException extends Exception {
        /* nothing to add here */
    }

    public static class InvalidEmailException extends InvalidViaException {
        /* nothing to add here */
    }

    public static class InvalidPhoneException extends InvalidViaException {
        /* nothing to add here */
    }
}
