package com.peppermint.app.data;

import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nuno Luz on 22-02-2016.
 */
public class PeppermintFilteredCursor extends FilteredCursor {

    private Set<String> mViaSet = new HashSet<>();
    private Map<Long, Contact> mPeppermintContacts;

    public PeppermintFilteredCursor(Cursor cursor, Map<Long, Contact> peppermintContacts) {
        super(cursor);
        this.mPeppermintContacts = peppermintContacts;
        setFilter(new Filter() {
            @Override
            public boolean isValid(Cursor cursor) {
                // removes duplicate contacts
                long rawId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
                String via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)).trim().toLowerCase() +
                        cursor.getString(cursor.getColumnIndex(RecipientManager.FIELD_DISPLAY_NAME)).replaceAll("\\s+", "").toLowerCase();

                if (!mViaSet.contains(via)) {
                    mViaSet.add(via);

                    String peppermintVia = mPeppermintContacts.containsKey(rawId) ? rawId + "_Peppermint" : null;
                    if(peppermintVia != null) {
                        if (!mViaSet.contains(peppermintVia)) {
                            mViaSet.add(peppermintVia);
                        } else {
                            return false;
                        }
                    }

                    return true;
                }

                return false;
            }
        });
    }

    public Contact getPeppermintContact(long rawId) {
        return mPeppermintContacts.get(rawId);
    }
}
