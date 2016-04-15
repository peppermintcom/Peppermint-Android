package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
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
import com.peppermint.app.tracking.TrackerManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AllContactsListFragment extends ContactListFragment {

    private static final String SCREEN_ID = "AllContacts";

    private static final List<String> MIMETYPES = new ArrayList<>();
    static {
        MIMETYPES.add(ContactData.EMAIL_MIMETYPE);
        MIMETYPES.add(ContactData.PHONE_MIMETYPE);
    }

    // the recipient list
    private ContactCursorAdapter mAdapter;
    private FilteredCursor mCursor;

    @Override
    protected Object onAsyncRefresh(Context context, String searchName, String searchVia) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            // get normal full, email or phone contact list
            FilteredCursor cursor = (FilteredCursor) ContactManager.get(context, null, searchName, false, MIMETYPES, searchVia);
            if (cursor.getOriginalCursor().getCount() <= 0 && searchName != null && searchVia != null) {
                cursor.close();
                cursor = (FilteredCursor) ContactManager.get(context, null, null, false, MIMETYPES, searchVia);
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
        mCursor = (FilteredCursor) data;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            setCursor();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new ContactCursorAdapter(mActivity, null);
        getListView().setAdapter(mAdapter);

        if(!mRefreshing) {
            setCursor();
        }
    }

    private synchronized void setCursor() {
        if(mCursor != null && mAdapter != null && mCursor != mAdapter.getCursor()) {
            mAdapter.changeCursor(mCursor);
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
        // close adapter cursors
        mCursor = null;
        mAdapter.changeCursor(null);
        mAdapter = null;
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        showHoldPopup(view);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final ContactRaw contactRaw = mAdapter.getContactRaw(position);

        Chat tappedChat;
        try {
            tappedChat = GlobalManager.insertOrUpdateChatAndRecipients(mActivity, contactRaw);
        } catch (SQLException e) {
            TrackerManager.getInstance(mActivity.getApplicationContext()).logException(e);
            Toast.makeText(mActivity, R.string.msg_database_error, Toast.LENGTH_LONG).show();
            return true;
        }

        if(!mActivity.getChatRecordOverlayController().triggerRecording(view, tappedChat)) {
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
