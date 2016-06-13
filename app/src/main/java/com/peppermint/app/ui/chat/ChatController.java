package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.peppermint.app.R;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiNoAccountException;
import com.peppermint.app.dal.DataObjectEvent;
import com.peppermint.app.dal.DatabaseHelper;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.chat.ChatManager;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.message.MessageManager;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.services.authenticator.AuthenticatorUtils;
import com.peppermint.app.services.messenger.MessengerSendEvent;
import com.peppermint.app.services.sync.SyncEvent;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.OverlayManager;
import com.peppermint.app.ui.base.TouchInterceptable;
import com.peppermint.app.ui.base.dialogs.CustomListDialog;
import com.peppermint.app.ui.base.navigation.NavigationItem;
import com.peppermint.app.ui.base.navigation.NavigationListAdapter;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.utils.SameAsyncTaskExecutor;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

/**
 * Created by Nuno Luz on 04-03-2016.
 *
 * Controls a chat/conversation/list of messages. Detaching the controller code from the
 * Activity/Fragment allows it to be used in chat heads (from a Service).
 */
public class ChatController extends ChatRecordOverlayController implements View.OnLongClickListener {

    private static final String TAG = ChatController.class.getSimpleName();

    private static final String STATE_DIALOG = TAG + "_stateDialog";
    private static final String STATE_MESSAGE_ID_WITH_ERROR = TAG + "_messageIdWithError";

    private static final String SCREEN_ID = "Chat";

    protected class RefreshMessageCursorLoader extends SameAsyncTaskExecutor<Void, Void, Cursor> {
        public RefreshMessageCursorLoader(Context mContext) {
            super(mContext);
        }

        @Override
        protected Cursor doInBackground(SameAsyncTask asyncTask, Void... params) {
            return MessageManager.getInstance(getContext()).getByChatId(getDatabase(), mChat.getId());
        }

        @Override
        protected void onCancelled(Cursor cursor) {
            if(cursor != null) {
                cursor.close();
            }
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if(ChatController.this.getContext() == null) {
                if(cursor != null) {
                    cursor.close();
                }
                return;
            }

            if(mAdapter == null) {
                mAdapter = new ChatMessageCursorAdapter(ChatController.this.getContext(), getMessagesServiceManager(),
                        getPlayerServiceManager(), cursor);
                mAdapter.setExclamationClickListener(new MessageView.ExclamationClickListener() {
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

    private DatabaseHelper mDatabaseHelper;
    private SmoothProgressBar mProgressBar;
    private RefreshMessageCursorLoader mRefreshMessageCursorLoader;

    // ui
    private RecipientDataGUI mRecipientDataGUI;
    private ListView mListView;

    // data
    private Chat mChat;
    private ChatMessageCursorAdapter mAdapter;

    // error dialog
    private CustomListDialog mErrorDialog;
    private long mMessageIdWithError;
    private boolean mAllowSkipNotification = true;

    public ChatController(Context context, RecipientDataGUI recipientDataGUI) {
        super(context);
        this.mRecipientDataGUI = recipientDataGUI;
        mDatabaseHelper = DatabaseHelper.getInstance(context);
        mRefreshMessageCursorLoader = new RefreshMessageCursorLoader(context);
    }

    @Override
    public void init(View rootView, OverlayManager overlayManager, TouchInterceptable touchInterceptable, Bundle savedInstanceState) {
        super.init(rootView, overlayManager, touchInterceptable, savedInstanceState);

        final List<NavigationItem> errorOptions = new ArrayList<>();
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

                final Message message = new Message();
                message.setId(mMessageIdWithError);

                switch (position) {
                    case 0: // retry
                        getMessagesServiceManager().retry(message);
                        break;
                    case 1: // delete
                        getMessagesServiceManager().cancel(message);
                        refreshList();
                        break;
                }
            }
        });

        final NavigationListAdapter errorAdapter = new NavigationListAdapter(getContext(), errorOptions);
        mErrorDialog.setListAdapter(errorAdapter);

        mProgressBar = (SmoothProgressBar) rootView.getRootView().findViewById(R.id.smoothProgress);
        setLoading(false);
        mListView = (ListView) rootView.findViewById(android.R.id.list);

        final ViewGroup recordLayout = (ViewGroup) rootView.findViewById(R.id.lytRecord);
        recordLayout.setOnLongClickListener(this);

        if(savedInstanceState != null) {
            final Bundle dialogState = savedInstanceState.getBundle(STATE_DIALOG);
            if (dialogState != null) {
                mErrorDialog.onRestoreInstanceState(dialogState);
            }
            mMessageIdWithError = savedInstanceState.getLong(STATE_MESSAGE_ID_WITH_ERROR);
        }
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        final Bundle dialogState = mErrorDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(STATE_DIALOG, dialogState);
        }
        outState.putLong(STATE_MESSAGE_ID_WITH_ERROR, mMessageIdWithError);
    }

