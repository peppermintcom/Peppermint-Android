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
import com.peppermint.app.data.ChatRecipient;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.data.PeppermintFilteredCursor;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.views.simple.CustomFontTextView;
import com.peppermint.app.utils.DateContainer;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        this.mContext = context;
        this.mDatabaseHelper = DatabaseHelper.getInstance(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.i_chat_layout, parent, false);
    }

    @Override
    public void bindView(View v, Context context, Cursor cursor) {
        ContactRaw rawContact = ContactManager.getRawContactFromCursor(cursor);

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
            if(isPeppermintContact(cursor, rawContact.getRawId())) {
                txtContact.setText(R.string.app_name);
            } else {
                txtContact.setText(rawContact.getEmail() != null ? rawContact.getEmail().getVia() : rawContact.getPhone().getVia());
            }

            Chat chat = getRecipientChat(cursor, rawContact);

            CustomFontTextView txtUnreadMessages = (CustomFontTextView) v.findViewById(R.id.txtUnreadMessages);
            CustomFontTextView txtLastMessageDate = (CustomFontTextView) v.findViewById(R.id.txtLastMessageDate);

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

    public ContactRaw getRecipient(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return ContactManager.getRawContactFromCursor(cursor);
    }

    protected Chat getRecipientChat(Cursor cursor, ContactRaw rawContact) {
        Set<Long> contactIdSet = getMergedContactIds(cursor, rawContact);
        for(Long id : contactIdSet) {
            Chat chat = null;
            if(!mChats.containsKey(id)) {
                ChatRecipient recipient = new ChatRecipient();
                recipient.setContactId(id);
                chat = ChatManager.getChatByRecipients(mDatabaseHelper.getReadableDatabase(), recipient);
                mChats.put(id, chat);
            } else {
                chat = mChats.get(id);
            }

            if(chat != null) {
                return chat;
            }
        }
        return null;
    }

    protected Set<Long> getMergedContactIds(Cursor cursor, ContactRaw recipient) {
        Set<Long> set = new HashSet<>();

        if(isPeppermintContact(cursor, recipient.getRawId())) {
            Set<Long> tmpSet = ((PeppermintFilteredCursor) cursor).getPeppermintMergedContactIds(recipient.getRawId());
            if(tmpSet != null) {
                set.addAll(tmpSet);
            }
        } else {
            set.add(recipient.getEmailOrPhoneContactId());
        }

        return set;
    }

    public boolean isPeppermintContact(Cursor cursor, long rawId) {
        return cursor instanceof PeppermintFilteredCursor && ((PeppermintFilteredCursor) cursor).getPeppermintContact(rawId) != null;
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
