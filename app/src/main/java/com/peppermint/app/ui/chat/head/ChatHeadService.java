package com.peppermint.app.ui.chat.head;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.view.View;

import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.cloud.ReceiverEvent;
import com.peppermint.app.cloud.senders.SenderEvent;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.ui.chat.ChatController;
import com.peppermint.app.ui.chat.RecipientDataGUI;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;

/**
 * Created by Nuno Luz on 24-02-2016.
 */
public class ChatHeadService extends Service implements MessagesServiceManager.ReceiverListener, MessagesServiceManager.SenderListener, RecipientDataGUI, ChatRecordOverlayController.Callbacks {

    public static final String ACTION_ENABLE = "com.peppermint.app.ChatHeadService.ENABLE";
    public static final String ACTION_DISABLE = "com.peppermint.app.ChatHeadService.DISABLE";

    private static final int MAX_CHAT_HEADS = 4;

    private ChatHeadController mChatHeadController;

    private SenderPreferences mPreferences;
    private MessagesServiceManager mMessagesServiceManager;

    private View mChatLayout;
    private ChatController mChatController;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mPreferences.isOverlayAllowed()) {
            mChatHeadController.requestLayout();
        }
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null && intent.getAction() != null && mPreferences.isOverlayAllowed()) {
            if(intent.getAction().compareTo(ACTION_ENABLE) == 0) {
                refreshChatHeadController();
            } else if(intent.getAction().compareTo(ACTION_DISABLE) == 0) {
                if(mChatHeadController != null) {
                    mChatHeadController.disable();
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = new SenderPreferences(this);
        mMessagesServiceManager = new MessagesServiceManager(this);
        mMessagesServiceManager.startAndBind();
        mMessagesServiceManager.addReceiverListener(this);
        mMessagesServiceManager.addSenderListener(this);

        /*OverlayManager overlayManager =

        mChatLayout = LayoutInflater.from(this).inflate(R.layout.a_chat_head_layout, null);
        mChatController = new ChatController(this, this, this);
        mChatController.init(mChatLayout, );*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mPreferences.isOverlayAllowed() && mChatHeadController != null) {
            mChatHeadController.disable();
        }
        mMessagesServiceManager.unbind();
    }

    private void refreshChatHeadController() {
        if(mPreferences.isOverlayAllowed() && mChatHeadController != null) {
            mChatHeadController.disable();
        }

        mChatHeadController = new ChatHeadController(this);

        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor chatCursor = ChatManager.getAll(db);
        int i = 0;
        while(chatCursor.moveToNext() && i < MAX_CHAT_HEADS) {
            Chat chat = ChatManager.getChatFromCursor(db, chatCursor);
            chat.setAmountUnopened(MessageManager.getUnopenedCountByChat(db, chat.getId()));
            mChatHeadController.addChat(chat);
            i++;
        }
        chatCursor.close();

        if(mPreferences.isOverlayAllowed()) {
            mChatHeadController.requestLayout();
            mChatHeadController.enable();
        }
    }

    @Override
    public void onReceivedMessage(ReceiverEvent event) {
        refreshChatHeadController();
    }

    @Override
    public void onSendStarted(SenderEvent event) {
        refreshChatHeadController();
    }

    @Override
    public void onSendCancelled(SenderEvent event) {

    }

    @Override
    public void onSendError(SenderEvent event) {
    }

    @Override
    public void onSendFinished(SenderEvent event) {
    }

    @Override
    public void onSendProgress(SenderEvent event) {

    }

    @Override
    public void onSendQueued(SenderEvent event) {
    }

    @Override
    public void onNewContact(Intent intentToLaunchActivity) {

    }

    @Override
    public void setRecipientData(String recipientName, String recipientVia, String recipientPhotoUri) {

    }
}
