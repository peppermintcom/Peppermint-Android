package com.peppermint.app.ui.chat;

import android.app.Activity;
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
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.sending.ReceiverEvent;
import com.peppermint.app.sending.SenderEvent;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.ui.views.NavigationListAdapter;
import com.peppermint.app.ui.views.dialogs.CustomListDialog;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * New recipient/contact fragment.
 */
public class ChatFragment extends ChatRecordOverlayFragment implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = ChatFragment.class.getSimpleName();

    private static final String STATE_DIALOG = TAG + "_stateDialog";
    private static final String STATE_MESSAGE_ID_WITH_ERROR = TAG + "_messageIdWithError";

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

    // error dialog
    private CustomListDialog mErrorDialog;
    private List<NavigationItem> mErrorOptions;
    private NavigationListAdapter mErrorAdapter;
    private long mMessageIdWithError;

    // hold to record, release to send popup
    private boolean mFirstRun = true;
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

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (ChatActivity) context;
        mDatabaseHelper = new DatabaseHelper(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mErrorOptions = new ArrayList<>();
        mErrorOptions.add(new NavigationItem(getString(R.string.retry), R.drawable.ic_drawer_refresh, null, true));
        mErrorOptions.add(new NavigationItem(getString(R.string.delete), R.drawable.ic_drawer_delete, null, true));

        mErrorDialog = new CustomListDialog(mActivity);
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
        mErrorAdapter = new NavigationListAdapter(mActivity, mErrorOptions);
        mErrorDialog.setListAdapter(mErrorAdapter);

        // hold popup
        mHoldPopup = new PopupWindow(getCustomActionBarActivity());
        mHoldPopup.setContentView(inflater.inflate(R.layout.v_recipients_popup, null));
        //noinspection deprecation
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        mHoldPopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mHoldPopup.setBackgroundDrawable(Utils.getDrawable(getCustomActionBarActivity(), R.drawable.img_coach));
        mHoldPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);
        // do not let the popup get in the way of user interaction
        mHoldPopup.setFocusable(false);
        mHoldPopup.setTouchable(false);

        View v = inflater.inflate(R.layout.f_chat, null);

        mRecordLayout = (RelativeLayout) v.findViewById(R.id.lytRecord);
        mRecordLayout.setOnClickListener(this);
        mRecordLayout.setOnLongClickListener(this);

        // get arguments
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
            Bundle dialogState = savedInstanceState.getBundle(STATE_DIALOG);
            if (dialogState != null) {
                mErrorDialog.onRestoreInstanceState(dialogState);
            }
            mMessageIdWithError = savedInstanceState.getLong(STATE_MESSAGE_ID_WITH_ERROR);
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle dialogState = mErrorDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(STATE_DIALOG, dialogState);
        }
        outState.putLong(STATE_MESSAGE_ID_WITH_ERROR, mMessageIdWithError);
    }

    @Override
    public void onResume() {
        super.onResume();
        TrackerManager.getInstance(getActivity().getApplicationContext()).trackScreenView(SCREEN_ID);

        if(mFirstRun) {
            mHandler.postDelayed(mShowPopupRunnable, 100);
            mFirstRun = false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getCustomActionBarActivity().addTouchEventInterceptor(mTouchInterceptor);
        if (mAdapter != null) {
            mAdapter.setDatabase(getDatabase());
        }
    }

    @Override
    public void onPause() {
        getCustomActionBarActivity().removeTouchEventInterceptor(mTouchInterceptor);
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
        dismissErrorDialog();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        showPopup();
    }

    @Override
    public boolean onLongClick(View v) {
        mAdapter.stopAllPlayers();
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
        if(event.getMessage().getRecipient().getId() == mRecipient.getId()) {
            event.setDoNotShowNotification(true);
        }
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
        if(!getMessagesServiceManager().isBound()) {
            return;
        }

        Cursor cursor = Message.getByChatCursor(getDatabase(), mChat.getId());
        if(mAdapter == null) {
            mAdapter = new MessageCursorAdapter(mActivity, getMessagesServiceManager(), getPlayerServiceManager(), cursor, getDatabase(), mActivity.getTrackerManager());
            mAdapter.setExclamationClickListener(new MessageCursorAdapter.ExclamationClickListener() {
                @Override
                public void onClick(View v, long messageId) {
                    showErrorDialog(messageId);
                }
            });
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
        if(mHoldPopup.isShowing() && !isDetached() && mActivity != null) {
            mHoldPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    private void showPopup() {
        mHandler.removeCallbacks(mShowPopupRunnable);
        dismissPopup();
        int[] location = new int[2];
        mRecordLayout.getLocationOnScreen(location);
        mHoldPopup.showAtLocation(mRecordLayout, Gravity.LEFT | Gravity.TOP, mRecordLayout.getWidth() - Utils.dpToPx(mActivity, 270), location[1] - Utils.dpToPx(mActivity, 30));
        mHandler.postDelayed(mDismissPopupRunnable, 6000);
    }

}
