package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.events.ReceiverEvent;
import com.peppermint.app.events.SenderEvent;
import com.peppermint.app.events.SyncEvent;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.TouchInterceptable;
import com.peppermint.app.ui.base.NavigationItem;
import com.peppermint.app.ui.base.NavigationListAdapter;
import com.peppermint.app.ui.base.dialogs.CustomListDialog;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 04-03-2016.
 *
 * Controls a conversation screen.
 */
public class ChatController extends ChatRecordOverlayController implements View.OnLongClickListener {

    private static final String TAG = ChatController.class.getSimpleName();

    private static final String STATE_DIALOG = TAG + "_stateDialog";
    private static final String STATE_MESSAGE_ID_WITH_ERROR = TAG + "_messageIdWithError";

    private static final String SCREEN_ID = "Chat";

    protected class RefreshMessageCursorAsyncTask extends AsyncTask<Void, Void, Cursor> {
        @Override
        protected Cursor doInBackground(Void... params) {
            return MessageManager.getByChatId(getDatabase(), mChat.getId());
        }

        @Override
        protected void onCancelled(Cursor cursor) {
            if(cursor != null) {
                cursor.close();
            }
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if(getContext() == null) {
                if(cursor != null) {
                    cursor.close();
                }
                return;
            }

            if(mAdapter == null) {
                mAdapter = new ChatMessageCursorAdapter(getContext(), getMessagesServiceManager(),
                        getPlayerServiceManager(), cursor, getDatabase(), TrackerManager.getInstance(getContext()));
                mAdapter.setExclamationClickListener(new ChatMessageCursorAdapter.ExclamationClickListener() {
                    @Override
                    public void onClick(View v, long messageId) {
                        showErrorDialog(messageId);
                    }
                });
                mListView.setAdapter(mAdapter);
            } else {
                mAdapter.changeCursor(cursor);
            }
        }
    }

    private RefreshMessageCursorAsyncTask mRefreshMessageCursorAsyncTask;

    // GENERIC
    private DatabaseHelper mDatabaseHelper;
    private boolean mSavedInstanceState = false;

    // UI
    private RecipientDataGUI mRecipientDataGUI;
    private ListView mListView;

    // DATA
    private Chat mChat;

    private ChatMessageCursorAdapter mAdapter;

    // error dialog
    private CustomListDialog mErrorDialog;
    private long mMessageIdWithError;

    public ChatController(Context context, RecipientDataGUI recipientDataGUI) {
        super(context);
        this.mRecipientDataGUI = recipientDataGUI;
        mDatabaseHelper = DatabaseHelper.getInstance(context);
    }

