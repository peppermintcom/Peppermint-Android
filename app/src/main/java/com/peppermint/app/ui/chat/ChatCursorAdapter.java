package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.ui.views.simple.CustomFontTextView;
import com.peppermint.app.utils.DateContainer;

import java.text.ParseException;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * ArrayAdapter to show recipients in a ListView.<br />
 * Uses the {@link RecipientAdapterUtils#getView(PeppermintApp, Context, Recipient, View, ViewGroup)}
 * to fill the view of each item.
 */
public class ChatCursorAdapter extends CursorAdapter {

    private Context mContext;
    private SQLiteDatabase mDb;
    private TrackerManager mTrackerManager;

    public ChatCursorAdapter(Context context, Cursor cursor, SQLiteDatabase db, TrackerManager mTrackerManager) {
        super(context, cursor, 0);
        this.mContext = context;
        this.mDb = db;
        this.mTrackerManager = mTrackerManager;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(mContext).inflate(R.layout.i_chat_layout, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Chat chat = getChat(cursor);
        Recipient recipient = chat.getMainRecipient();

        AnimatedAvatarView imgPhoto = (AnimatedAvatarView) view.findViewById(R.id.imgPhoto);
        CustomFontTextView txtName = (CustomFontTextView) view.findViewById(R.id.txtName);
        CustomFontTextView txtContact = (CustomFontTextView) view.findViewById(R.id.txtContact);

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

        CustomFontTextView txtUnreadMessages = (CustomFontTextView) view.findViewById(R.id.txtUnreadMessages);
        CustomFontTextView txtLastMessageDate = (CustomFontTextView) view.findViewById(R.id.txtLastMessageDate);

        if(chat.getLastMessageTimestamp() != null) {
            try {
                DateContainer lastMessageDate = new DateContainer(DateContainer.TYPE_DATE, chat.getLastMessageTimestamp().substring(0, 10));
                txtLastMessageDate.setText(DateContainer.getRelativeLabelToToday(mContext, lastMessageDate));
            } catch (ParseException e) {
                mTrackerManager.logException(e);
                txtLastMessageDate.setText(chat.getLastMessageTimestamp());
            }
        }

        int unreadAmount = Message.getUnopenedCountByChat(mDb, chat.getId());
        if(unreadAmount > 0) {
            txtUnreadMessages.setText(String.valueOf(unreadAmount));
            txtUnreadMessages.setVisibility(View.VISIBLE);
        } else {
            txtUnreadMessages.setVisibility(View.GONE);
        }
    }

    public Chat getChat(Cursor cursor) {
        return Chat.getFromCursor(mDb, cursor);
    }

    public Chat getChat(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getChat(cursor);
    }

    public SQLiteDatabase getDatabase() {
        return mDb;
    }

    public void setDatabase(SQLiteDatabase mDb) {
        this.mDb = mDb;
    }
}
