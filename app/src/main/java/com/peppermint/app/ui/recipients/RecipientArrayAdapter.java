package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.data.Recipient;

import java.util.List;
import java.util.Map;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * ArrayAdapter to show recipients in a ListView.<br />
 * Uses the {@link RecipientAdapterUtils#getView(PeppermintApp, Context, Recipient, View, ViewGroup)}
 * to fill the view of each item.
 */
public class RecipientArrayAdapter extends ArrayAdapter<Recipient> {

    private PeppermintApp mApp;
    private Map<Long, Recipient> mRecipientMap;
    private List<Long> mAllowedIds;

    public RecipientArrayAdapter(PeppermintApp app, Context context, Map<Long, Recipient> recipientMap, List<Long> allowedIds) {
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
        return mAllowedIds.get(position);
    }

    @Override
    public Recipient getItem(int position) {
        long id = mAllowedIds.get(position);
        return mRecipientMap.get(id);
    }

    @Override
    public int getCount() {
        return mAllowedIds.size();
    }
}
