package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.data.PeppermintFilteredCursor;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.base.views.CustomFontTextView;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.utils.DateContainer;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * CursorAdapter to show recipients in a ListView.
 */
public class ContactCursorAdapter extends CursorAdapter {

    private TrackerManager mTrackerManager;
    private Context mContext;
    private DatabaseHelper mDatabaseHelper;
    private Map<Long, Chat> mChats = new HashMap<>();

    public ContactCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.mTrackerManager = TrackerManager.getInstance(context.getApplicationContext());
        this.mDatabaseHelper = DatabaseHelper.getInstance(context);
        this.mContext = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.i_chat_layout, parent, false);
    }

    @Override
    public void bindView(View v, Context context, Cursor cursor) {
        ContactRaw rawContact = cursor instanceof PeppermintFilteredCursor ?
                ((PeppermintFilteredCursor) cursor).getContactRaw() :
                 ContactManager.getRawContactFromCursor(context, cursor);

        AnimatedAvatarView imgPhoto = (AnimatedAvatarView) v.findViewById(R.id.imgPhoto);
        TextView txtName = (TextView) v.findViewById(R.id.txtName);
        TextView txtContact = (TextView) v.findViewById(R.id.txtContact);

        if(rawContact != null && rawContact.getPhotoUri() != null) {
            imgPhoto.setStaticDrawable(Uri.parse(rawContact.getPhotoUri()));
            imgPhoto.setShowStaticAvatar(true);
        } else {
            imgPhoto.setShowStaticAvatar(false);
        }

        if(rawContact != null) {

            txtName.setText(rawContact.getDisplayName());
            if(rawContact.getPeppermint() != null) {
                txtContact.setText(mContext.getString(R.string.app_name));
            } else {
                txtContact.setText(rawContact.getMainDataVia());
            }

            CustomFontTextView txtUnreadMessages = (CustomFontTextView) v.findViewById(R.id.txtUnreadMessages);
            CustomFontTextView txtLastMessageDate = (CustomFontTextView) v.findViewById(R.id.txtLastMessageDate);

            Chat chat = getContactRawChat(rawContact);

            if(chat != null && chat.getLastMessageTimestamp() != null) {
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

            int unreadAmount = 0;
            if(chat != null) {
                unreadAmount = MessageManager.getUnopenedCountByChat(mDatabaseHelper.getReadableDatabase(), chat.getId());
            }

            if(unreadAmount > 0) {
                txtUnreadMessages.setText(String.valueOf(unreadAmount));
                txtUnreadMessages.setVisibility(View.VISIBLE);
            } else {
                txtUnreadMessages.setVisibility(View.GONE);
            }
        }
    }

    public ContactRaw getContactRaw(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return (cursor instanceof PeppermintFilteredCursor ? ((PeppermintFilteredCursor) cursor).getContactRaw() : ContactManager.getRawContactFromCursor(mContext, cursor));
    }

    protected Chat getContactRawChat(ContactRaw rawContact) {
        long id = rawContact.getMainDataId();
        Chat chat;
        if(!mChats.containsKey(id)) {
            chat = rawContact.getPeppermint() != null ?
                    ChatManager.getMainChatByDroidContactId(mDatabaseHelper.getReadableDatabase(), rawContact.getContactId()) :
                    ChatManager.getChatByRecipients(mDatabaseHelper.getReadableDatabase(), new Recipient(rawContact, null));
            if(chat != null) {
                mChats.put(id, chat);
            }
            return chat;
        }

        return mChats.get(id);
    }

    @Override
    public void notifyDataSetChanged() {
        mChats.clear();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        mChats.clear();
        super.notifyDataSetInvalidated();
    }

    @Override
    public void changeCursor(Cursor cursor) {
        if(cursor != getCursor()) {
            mChats.clear();
        }
        super.changeCursor(cursor);
    }
}
