package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.views.simple.CustomFontTextView;
import com.peppermint.app.utils.DateContainer;

import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.util.Set;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * ArrayAdapter to show chats in a ListView.
 */
public class ChatCursorAdapter extends CursorAdapter {

    private Context mContext;
    private SQLiteDatabase mDb;
    private TrackerManager mTrackerManager;
    private Set<Long> mPeppermintSet;

    public ChatCursorAdapter(Context context, Cursor cursor, Set<Long> peppermintSet, SQLiteDatabase db, TrackerManager mTrackerManager) {
        super(context, cursor, 0);
        this.mContext = context;
        this.mPeppermintSet = peppermintSet;
        this.mDb = db;
        this.mTrackerManager = mTrackerManager;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(mContext).inflate(R.layout.i_chat_layout, parent, false);
    }

    @Override
    public synchronized void bindView(View view, Context context, Cursor cursor) {
        Chat chat = getChat(cursor);
        Recipient recipient = chat.getMainRecipientParameter();

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
            txtName.setText(recipient.getDisplayName());
            if(mPeppermintSet != null && mPeppermintSet.contains(recipient.getRawId())) {
                txtContact.setText(R.string.app_name);
            } else {
                txtContact.setText(recipient.getEmail() != null ? recipient.getEmail().getVia() : recipient.getPhone().getVia());
            }
        }

        CustomFontTextView txtUnreadMessages = (CustomFontTextView) view.findViewById(R.id.txtUnreadMessages);
        CustomFontTextView txtLastMessageDate = (CustomFontTextView) view.findViewById(R.id.txtLastMessageDate);

        if(chat.getLastMessageTimestamp() != null) {
            try {
                DateContainer lastMessageDate = new DateContainer(DateContainer.TYPE_DATE, chat.getLastMessageTimestamp());
                txtLastMessageDate.setText(DateContainer.getRelativeLabelToToday(mContext, lastMessageDate, DateTimeZone.getDefault()));
            } catch (ParseException e) {
                mTrackerManager.logException(e);
                txtLastMessageDate.setText(chat.getLastMessageTimestamp());
            }
        } else {
            txtLastMessageDate.setText("");
        }

        int unreadAmount = MessageManager.getUnopenedCountByChat(mDb, chat.getId());
        if(unreadAmount > 0) {
            txtUnreadMessages.setText(String.valueOf(unreadAmount));
            txtUnreadMessages.setVisibility(View.VISIBLE);
        } else {
            txtUnreadMessages.setVisibility(View.GONE);
        }
    }

    public Chat getChat(Cursor cursor) {
        // get recipient data as well
        return ChatManager.getFromCursor(mContext, cursor);
    }

    public Chat getChat(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getChat(cursor);
    }

    public SQLiteDatabase getDatabase() {
        return mDb;
    }

    public synchronized void setDatabase(SQLiteDatabase mDb) {
        this.mDb = mDb;
    }

    public Set<Long> getPeppermintSet() {
        return mPeppermintSet;
    }

    public synchronized void setPeppermintSet(Set<Long> mPeppermintSet) {
        this.mPeppermintSet = mPeppermintSet;
    }
}
