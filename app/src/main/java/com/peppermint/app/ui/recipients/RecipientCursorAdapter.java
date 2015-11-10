package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.Recipient;

import java.util.List;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * CursorAdapter to show recipients in a ListView.
 */
public class RecipientCursorAdapter extends CursorAdapter {

    public static RecipientCursorAdapter get(PeppermintApp app, Context context, List<Long> allowedIds, String freeTextSearch, Boolean areStarred, List<String> allowedMimeTypes) {
        return new RecipientCursorAdapter(app, context, RecipientAdapterUtils.getRecipientsCursor(context, allowedIds, freeTextSearch, areStarred, allowedMimeTypes));
    }

    private PeppermintApp mApp;

    public RecipientCursorAdapter(PeppermintApp app, Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.mApp = app;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.i_recipient_layout, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        RecipientAdapterUtils.getView(mApp, context, RecipientAdapterUtils.getRecipient(cursor), view, null);
    }

    public Recipient getRecipient(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return RecipientAdapterUtils.getRecipient(cursor);
    }
}
