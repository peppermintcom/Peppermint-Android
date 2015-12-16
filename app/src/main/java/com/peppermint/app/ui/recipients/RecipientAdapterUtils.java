package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.utils.FilteredCursor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Nuno Luz on 18-09-2015.
 *
 * Utility static methods for handling {@link Recipient}s.
 */
public class RecipientAdapterUtils {

    private static final String DISPLAY_NAME = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
            ContactsContract.Contacts.DISPLAY_NAME;

    private static final String[] PROJECTION = {
            ContactsContract.Data._ID,
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.Data.STARRED,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
            DISPLAY_NAME,
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE
    };

    private static final String[] PROJECTION_DUP = {
            ContactsContract.RawContacts._ID,
            ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY
    };

    /**
     * Loads the recipient data from the cursor's current positions and places it in a
     * new {@link Recipient} object.
     *
     * @param cursor the cursor
     * @return the new recipient object
     */
    public static Recipient getRecipient(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID));
        long rawId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
        boolean starred = cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.STARRED)) != 0;
        String mime = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
        String via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1));
        String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_URI));
        String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
        String accountType = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));

        return new Recipient(id, rawId, starred, mime, name, accountType, photoUri, via);
    }

    public static Cursor getRecipientsCursor(Context context, List<Long> allowedRawIds, List<String> allowedMimeTypes, String viaSearch) {
        List<String> args = new ArrayList<>();
        String condViaSearch = (viaSearch == null ? "" : " AND (LOWER(REPLACE(" + ContactsContract.Data.DATA1 + ", ' ', '')) LIKE " + DatabaseUtils.sqlEscapeString(/*"%" + */viaSearch + "%") + ")");
        String condMimeTypes = getConditions(ContactsContract.Data.MIMETYPE, allowedMimeTypes, args, false);
        String condIds = getConditions(ContactsContract.Data.RAW_CONTACT_ID, allowedRawIds, null, false);

        return context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION,
                "1" + condViaSearch + " AND (" + condMimeTypes + ")" + " AND (" + condIds + ")" + " AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + ">0",
                args.toArray(new String[args.size()]), DISPLAY_NAME + " COLLATE NOCASE");
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
    public static Cursor getRecipientsCursor(final Context context, List<Long> allowedIds, String freeTextSearch, Boolean areStarred, List<String> allowedMimeTypes, String enforcedViaSearch) {
        List<String> args = new ArrayList<>();
        String condStarred = (areStarred == null ? "" : " AND " + ContactsContract.Contacts.STARRED + "=" + (areStarred ? "1" : "0"));
        String condFreeSearch = (freeTextSearch == null ? "" : " AND (LOWER(" + DISPLAY_NAME + ") LIKE " + DatabaseUtils.sqlEscapeString(freeTextSearch + "%") + " OR LOWER(" + DISPLAY_NAME + ") LIKE " + DatabaseUtils.sqlEscapeString("% " + freeTextSearch + "%") + " OR LOWER(REPLACE(" + ContactsContract.Data.DATA1 + ", ' ', '')) LIKE " + DatabaseUtils.sqlEscapeString(/*"%" + */freeTextSearch + "%") + ")");
        String condViaSearch = (enforcedViaSearch == null ? "" : " AND (LOWER(REPLACE(" + ContactsContract.Data.DATA1 + ", ' ', '')) LIKE " + DatabaseUtils.sqlEscapeString(/*"%" + */enforcedViaSearch + "%") + ")");
        String condMimeTypes = getConditions(ContactsContract.Data.MIMETYPE, allowedMimeTypes, args, false);
        String condIds = getConditions(ContactsContract.Data._ID, allowedIds, null, false);

        /*Cursor tmp = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, null,
                "1=1", null, null);
        while(tmp != null && tmp.moveToNext()) {
            for(int i=0; i<tmp.getColumnCount(); i++) {
                Log.d(RecipientAdapterUtils.class.getSimpleName(), "       %%|| " + tmp.getColumnName(i) + " = " + tmp.getString(i));
            }
        }
        tmp.close();*/

        Cursor rootCursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null,
                "1" + condStarred + condFreeSearch + condViaSearch + " AND (" + condMimeTypes + ")" + " AND (" + condIds + ")" + " AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + ">0",
                args.toArray(new String[args.size()]), DISPLAY_NAME + " COLLATE NOCASE");

        if(allowedIds != null) {
            return rootCursor;
        }

        return new FilteredCursor(rootCursor, new FilteredCursor.Filter() {
            private Set<String> mViaSet = new HashSet<>();

            @Override
            public boolean isValid(Cursor cursor) {
                // removes duplicate contacts
                String via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)).trim().toLowerCase() + cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)).replaceAll("\\s+", "").toLowerCase();

                if (!mViaSet.contains(via)) {
                    mViaSet.add(via);
                    return true;
                }

                /*// check again but with raw contact display name
                long rawId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
                Cursor dupCursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, PROJECTION_DUP,
                        ContactsContract.RawContacts._ID + "=" + rawId, null, null);
                if(dupCursor != null && dupCursor.moveToNext()) {
                    via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)).trim().toLowerCase() + dupCursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY)).replaceAll("\\s+", "").toLowerCase();
                }
                dupCursor.close();

                if (!mViaSet.contains(via)) {
                    mViaSet.add(via);
                    return true;
                }*/

                return false;
            }
        });
    }

    /**
     * Builds a string with a set of conditions in SQL syntax (e.g. the where part).
     * @param field the restricted field
     * @param allowed the set of allowed values
     * @param args the set of allowed values in string format
     * @param isAnd the separator between conditions (either AND or OR)
     * @param <T> the allowed values type
     * @return the conditions string
     */
    protected static <T> String getConditions(String field, List<T> allowed, List<String> args, boolean isAnd) {
        if(allowed == null) {
            return "1";
        }

        StringBuilder b = new StringBuilder();
        for(int i=0; i<allowed.size(); i++) {
            if(i != 0) {
                b.append(isAnd ? " AND " : " OR ");
            }

            if(args != null) {
                args.add(allowed.get(i).toString());
                b.append(field);
                b.append("=?");
            } else {
                b.append(field);
                b.append("=");
                b.append(allowed.get(i).toString());
            }
        }
        return b.toString();
    }

    /**
     * Builds a view with Recipient data for Adapters.
     * @param app the PeppermintApp instance
     * @param context the context
     * @param recipient the recipient data
     * @param convertView the re-usable view (if it exists)
     * @param parent the parent view
     * @return the built recipient view
     */
    public static View getView(PeppermintApp app, Context context, Recipient recipient, View convertView, ViewGroup parent) {
        View v = convertView;

        if(v == null) {
            v = LayoutInflater.from(context).inflate(R.layout.i_recipient_layout, parent, false);
        }

        AnimatedAvatarView imgPhoto = (AnimatedAvatarView) v.findViewById(R.id.imgPhoto);
        TextView txtName = (TextView) v.findViewById(R.id.txtName);
        TextView txtVia = (TextView) v.findViewById(R.id.txtVia);
        TextView txtContact = (TextView) v.findViewById(R.id.txtContact);

        txtVia.setTypeface(app.getFontRegular());
        txtName.setTypeface(app.getFontSemibold());
        txtContact.setTypeface(app.getFontSemibold());

        if(recipient != null && recipient.getPhotoUri() != null) {
            imgPhoto.setStaticDrawable(Uri.parse(recipient.getPhotoUri()));
            imgPhoto.setShowStaticAvatar(true);
        } else {
            imgPhoto.setShowStaticAvatar(false);
        }

        if(recipient != null) {
            txtName.setText(recipient.getName());
            txtContact.setText(recipient.getVia());
        }

        return v;
    }
}
