package com.peppermint.app.ui.chat.recorder;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.peppermint.app.services.player.PlayerServiceManager;
import com.peppermint.app.R;
import com.peppermint.app.services.messenger.MessagesServiceManager;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.recording.Recording;
import com.peppermint.app.PeppermintEventBus;
import com.peppermint.app.services.player.PlayerEvent;
import com.peppermint.app.services.messenger.ReceiverEvent;
import com.peppermint.app.services.recorder.RecorderEvent;
import com.peppermint.app.services.messenger.SenderEvent;
import com.peppermint.app.services.messenger.SyncEvent;
import com.peppermint.app.ui.base.OverlayManager;
import com.peppermint.app.ui.base.TouchInterceptable;

/**
 * Created by Nuno Luz on 04-03-2016.
 *
 * Controls the "tap and hold to record, release to send" record overlay.
 */
public class ChatRecordOverlayController implements ChatRecordOverlay.OnRecordingFinishedCallback,
        MessagesServiceManager.ServiceListener,
        PlayerServiceManager.PlayServiceListener {

    private static final String TAG = ChatRecordOverlayController.class.getSimpleName();

    // keys to save the instance state
    private static final String CHAT_TAPPED_KEY = TAG + "_ChatTapped";

    // contextual/external instances
    private Context mContext;
    private SenderPreferences mPreferences;
    private OverlayManager mOverlayManager;
    private TouchInterceptable mTouchInterceptable;

    private MessagesServiceManager mMessagesServiceManager;
    private PlayerServiceManager mPlayerServiceManager;

    // overlay
    private ChatRecordOverlay mChatRecordOverlay;

    private Chat mChat;
    private boolean mStarted = false;

    public ChatRecordOverlayController(Context context) {
        mContext = context;

        mPreferences = new SenderPreferences(mContext);

        // services
        mMessagesServiceManager = new MessagesServiceManager(mContext);
        mPlayerServiceManager = new PlayerServiceManager(mContext);
    }

    public void init(View rootView, OverlayManager overlayManager, TouchInterceptable touchInterceptable, Bundle savedInstanceState) {
        mOverlayManager = overlayManager;
        mTouchInterceptable = touchInterceptable;

        mMessagesServiceManager.addServiceListener(this);
        mPlayerServiceManager.addServiceListener(this);
        PeppermintEventBus.registerMessages(this);

        // start services
        mMessagesServiceManager.start();
        mPlayerServiceManager.start();

        if (savedInstanceState != null) {
            mChat = (Chat) savedInstanceState.getSerializable(CHAT_TAPPED_KEY);
        }

        // create overlay
        mChatRecordOverlay = (ChatRecordOverlay) mOverlayManager.createOverlay(new ChatRecordOverlay(mTouchInterceptable));
        mChatRecordOverlay.setOnRecordingFinishedCallback(this);
    }

    public void saveInstanceState(Bundle outState) {
        // save tapped recipient in case add contact dialog is showing
        outState.putSerializable(CHAT_TAPPED_KEY, mChat);
    }

    public void start() {
        mStarted = true;
        mMessagesServiceManager.bind();
        mPlayerServiceManager.bind();
        mChatRecordOverlay.bindService();
    }

    public void stop() {
        mChatRecordOverlay.hide(false, true);
        mChatRecordOverlay.unbindService();
        mPlayerServiceManager.unbind();
        mMessagesServiceManager.unbind();
        mStarted = false;
    }

    public void deinit() {
        PeppermintEventBus.unregisterMessages(this);
        mMessagesServiceManager.removeServiceListener(this);
        mPlayerServiceManager.removeServiceListener(this);
    }

    public boolean triggerRecording(View boundsView, Chat chat) {
        String userRef = getPreferences().getFullName();
        if(userRef == null) {
            // not really important; it's only used for file naming purposes
            userRef = mContext.getString(R.string.peppermint_com);
        }

        this.mChat = chat;

        // start recording
        mChatRecordOverlay.setViewBounds(boundsView);
        mChatRecordOverlay.setChat(mChat);
        mChatRecordOverlay.setSenderName(userRef);
        mChatRecordOverlay.show(false);

        return true;
    }

    @Override
    public void onRecordingFinished(final RecorderEvent event) {
        sendMessage(event.getChat(), event.getRecording());
    }

    protected Message sendMessage(Chat chat, Recording recording) {
        return mMessagesServiceManager.send(chat, recording);
    }

    public ChatRecordOverlay getChatRecordOverlay() {
        return mChatRecordOverlay;
    }

    public SenderPreferences getPreferences() {
        return mPreferences;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public MessagesServiceManager getMessagesServiceManager() {
        return mMessagesServiceManager;
    }

    public PlayerServiceManager getPlayerServiceManager() {
        return mPlayerServiceManager;
    }

    public TouchInterceptable getTouchInterceptable() {
        return mTouchInterceptable;
    }

    public void setTouchInterceptable(TouchInterceptable mTouchInterceptable) {
        this.mTouchInterceptable = mTouchInterceptable;
    }

    public boolean isStarted() {
        return mStarted;
    }

    public void onEventMainThread(SyncEvent event) {
    }

    public void onEventMainThread(ReceiverEvent event) {
    }

    public void onEventMainThread(SenderEvent event) {
    }

    @Override
    public void onBoundSendService() {
    }

    public void onEventMainThread(PlayerEvent event) {
    }

    @Override
    public void onBoundPlayService() {
    }

}
