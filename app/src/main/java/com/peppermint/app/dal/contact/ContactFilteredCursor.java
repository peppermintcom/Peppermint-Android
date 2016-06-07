package com.peppermint.app.dal.contact;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.peppermint.app.dal.FilteredCursor;
import com.peppermint.app.dal.contact.ContactManager;
import com.peppermint.app.dal.contact.ContactData;
import com.peppermint.app.dal.contact.ContactRaw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nuno Luz on 22-02-2016.
 *
 * Filters {@link ContactRaw} cursors to merge Peppermint contacts and avoid duplicate contacts.
 */
public class ContactFilteredCursor extends FilteredCursor {

    private Set<String> mViaSet = new HashSet<>();
    private Map<Long, ContactData> mPeppermintContacts;
    private Map<Long, List<ContactData>> mFilteredContacts = new HashMap<>();

    private void addFilteredContact(Cursor cursor) {
        ContactData contact = ContactManager.getInstance().getContactFromCursor(cursor);
        addFilteredContact(contact);
    }

    private void addFilteredContact(ContactData contact) {
        List<ContactData> list = mFilteredContacts.get(contact.getRawId());
        if(list == null) {
            list = new ArrayList<>();
            mFilteredContacts.put(contact.getRawId(), list);
        }
        list.add(contact);
    }

    public ContactFilteredCursor(final Context mContext, Cursor cursor) {
        super(cursor);
        this.mPeppermintContacts = ContactManager.getInstance().getPeppermintContacts(mContext);

        setFilter(new Filter() {
            @Override
            public boolean isValid(Cursor cursor) {
                // this algorithm assumes that contacts are ordered by mimetype with Peppermint first,
                // followed by Email mimetype.

                long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));

                // check if there's already a peppermint contact there
                if(mPeppermintContacts.containsKey(contactId)) {
                    final String peppermintVia = contactId + "_Peppermint";
                    if (mViaSet.contains(peppermintVia)) {
                        addFilteredContact(cursor);
                        return false;
                    }
                    ContactData peppermintContact = mPeppermintContacts.get(contactId);
                    addFilteredContact(peppermintContact);
                    mViaSet.add(peppermintVia);
                }

                final String emailVia = contactId + "_" + ContactData.EMAIL_MIMETYPE;
                if (mViaSet.contains(emailVia)) {
                    addFilteredContact(cursor);
                    return false;
                }
                mViaSet.add(emailVia);

                // check for duplicates
                String via = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)).trim().toLowerCase() +
                        cursor.getString(cursor.getColumnIndex(ContactManager.FIELD_DISPLAY_NAME)).replaceAll("\\s+", "").toLowerCase();

                if (!mViaSet.contains(via)) {
                    mViaSet.add(via);
                    return true;
                }

                addFilteredContact(cursor);
                return false;
            }
        });
    }

    public ContactRaw getContactRaw() {
        ContactRaw contactRaw = ContactManager.getInstance().getRawContactFromCursor(null, mCursor);
        if(mFilteredContacts != null && mFilteredContacts.containsKey(contactRaw.getRawId())) {
            List<ContactData> contactList = mFilteredContacts.get(contactRaw.getRawId());
            if(contactList != null) {
                for(ContactData contact : contactList) {
                    contactRaw.setContactData(contact);
                }
            }
        }
        return contactRaw;
    }

    @Override
    public void close() {
        super.close();

        if(mViaSet != null) {
            mViaSet.clear();
            mViaSet = null;
        }

        if(mFilteredContacts != null) {
            mFilteredContacts.clear();
            mFilteredContacts = null;
        }

        if(mPeppermintContacts != null) {
            mPeppermintContacts.clear();
            mPeppermintContacts = null;
        }
    }
}
