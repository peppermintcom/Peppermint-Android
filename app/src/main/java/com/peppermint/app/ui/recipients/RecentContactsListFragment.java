package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.ContactData;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.ui.chat.ChatCursorAdapter;

import java.util.Map;

public class RecentContactsListFragment extends ContactListFragment {

    private static final String SCREEN_ID = "RecentContacts";

    private ChatCursorAdapter mAdapter;
    private Cursor mCursor;
    private Map<Long, ContactData> mPeppermintContacts;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new ChatCursorAdapter(mActivity, null, mActivity.getTrackerManager());
        getListView().setAdapter(mAdapter);

        if(!mRefreshing) {
            setCursor();
        }
    }

    private synchronized void setCursor() {
        if(mCursor != null && mAdapter != null && mCursor != mAdapter.getCursor()) {
            mAdapter.setPeppermintContacts(mPeppermintContacts);
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

        if(mPeppermintContacts != null) {
            mPeppermintContacts.clear();
            mPeppermintContacts = null;
        }

        super.onDestroy();
    }

    @Override
    protected Object onAsyncRefresh(Context context, String searchName, String searchVia) {
        mPeppermintContacts = ContactManager.getPeppermintContacts(context);
        return ChatManager.getAll(DatabaseHelper.getInstance(context).getReadableDatabase(), true);
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
        mCursor = (Cursor) data;
        setCursor();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        Intent intent = new Intent(mActivity, ChatActivity.class);
        intent.putExtra(ChatActivity.PARAM_CHAT_ID, mAdapter.getChat(position).getId());
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        mActivity.getChatRecordOverlayController().triggerRecording(view, mAdapter.getChat(position));
        return true;
    }

}
