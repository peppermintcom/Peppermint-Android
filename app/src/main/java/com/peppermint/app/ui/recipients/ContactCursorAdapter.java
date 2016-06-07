package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.peppermint.app.dal.contact.ContactFilteredCursor;
import com.peppermint.app.dal.contact.ContactManager;
import com.peppermint.app.dal.contact.ContactRaw;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * CursorAdapter to show recipients in a ListView.
 */
public class ContactCursorAdapter extends CursorAdapter {

    private Context mContext;

    public ContactCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.mContext = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new ContactView(context);
    }

    @Override
    public void bindView(View v, Context context, Cursor cursor) {
        final ContactRaw rawContact = cursor instanceof ContactFilteredCursor ?
                ((ContactFilteredCursor) cursor).getContactRaw() :
                 ContactManager.getInstance().getRawContactFromCursor(context, cursor);
        ((ContactView) v).setContactRaw(rawContact);
    }

    public ContactRaw getContactRaw(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return (cursor instanceof ContactFilteredCursor ? ((ContactFilteredCursor) cursor).getContactRaw() : ContactManager.getInstance().getRawContactFromCursor(mContext, cursor));
    }
}
