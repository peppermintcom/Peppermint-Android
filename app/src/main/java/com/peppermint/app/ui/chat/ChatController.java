package com.peppermint.app.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.cloud.ReceiverEvent;
import com.peppermint.app.cloud.SyncEvent;
import com.peppermint.app.cloud.senders.SenderEvent;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.ChatRecipient;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.TouchInterceptable;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.ui.views.NavigationListAdapter;
import com.peppermint.app.ui.views.dialogs.CustomListDialog;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 04-03-2016.
 */
public class ChatController extends ChatRecordOverlayController implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = ChatController.class.getSimpleName();

    private static final String STATE_DIALOG = TAG + "_stateDialog";
    private static final String STATE_MESSAGE_ID_WITH_ERROR = TAG + "_messageIdWithError";

    private static final String SCREEN_ID = "Chat";

    // GENERIC
    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    // UI
    private RecipientDataGUI mRecipientDataGUI;
    private RelativeLayout mRecordLayout;
    private ListView mListView;

    // DATA
    private Chat mChat;
    private long mAutoPlayMessageId;

    private ChatMessageCursorAdapter mAdapter;

    // error dialog
    private CustomListDialog mErrorDialog;
    private long mMessageIdWithError;

    // hold to record, release to send popup
    private Handler mHandler = new Handler();
    private PopupWindow mHoldPopup;
    private Runnable mShowPopupRunnable = new Runnable() {
        @Override
        public void run() {
            showPopup();
        }
    };
    private Runnable mDismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            dismissPopup();
        }
    };
    private View.OnTouchListener mTouchInterceptor = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                dismissPopup();
            }
            return false;
        }
    };

    public ChatController(Context context, RecipientDataGUI recipientDataGUI, Callbacks callbacks) {
        super(context, callbacks);
        this.mRecipientDataGUI = recipientDataGUI;
        mDatabaseHelper = DatabaseHelper.getInstance(context);
    }

    @Override
    public void init(View rootView, OverlayManager overlayManager, TouchInterceptable touchInterceptable, AuthenticationPolicyEnforcer authenticationPolicyEnforcer, Bundle savedInstanceState) {
        super.init(rootView, overlayManager, touchInterceptable, authenticationPolicyEnforcer, savedInstanceState);

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

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        // hold popup
        mHoldPopup = new PopupWindow(getContext());
        mHoldPopup.setContentView(layoutInflater.inflate(R.layout.v_recipients_popup, null));
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        //noinspection deprecation
        mHoldPopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mHoldPopup.setBackgroundDrawable(Utils.getDrawable(getContext(), R.drawable.img_coach));
        mHoldPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);
        // do not let the popup get in the way of user interaction
        mHoldPopup.setFocusable(false);
        mHoldPopup.setTouchable(false);

        mListView = (ListView) rootView.findViewById(android.R.id.list);

        mRecordLayout = (RelativeLayout) rootView.findViewById(R.id.lytRecord);
        mRecordLayout.setOnClickListener(this);
        mRecordLayout.setOnLongClickListener(this);

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
    }

    @Override
    public void start() {
        super.start();

        getTouchInterceptable().addTouchEventInterceptor(mTouchInterceptor);
        if (mAdapter != null) {
            mAdapter.setDatabase(getDatabase());
        }

        TrackerManager.getInstance(getContext().getApplicationContext()).trackScreenView(SCREEN_ID);

        if(!getPreferences().isChatTipShown()) {
            mHandler.postDelayed(mShowPopupRunnable, 100);
            getPreferences().setChatTipShown(true);
        }
    }

    @Override
    public void stop() {
        getTouchInterceptable().removeTouchEventInterceptor(mTouchInterceptor);

        if(mAdapter != null) {
            mAdapter.changeCursor(null);
        }

        super.stop();
    }

    @Override
    public void deinit() {
        if(mAdapter != null) {
            mAdapter.destroy();
        }
        dismissErrorDialog();

        super.deinit();
    }

    @Override
    public void onClick(View v) {
        showPopup();
    }

    @Override
    public boolean onLongClick(View v) {
        mAdapter.stopAllPlayers();
        return triggerRecording(v, mChat);
    }

    @Override
    public void onSendStarted(SenderEvent event) {
        // add to UI
        refreshList();
    }

    @Override
    public void onReceivedMessage(ReceiverEvent event) {
        refreshList();
        if(event.getMessage().getChatId() == mChat.getId()) {
            event.setDoNotShowNotification(true);
        }
    }

    @Override
    public void onSyncFinished(SyncEvent event) {
        refreshList();
    }

    @Override
    public void onBoundPlayService() {
        refreshList();
    }

    @Override
    public void onBoundSendService() {
        refreshList();
    }

    @Override
    public void onSendCancelled(SenderEvent event) {
        refreshList();
    }

    private void refreshList() {
        if(!getMessagesServiceManager().isBound() || !getPlayerServiceManager().isBound()) {
            return;
        }

        Cursor cursor = MessageManager.getByChatId(getDatabase(), mChat.getId());
        if(mAdapter == null) {
            mAdapter = new ChatMessageCursorAdapter(getContext(), getMessagesServiceManager(),
                    getPlayerServiceManager(), cursor, getDatabase(), TrackerManager.getInstance(getContext().getApplicationContext()));
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

        if(mAutoPlayMessageId > 0) {
            int count = mAdapter.getCount();
            Message chosenMessage = null;
            int chosenIndex = -1;
            for(int i=count-1; i>=0 && chosenMessage == null; i--) {
                Message message = mAdapter.getMessage(i);
                if(message.getId() == mAutoPlayMessageId) {
                    chosenMessage = message;
                    chosenIndex = i;
                }
            }
            mAutoPlayMessageId = 0;

            mListView.setSelection(chosenIndex);
            getPlayerServiceManager().play(chosenMessage, 0);
        } else if(mAutoPlayMessageId < 0) {
            mAutoPlayMessageId = 0;
            int chosenIndex = mAdapter.getCount() - 1;
            Message chosenMessage = mAdapter.getMessage(chosenIndex);
            mListView.setSelection(chosenIndex);
            getPlayerServiceManager().play(chosenMessage, 0);
        }
    }

    private SQLiteDatabase getDatabase() {
        if(mDatabase == null || !mDatabase.isOpen()) {
            mDatabase = mDatabaseHelper.getReadableDatabase();
        }
        return mDatabase;
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

    private void dismissPopup() {
        if(mHoldPopup.isShowing() && getContext() != null) {
            mHoldPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    private void showPopup() {
        if(!mHoldPopup.isShowing() && getContext() != null) {
            mHandler.removeCallbacks(mShowPopupRunnable);
            dismissPopup();
            int[] location = new int[2];
            mRecordLayout.getLocationOnScreen(location);
            mHoldPopup.showAtLocation(mRecordLayout, Gravity.LEFT | Gravity.TOP, mRecordLayout.getWidth() - Utils.dpToPx(getContext(), 270), location[1] - Utils.dpToPx(getContext(), 30));
            mHandler.postDelayed(mDismissPopupRunnable, 6000);
        }
    }

    public Chat getChat() {
        return mChat;
    }

    public void setChat(long chatId) {
        this.mChat = ChatManager.getChatById(getDatabase(), chatId);
        if(mChat == null) {
            return;
        }
        ChatRecipient recipient = mChat.getRecipientList().get(0);
        if(recipient != null && mRecipientDataGUI != null) {
            mRecipientDataGUI.setRecipientData(recipient.getDisplayName(),
                    recipient.getVia(),
                    recipient.getPhotoUri());
        }
    }

    public long getAutoPlayMessageId() {
        return mAutoPlayMessageId;
    }

    public void setAutoPlayMessageId(long mAutoPlayMessageId) {
        this.mAutoPlayMessageId = mAutoPlayMessageId;
    }
}
