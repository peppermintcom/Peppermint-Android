package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.peppermint.app.dal.DatabaseHelper;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.chat.ChatManager;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * ArrayAdapter to show chats in a ListView.
 */
public class ChatCursorAdapter extends CursorAdapter {

    private Context mContext;
    private DatabaseHelper mDatabaseHelper;

    public ChatCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.mContext = context;
        this.mDatabaseHelper = DatabaseHelper.getInstance(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new ChatView(context);
    }

    @Override
    public synchronized void bindView(View view, Context context, Cursor cursor) {
        ((ChatView) view).setChat(getChat(cursor));
    }

    public Chat getChat(Cursor cursor) {
        // get recipient data as well
        return ChatManager.getInstance(mContext).getFromCursor(mDatabaseHelper.getReadableDatabase(), cursor);
    }

    public Chat getChat(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getChat(cursor);
    }
}
