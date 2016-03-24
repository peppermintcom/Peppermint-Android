package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.ChatRecipient;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.views.simple.CustomFontTextView;
import com.peppermint.app.utils.DateContainer;

import java.text.ParseException;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * ArrayAdapter to show chats in a ListView.
 */
public class ChatCursorAdapter extends CursorAdapter {

    private Context mContext;
    private DatabaseHelper mDatabaseHelper;
    private TrackerManager mTrackerManager;
    private Set<Long> mPeppermintSet;

    public ChatCursorAdapter(Context context, Cursor cursor, Set<Long> peppermintSet, TrackerManager mTrackerManager) {
        super(context, cursor, 0);
        this.mContext = context;
        this.mPeppermintSet = peppermintSet;
        this.mTrackerManager = mTrackerManager;
        this.mDatabaseHelper = DatabaseHelper.getInstance(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(mContext).inflate(R.layout.i_chat_layout, parent, false);
    }

    @Override
    public synchronized void bindView(View view, Context context, Cursor cursor) {
        Chat chat = getChat(cursor);
        ChatRecipient recipient = chat.getRecipientList().get(0);

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
            if(mPeppermintSet != null && mPeppermintSet.contains(recipient.getRawContactId())) {
                txtContact.setText(R.string.app_name);
            } else {
                txtContact.setText(recipient.getVia());
            }
        }

        CustomFontTextView txtUnreadMessages = (CustomFontTextView) view.findViewById(R.id.txtUnreadMessages);
        CustomFontTextView txtLastMessageDate = (CustomFontTextView) view.findViewById(R.id.txtLastMessageDate);

        if(chat.getLastMessageTimestamp() != null) {
            try {
                DateContainer lastMessageDate = new DateContainer(DateContainer.TYPE_DATE, chat.getLastMessageTimestamp());
                txtLastMessageDate.setText(DateContainer.getRelativeLabelToToday(mContext, lastMessageDate, TimeZone.getDefault()));
            } catch (ParseException e) {
                mTrackerManager.logException(e);
                txtLastMessageDate.setText(chat.getLastMessageTimestamp());
            }
        } else {
            txtLastMessageDate.setText("");
        }

        int unreadAmount = MessageManager.getUnopenedCountByChat(mDatabaseHelper.getReadableDatabase(), chat.getId());
        if(unreadAmount > 0) {
            txtUnreadMessages.setText(String.valueOf(unreadAmount));
            txtUnreadMessages.setVisibility(View.VISIBLE);
        } else {
            txtUnreadMessages.setVisibility(View.GONE);
        }
    }

    public Chat getChat(Cursor cursor) {
        // get recipient data as well
        return ChatManager.getChatFromCursor(mDatabaseHelper.getReadableDatabase(), cursor);
    }

    public Chat getChat(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getChat(cursor);
    }

    public Set<Long> getPeppermintSet() {
        return mPeppermintSet;
    }

    public synchronized void setPeppermintSet(Set<Long> mPeppermintSet) {
        this.mPeppermintSet = mPeppermintSet;
    }
}