    @Override
    public void start() {
        super.start();

        final Context context = getContext();
        TrackerManager.getInstance(context).trackScreenView(SCREEN_ID);

        if(mProgressBar != null) {
            final AuthenticatorUtils authenticatorUtils = new AuthenticatorUtils(context);
            try {
                if (authenticatorUtils.isPerformingSync()) {
                    setLoading(true);
                } else {
                    setLoading(false);
                }
            } catch (PeppermintApiNoAccountException e) {
                Log.w(TAG, "Not authenticated!", e);
            }
        }
    }

    @Override
    public void stop() {
        if(mRefreshMessageCursorLoader != null) {
            mRefreshMessageCursorLoader.cancel(true);
        }
        if(mAdapter != null) {
            mAdapter.changeCursor(null);
        }
        super.stop();
    }

    public void setLoading(boolean loading) {
        if(mProgressBar == null) {
            return;
        }
        final boolean currentlyLoading = mProgressBar.getVisibility() == View.VISIBLE;
        if(loading != currentlyLoading) {
            if(loading) {
                mProgressBar.progressiveStart();
                mProgressBar.setVisibility(View.VISIBLE);
            } else {
                mProgressBar.setVisibility(View.GONE);
                mProgressBar.progressiveStop();
            }
        }
    }

    public void stopPlayer() {
        if(getPlayerServiceManager() != null && getPlayerServiceManager().isBound()) {
            getPlayerServiceManager().stop();
        }
    }

    @Override
    public void deinit() {
        stopPlayer();

        if(mAdapter != null) {
            mAdapter.changeCursor(null);
            mAdapter.destroy();
            mAdapter = null;
        }
        dismissErrorDialog();

        super.deinit();
    }

    @Override
    public boolean onLongClick(View v) {
        if(getPlayerServiceManager() != null && getPlayerServiceManager().isBound()) {
            getPlayerServiceManager().pause();
        }

        triggerRecording(v, mChat);

        return true;
    }

    @Override
    public void onEventMainThread(SyncEvent event) {
        super.onEventMainThread(event);
        if(mChat == null) {
            return;
        }

        setLoading(event.getType() == SyncEvent.EVENT_STARTED || event.getType() == SyncEvent.EVENT_PROGRESS);

        if(event.getType() == SyncEvent.EVENT_FINISHED || event.getType() == SyncEvent.EVENT_PROGRESS) {
            if(event.getAffectedChatIdSet().contains(mChat.getId())) {
                refreshList();
            }
        }
    }

    public void onEventMainThread(DataObjectEvent<Message> event) {
        super.onEventMainThread(event);
        if(mChat != null && event.getDataObject().getChatId() == mChat.getId()) {
            refreshList();
            if (mAllowSkipNotification && event.getDataObject().isReceived() && Utils.isScreenOnAndUnlocked(getContext())) {
                event.setSkipNotifications(true);
            }
        }
    }

    @Override
    public void onEventMainThread(MessengerSendEvent event) {
        super.onEventMainThread(event);
        if(mChat == null) {
            return;
        }

        // add to ui
        if(event.getSenderTask().getMessage().getChatId() == mChat.getId()) {
            refreshList();
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

    public void refreshList() {
        if(mChat == null || !getMessagesServiceManager().isBound() || !getPlayerServiceManager().isBound()) {
            return;
        }
        mRefreshMessageCursorLoader.execute();
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
        setChat(ChatManager.getInstance(getContext()).getChatById(getDatabase(), chatId));
    }

    public void setChat(Chat chat) {
        this.mChat = chat;
        if(mChat == null) {
            return;
        }
        final Recipient recipient = mChat.getRecipientList().get(0);
        if(recipient != null && mRecipientDataGUI != null) {
            mRecipientDataGUI.setRecipientData(recipient.getDisplayName(),
                    recipient.getVia(),
                    recipient.getPhotoUri());
        }
        refreshList();
    }

    public void setAllowSkipNotification(boolean mAllowSkipNotification) {
        this.mAllowSkipNotification = mAllowSkipNotification;
    }
}
