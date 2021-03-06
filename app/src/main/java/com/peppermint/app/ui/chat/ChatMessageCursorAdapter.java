package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.peppermint.app.PlayerServiceManager;
import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.MessageManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * ArrayAdapter to show chat messages in a ListView.<br />
 */
public class ChatMessageCursorAdapter extends CursorAdapter {

    private Set<MessageView> mViewSet = new HashSet<>();

    private final Context mContext;
    private MessagesServiceManager mMessagesServiceManager;
    private PlayerServiceManager mPlayerServiceManager;
    private MessageView.ExclamationClickListener mExclamationClickListener;

    public ChatMessageCursorAdapter(final Context mContext, MessagesServiceManager mMessagesServiceManager, PlayerServiceManager mPlayerServiceManager, Cursor cursor) {
        super(mContext, cursor, 0);
        this.mContext = mContext;
        this.mMessagesServiceManager = mMessagesServiceManager;
        this.mPlayerServiceManager = mPlayerServiceManager;
    }

    public void destroy() {
        for(MessageView messageView : mViewSet) {
            // reset the MessageView; stops listening for player/message events
            // when the message is set to null
            messageView.setMessage(null, null);
            messageView.setSharedMessageServiceManager(null);
            messageView.setSharedPlayerServiceManager(null);
        }
        mViewSet.clear();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final MessageView messageView = new MessageView(context);
        mViewSet.add(messageView);
        return messageView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Message message = getMessage(cursor);
        Message prevMessage = null;

        if(cursor.moveToPrevious()) {
            prevMessage = getMessage(cursor);
            cursor.moveToNext();
        }

        final MessageView messageView = (MessageView) view;
        messageView.setSharedPlayerServiceManager(mPlayerServiceManager);
        messageView.setSharedMessageServiceManager(mMessagesServiceManager);
        messageView.setOnExclamationClickListener(mExclamationClickListener);
        messageView.setMessage(message, prevMessage);
    }

    public Message getMessage(Cursor cursor) {
        return MessageManager.getFromCursor(DatabaseHelper.getInstance(mContext).getReadableDatabase(), cursor);
    }

    public Message getMessage(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getMessage(cursor);
    }

    public void setExclamationClickListener(MessageView.ExclamationClickListener mExclamationClickListener) {
        this.mExclamationClickListener = mExclamationClickListener;
    }
}
