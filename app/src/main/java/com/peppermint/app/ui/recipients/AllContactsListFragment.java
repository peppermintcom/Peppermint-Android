package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.ContactData;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.FilteredCursor;
import com.peppermint.app.data.GlobalManager;
import com.peppermint.app.data.PeppermintFilteredCursor;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.chat.ChatActivity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AllContactsListFragment extends ContactListFragment {

    private static final String TAG = AllContactsListFragment.class.getSimpleName();

    private static final String SCREEN_ID = "AllContacts";

    private static final String SAVED_DIALOG_STATE_KEY = TAG + "_AddEmailDialogState";

    private static final List<String> MIMETYPES = new ArrayList<>();
    static {
        MIMETYPES.add(ContactData.EMAIL_MIMETYPE);
        MIMETYPES.add(ContactData.PHONE_MIMETYPE);
    }

    private class ContactsContentObserver extends ContentObserver {
        public ContactsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            refresh();
        }
    }
    private ContactsContentObserver mContactsObserver;

    // dialogs
    private AddEmailDialog mAddContactDialog;

    // the recipient list
    private ContactCursorAdapter mAdapter;
    private PeppermintFilteredCursor mCursor;

    @Override
    protected Object onAsyncRefresh(Context context, String searchName, String searchVia) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            // get normal full, email or phone contact list
            FilteredCursor cursor = (FilteredCursor) ContactManager.get(context, null, searchName, MIMETYPES, searchVia);
            if (cursor.getOriginalCursor().getCount() <= 0 && searchName != null && searchVia != null) {
                cursor.close();
                cursor = (FilteredCursor) ContactManager.get(context, null, null, MIMETYPES, searchVia);
            }
            cursor.filter();
            return cursor;
        }

        return null;
    }

    @Override
    protected void onAsyncRefreshCancelled(Context context, Object data) {
        Cursor cursor = (Cursor) data;
        if(cursor != null) {
            cursor.close();
        }
    }

    @Override
    protected void onAsyncRefreshFinished(Context context, Object data) {
        mCursor = (PeppermintFilteredCursor) data;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            setCursor();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // dialog for adding missing email
        mAddContactDialog = new AddEmailDialog(mActivity);
        mAddContactDialog.setPositiveButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ContactRaw contactRaw = mAddContactDialog.getContact();
                final String emailAddress = mAddContactDialog.getEmail();
                try {
                    ContactManager.insertEmail(mActivity, emailAddress, contactRaw.getRawId(), 0, null);
                    mAddContactDialog.dismiss();
                } catch (ContactManager.InvalidEmailException e) {
                    TrackerManager.getInstance(mActivity).log("Invalid email address: " + emailAddress, e);
                    Toast.makeText(mActivity, R.string.msg_insert_mail, Toast.LENGTH_LONG).show();
                }
            }
        });

        if(savedInstanceState != null) {
            Bundle dialogState = savedInstanceState.getBundle(SAVED_DIALOG_STATE_KEY);
            if (dialogState != null) {
                mAddContactDialog.onRestoreInstanceState(dialogState);
            }
        }

        mContactsObserver = new ContactsContentObserver(new Handler(mActivity.getMainLooper()));
        mActivity.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, mContactsObserver);

        mAdapter = new ContactCursorAdapter(mActivity, null);
        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save add contact dialog state as well
        Bundle dialogState = mAddContactDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(SAVED_DIALOG_STATE_KEY, dialogState);
        }
    }

    private synchronized void setCursor() {
        if(mCursor != null && mAdapter != null && mCursor != mAdapter.getCursor()) {
            mAdapter.changeCursor(mCursor);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!mRefreshing) {
            if(mCursor == null) {
                refresh();
            } else {
                setCursor();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // set tracker screen label
        mActivity.getOverlayManager().setRootScreenId(SCREEN_ID);
        mActivity.getTrackerManager().trackScreenView(SCREEN_ID);
    }

    @Override
    public void onDestroy() {
        if(mActivity != null) {
            mActivity.getContentResolver().unregisterContentObserver(mContactsObserver);
        }

        // close adapter cursors
        mCursor = null;
        mAdapter.changeCursor(null);
        mAdapter = null;
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        ContactRaw contactRaw = mAdapter.getContactRaw(position);
        Chat chat = mAdapter.getContactRawChat(contactRaw);
        if(chat == null) {
            showHoldPopup(view);
        } else {
            Intent intent = new Intent(mActivity, ChatActivity.class);
            intent.putExtra(ChatActivity.PARAM_CHAT_ID, chat.getId());
            startActivity(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final ContactRaw contactRaw = mAdapter.getContactRaw(position);
        if (contactRaw.getEmail() == null) {
            mAddContactDialog.setContact(contactRaw);
            if(!mAddContactDialog.isShowing()) {
                mAddContactDialog.show();
            }
            return false;
        }
        mAddContactDialog.setContact(null);

        Chat tappedChat;
        try {
            tappedChat = GlobalManager.insertOrUpdateChatAndRecipients(mActivity, contactRaw);
        } catch (SQLException e) {
            TrackerManager.getInstance(mActivity.getApplicationContext()).logException(e);
            Toast.makeText(mActivity, R.string.msg_database_error, Toast.LENGTH_LONG).show();
            return true;
        }

        if(!mChatRecordOverlayController.triggerRecording(view, tappedChat)) {
            final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(mActivity);
            databaseHelper.lock();
            try {
                ChatManager.delete(databaseHelper.getWritableDatabase(), tappedChat.getId());
            } catch (SQLException e) {
                TrackerManager.getInstance(mActivity.getApplicationContext()).logException(e);
            }
            databaseHelper.unlock();
        }

        return true;
    }

}