    @Override
    public void init(View rootView, OverlayManager overlayManager, TouchInterceptable touchInterceptable, Bundle savedInstanceState) {
        super.init(rootView, overlayManager, touchInterceptable, savedInstanceState);

        List<NavigationItem> errorOptions = new ArrayList<>();
        errorOptions.add(new NavigationItem(getContext().getString(R.string.retry), R.drawable.ic_drawer_refresh, null, true));
        errorOptions.add(new NavigationItem(getContext().getString(R.string.delete), R.drawable.ic_drawer_delete, null, true));

        mErrorDialog = new CustomListDialog(getContext());
        mErrorDialog.setCancelable(true);
        mErrorDialog.setNegativeButtonText(R.string.cancel);
        mErrorDialog.setNegativeButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mErrorDialog.dismiss();
            }
        });
        mErrorDialog.setTitleText(R.string.unable_to_send_message);
        mErrorDialog.setListOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dismissErrorDialog();

                Message message = new Message();
                message.setId(mMessageIdWithError);

                switch (position) {
                    case 0:
                        // retry
                        getMessagesServiceManager().retry(message);
                        break;
                    case 1:
                        // delete
                        getMessagesServiceManager().cancel(message);
                        refreshList();
                        break;
                }
            }
        });
        NavigationListAdapter errorAdapter = new NavigationListAdapter(getContext(), errorOptions);
        mErrorDialog.setListAdapter(errorAdapter);

        mListView = (ListView) rootView.findViewById(android.R.id.list);

        final ViewGroup recordLayout = (ViewGroup) rootView.findViewById(R.id.lytRecord);
        recordLayout.setOnLongClickListener(this);

        if(savedInstanceState != null) {
            Bundle dialogState = savedInstanceState.getBundle(STATE_DIALOG);
            if (dialogState != null) {
                mErrorDialog.onRestoreInstanceState(dialogState);
            }
            mMessageIdWithError = savedInstanceState.getLong(STATE_MESSAGE_ID_WITH_ERROR);
        }
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        Bundle dialogState = mErrorDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(STATE_DIALOG, dialogState);
        }
        outState.putLong(STATE_MESSAGE_ID_WITH_ERROR, mMessageIdWithError);
        mSavedInstanceState = true;
    }

    @Override
    public void start() {
        super.start();

        if (mAdapter != null) {
            mAdapter.setDatabase(getDatabase());
        }

        TrackerManager.getInstance(getContext().getApplicationContext()).trackScreenView(SCREEN_ID);
    }

    @Override
    public void stop() {
        if(mAdapter != null) {
            mAdapter.changeCursor(null);
        }

        super.stop();
    }

    @Override
    public void deinit() {
        if(mAdapter != null) {
            mAdapter.destroy(!mSavedInstanceState);
        }
        dismissErrorDialog();
        mSavedInstanceState = false;

        super.deinit();
    }

    @Override
    public boolean onLongClick(View v) {
        if(mAdapter == null) {
            return false;
        }

        mAdapter.stopAllPlayers();
        triggerRecording(v, mChat);

        return true;
    }

    @Override
    public void onEventMainThread(SyncEvent event) {
        super.onEventMainThread(event);
        if(event.getType() == SyncEvent.EVENT_FINISHED) {
            refreshList();
        }
    }

    @Override
    public void onEventMainThread(ReceiverEvent event) {
        super.onEventMainThread(event);
        if(mChat != null && event.getType() == ReceiverEvent.EVENT_RECEIVED) {
            refreshList();
            if(event.getMessage().getChatId() == mChat.getId() && Utils.isScreenOnAndUnlocked(getContext())) {
                event.setDoNotShowNotification(true);
            }
        }
    }

    @Override
    public void onEventMainThread(SenderEvent event) {
        super.onEventMainThread(event);
        if(event.getType() == SenderEvent.EVENT_STARTED ||
                event.getType() == SenderEvent.EVENT_CANCELLED) {
            // add to UI
            refreshList();
        } else if(event.getType() == SenderEvent.EVENT_NON_CANCELLABLE) {
            // will include transcription
            if(mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onBoundPlayService() {
        refreshList();
    }

    @Override
    public void onBoundSendService() {
        refreshList();
    }

    private void refreshList() {
        if(mChat == null || !getMessagesServiceManager().isBound() || !getPlayerServiceManager().isBound()) {
            return;
        }

        if(mRefreshMessageCursorAsyncTask != null) {
            mRefreshMessageCursorAsyncTask.cancel(true);
            mRefreshMessageCursorAsyncTask = null;
        }

        mRefreshMessageCursorAsyncTask = new RefreshMessageCursorAsyncTask();
        mRefreshMessageCursorAsyncTask.execute();
    }

    private SQLiteDatabase getDatabase() {
        return mDatabaseHelper.getReadableDatabase();
    }

    private void showErrorDialog(long messageIdWithError) {
        if(mErrorDialog != null && !mErrorDialog.isShowing()) {
            mMessageIdWithError = messageIdWithError;
            mErrorDialog.show();
        }
    }

    private void dismissErrorDialog() {
        if(mErrorDialog != null && mErrorDialog.isShowing()) {
            mErrorDialog.dismiss();
        }
    }

    public Chat getChat() {
        return mChat;
    }

    public void setChat(long chatId) {
        setChat(ChatManager.getChatById(getDatabase(), chatId));
    }

    public void setChat(Chat chat) {
        this.mChat = chat;
        if(mChat == null) {
            return;
        }
        Recipient recipient = mChat.getRecipientList().get(0);
        if(recipient != null && mRecipientDataGUI != null) {
            mRecipientDataGUI.setRecipientData(recipient.getDisplayName(),
                    recipient.getVia(),
                    recipient.getPhotoUri());
        }
        refreshList();
    }
}
