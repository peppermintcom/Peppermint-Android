package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.data.Recipient;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * ArrayAdapter to show recipients in a ListView.
 */
public class RecipientArrayAdapter extends ArrayAdapter<Recipient> {

    public static RecipientArrayAdapter get(PeppermintApp app, Context context, Long[] allowedIds) {

        Map<Long, Recipient> recipientMap = new HashMap<>();

        Cursor cursor = RecipientAdapterUtils.getRecipientsCursor(context, allowedIds, null, null, null);
        while(cursor.moveToNext()) {
            Recipient recipient = RecipientAdapterUtils.getRecipient(cursor);
            recipientMap.put(recipient.getContactId(), recipient);
        }
        cursor.close();

        return new RecipientArrayAdapter(app, context, recipientMap, allowedIds);
    }

    private PeppermintApp mApp;
    private Map<Long, Recipient> mRecipientMap;
    private Long[] mAllowedIds;

    public RecipientArrayAdapter(PeppermintApp app, Context context, Map<Long, Recipient> recipientMap, Long[] allowedIds) {
        super(context, 0);
        this.mRecipientMap = recipientMap;
        this.mApp = app;
        this.mAllowedIds = allowedIds;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return RecipientAdapterUtils.getView(mApp, getContext(), getItem(position), convertView, parent);
    }

    @Override
    public long getItemId(int position) {
        return mAllowedIds[position];
    }

    @Override
    public Recipient getItem(int position) {
        long id = mAllowedIds[position];
        return mRecipientMap.get(id);
    }

    @Override
    public int getCount() {
        return mAllowedIds.length;
    }
}
