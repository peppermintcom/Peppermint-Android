package com.peppermint.app.ui.chat;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.Recording;
import com.peppermint.app.sending.ReceiverEvent;
import com.peppermint.app.sending.SenderEvent;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * New recipient/contact fragment.
 */
public class ChatFragment extends ChatRecordOverlayFragment implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = ChatFragment.class.getSimpleName();

    public static final String PARAM_RECIPIENT = TAG + "_paramRecipient";
    public static final String PARAM_CHAT = TAG + "_paramChat";

    private static final String SCREEN_ID = "Chat";

    // GENERIC
    private ChatActivity mActivity;
    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    // UI
    private RelativeLayout mRecordLayout;

    // DATA
    private Chat mChat;
    private Recipient mRecipient;

    private MessageCursorAdapter mAdapter;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (ChatActivity) context;
        mDatabaseHelper = new DatabaseHelper(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.f_chat, null);

        mRecordLayout = (RelativeLayout) v.findViewById(R.id.lytRecord);
        mRecordLayout.setOnLongClickListener(this);
        mRecordLayout.setOnTouchListener(getChatRecordOverlay());
        /*v.setOnTouchListener(getChatRecordOverlay());*/

        // get arguments
        Recording argRecording = null;
        Bundle args = getArguments();
        if(args != null) {
            mRecipient = (Recipient) args.getSerializable(PARAM_RECIPIENT);
            mChat = (Chat) args.getSerializable(PARAM_CHAT);
        }

        // check if there's the mandatory recipient data
        if(mRecipient == null || (mRecipient.getId() <= 0 && mRecipient.getVia() == null)) {
            mActivity.getTrackerManager().logException(new RuntimeException("Recipient is null on ChatActivity intent!"));
            mActivity.finish();
            return null;
        }

        // try to find the recipient in the database
        if(mRecipient.getId() <= 0) {
            Recipient foundRecipient = Recipient.getByViaOrId(getDatabase(), mRecipient.getVia(), 0);
            if(foundRecipient != null) {
                mRecipient.setId(foundRecipient.getId());
            }
        }

        // try to get additional recipient data from contacts
        if(mRecipient.getContactId() <= 0 || mRecipient.getRawId() <= 0 || mRecipient.getPhotoUri() == null) {
            RecipientAdapterUtils.fillRecipientDetails(mActivity, mRecipient);
        }

        // create new chat instance if there's none
        if(mChat == null) {
            mChat = Chat.getByMainRecipient(getDatabase(), mRecipient.getId());
            if(mChat == null) {
                mChat = new Chat(mRecipient, DateContainer.getCurrentTimestamp());
            }
        }

        // set the action bar recipient data
        mActivity.setActionBarData(mRecipient.getName(), mRecipient.getVia(), mRecipient.getPhotoUri());

        if(savedInstanceState != null) {
        }

        refreshList();

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        TrackerManager.getInstance(getActivity().getApplicationContext()).trackScreenView(SCREEN_ID);
    }

    @Override
    public void onStart() {
        super.onStart();
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
        if(mDatabase != null) {
            mDatabase.close();
            mDatabase = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mAdapter.destroy();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public boolean onLongClick(View v) {
        return triggerRecording(v, mRecipient);
    }

    @Override
    public void onSendStarted(SenderEvent event) {
        // add to UI
        refreshList();
    }

    @Override
    public void onReceivedMessage(ReceiverEvent event) {
        refreshList();
    }

    @Override
    public void onBoundSendService() {
        refreshList();
    }

    private void refreshList() {
        Cursor cursor = Message.getByChatCursor(getDatabase(), mChat.getId());
        if(mAdapter == null) {
            mAdapter = new MessageCursorAdapter(mActivity, cursor, getDatabase(), mActivity.getTrackerManager());
            setListAdapter(mAdapter);
        } else {
            mAdapter.swapCursor(cursor);
            mAdapter.notifyDataSetChanged();
        }
    }

    private SQLiteDatabase getDatabase() {
        if(mDatabase == null || !mDatabase.isOpen()) {
            mDatabase = mDatabaseHelper.getReadableDatabase();
        }
        return mDatabase;
    }
}
