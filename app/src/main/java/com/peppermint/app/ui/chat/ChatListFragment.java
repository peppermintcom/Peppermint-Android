package com.peppermint.app.ui.chat;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.peppermint.app.R;
import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.cloud.ReceiverEvent;
import com.peppermint.app.cloud.senders.SenderEvent;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * New recipient/contact fragment.
 */
public class ChatListFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = ChatListFragment.class.getSimpleName();

    private static final String SCREEN_ID = "ChatList";

    // GENERIC
    private CustomActionBarActivity mActivity;
    private MessagesServiceManager mMessagesServiceManager;
    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    // DATA
    private ChatCursorAdapter mAdapter;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (CustomActionBarActivity) context;
        mDatabaseHelper = new DatabaseHelper(context);
        mMessagesServiceManager = new MessagesServiceManager(mActivity);
        mMessagesServiceManager.addServiceListener(mServiceListener);
        mMessagesServiceManager.addSenderListener(mSenderListener);
        mMessagesServiceManager.addReceiverListener(mReceiverListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.f_chatlist_layout, null);

        if(savedInstanceState != null) {
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
        TrackerManager.getInstance(getActivity().getApplicationContext()).trackScreenView(SCREEN_ID);
    }

    @Override
    public void onStart() {
        super.onStart();
        mMessagesServiceManager.startAndBind();
        if(mAdapter != null) {
            mAdapter.setDatabase(getDatabase());
        }
    }

    @Override
    public void onPause() {
        Utils.hideKeyboard(mActivity, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        super.onPause();
    }

    @Override
    public void onStop() {
        mMessagesServiceManager.unbind();
        mAdapter.changeCursor(null);
        if(mDatabase != null) {
            mDatabase.close();
            mDatabase = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mMessagesServiceManager.removeReceiverListener(mReceiverListener);
        mMessagesServiceManager.removeSenderListener(mSenderListener);
        mActivity = null;
        super.onDestroy();
    }

    private MessagesServiceManager.ServiceListener mServiceListener = new MessagesServiceManager.ServiceListener() {
        @Override
        public void onBoundSendService() {
            mMessagesServiceManager.removeAllNotifications();
        }
    };

    private MessagesServiceManager.SenderListener mSenderListener = new MessagesServiceManager.SenderListener() {
        @Override
        public void onSendStarted(SenderEvent event) {
        }

        @Override
        public void onSendCancelled(SenderEvent event) {

        }

        @Override
        public void onSendError(SenderEvent event) {

        }

        @Override
        public void onSendFinished(SenderEvent event) {
            // add to UI
            refreshList();
        }

        @Override
        public void onSendProgress(SenderEvent event) {

        }

        @Override
        public void onSendQueued(SenderEvent event) {

        }
    };

    private MessagesServiceManager.ReceiverListener mReceiverListener = new MessagesServiceManager.ReceiverListener() {
        @Override
        public void onReceivedMessage(ReceiverEvent event) {
            refreshList();
            event.setDoNotShowNotification(true);
        }
    };

    private void refreshList() {
        Cursor cursor = Chat.getAllCursor(getDatabase());
        if(mAdapter == null) {
            mAdapter = new ChatCursorAdapter(mActivity, cursor, getDatabase(), mActivity.getTrackerManager());
            setListAdapter(mAdapter);
        } else {
            mAdapter.changeCursor(cursor);
            mAdapter.notifyDataSetChanged();
        }
    }

    private SQLiteDatabase getDatabase() {
        if(mDatabase == null || !mDatabase.isOpen()) {
            mDatabase = mDatabaseHelper.getReadableDatabase();
        }
        return mDatabase;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Chat chat = mAdapter.getChat(position);
        Intent chatIntent = new Intent(mActivity, ChatActivity.class);
        chatIntent.putExtra(ChatFragment.PARAM_CHAT, chat);
        chatIntent.putExtra(ChatFragment.PARAM_RECIPIENT, chat.getMainRecipient());
        startActivity(chatIntent);
    }
}
