package com.peppermint.app.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.utils.FilteredCursor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by NunoLuz on 27/08/2015.
 *
 * CursorAdapter to show recipients in a ListView
 * It contains utility static methods to get the proper FilteredCursor.
 */
public class RecipientAdapter extends CursorAdapter {

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

    public static RecipientAdapter get(Context context, Long[] allowedIds, String freeTextSearch, Boolean areStarred, String[] allowedMimeTypes) {
        return new RecipientAdapter(context, getContactsCursor(context, allowedIds, freeTextSearch, areStarred, allowedMimeTypes));
    }

    public RecipientAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    public static FilteredCursor getContactsCursor(Context context, Long[] allowedIds, String freeTextSearch, Boolean areStarred, String[] allowedMimeTypes) {
        List<String> args = new ArrayList<>();
        String condStarred = (areStarred == null ? "" : " AND " + ContactsContract.Contacts.STARRED + "=" + (areStarred ? "1" : "0"));
        String condFreeSearch = (freeTextSearch == null ? "" : " AND (LOWER(" + DISPLAY_NAME + ") LIKE " + DatabaseUtils.sqlEscapeString(/*"%" + */freeTextSearch + "%") + " OR LOWER(" + ContactsContract.Data.DATA1 + ") LIKE " + DatabaseUtils.sqlEscapeString(/*"%" + */freeTextSearch + "%") + ")");
        String condMimeTypes = getConditions(ContactsContract.Data.MIMETYPE, allowedMimeTypes, args, false);
        String condIds = getConditions(ContactsContract.Data._ID, allowedIds, null, false);

        return new FilteredCursor(context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION,
                "1" + condStarred + condFreeSearch + " AND (" + condMimeTypes + ")" + " AND (" + condIds + ")",
                args.toArray(new String[args.size()]), DISPLAY_NAME + " COLLATE NOCASE"), new FilteredCursor.Filter() {
            private Set<String> mViaSet = new HashSet<>();
            @Override
            public boolean isValid(Cursor cursor) {
                String via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)).trim().toLowerCase();
                if(!mViaSet.contains(via)) {
                    mViaSet.add(via);
                    return true;
                }
                return false;
            }
        });
    }

    protected static <T> String getConditions(String field, T[] allowed, List<String> args, boolean isAnd) {
        if(allowed == null) {
            return "1";
        }

        StringBuilder b = new StringBuilder();
        for(int i=0; i<allowed.length; i++) {
            if(i != 0) {
                b.append(isAnd ? " AND " : " OR ");
            }

            if(args != null) {
                args.add(allowed[i].toString());
                b.append(field);
                b.append("=?");
            } else {
                b.append(field);
                b.append("=");
                b.append(allowed[i].toString());
            }
        }
        return b.toString();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.recipient_item_layout, parent, false);
    }

    @Override
    public void bindView(View v, Context context, Cursor cursor) {
        String via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1));
        long rawId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
        boolean starred = cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.STARRED)) != 0;
        String mime = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));

        String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_URI));
        String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
        String accountType = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));

        ImageView imgPhoto = (ImageView) v.findViewById(R.id.photo);
        TextView txtName = (TextView) v.findViewById(R.id.name);
        TextView txtVia = (TextView) v.findViewById(R.id.via);

        if(photoUri != null) {
            imgPhoto.setImageURI(Uri.parse(photoUri));
        } else {
            imgPhoto.setImageResource(R.drawable.ic_account_circle_grey600_48dp);
        }

        txtName.setText(name);
        txtVia.setText(via);
    }

    public Recipient getRecipient(int position) {
        Cursor cursor = (Cursor) getItem(position);

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
}
