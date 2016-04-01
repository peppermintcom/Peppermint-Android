package com.peppermint.app.ui.chat.head;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.events.MessageEvent;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.ReceiverEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 24-02-2016.
 *
 * Service that controls the chat head overlay UI.
 */
public class ChatHeadService extends Service {

    public static final String ACTION_ENABLE = "com.peppermint.app.ChatHeadService.ENABLE";
    public static final String ACTION_DISABLE = "com.peppermint.app.ChatHeadService.DISABLE";

    public static final String ACTION_SHOW = "com.peppermint.app.ChatHeadService.SHOW";
    public static final String ACTION_HIDE = "com.peppermint.app.ChatHeadService.HIDE";

    private List<String> mVisibleActivities = new ArrayList<>();
    private List<Chat> mChats = new ArrayList<>();
    private ChatHeadController mChatHeadController;

    private SenderPreferences mPreferences;
    private MessagesServiceManager mMessagesServiceManager;
    private boolean mWasVisible = false;

    private final BroadcastReceiver mRotationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(needsToStop()) {
                return;
            }

            if(mChatHeadController != null) {
                mChatHeadController.requestLayout();
            }
        }
    };
    private final IntentFilter mRotationIntentFilter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);

    protected ChatHeadServiceBinder mBinder = new ChatHeadServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class ChatHeadServiceBinder extends Binder {
        void enable() { refreshChatHeadController(); }
        void disable() { stopSelf(); }
        void show() { refreshChatHeadController(); }
        boolean hide() { if(mChatHeadController != null) { return mChatHeadController.hide(); } return false; }

        void addVisibleActivity(String activityFullClassName) {
            mVisibleActivities.add(activityFullClassName);
            boolean hidden = hide();
            if(mVisibleActivities.size() <= 1) {
                mWasVisible = hidden;
            }
        }

        boolean removeVisibleActivity(String activityFullClassName) {
            boolean ret = mVisibleActivities.remove(activityFullClassName);
            if(mWasVisible) {
                show();
            }
            return ret;
        }
    }

    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction() != null) {
            if(intent.getAction().compareTo(ACTION_ENABLE) == 0) {
                refreshChatHeadController();
            } else if(intent.getAction().compareTo(ACTION_DISABLE) == 0) {
                stopSelf();
            } else if(intent.getAction().compareTo(ACTION_SHOW) == 0) {
                refreshChatHeadController();
            } else if(intent.getAction().compareTo(ACTION_HIDE) == 0) {
                mChatHeadController.hide();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = new SenderPreferences(this);

        mMessagesServiceManager = new MessagesServiceManager(this);
        mMessagesServiceManager.startAndBind();

        PeppermintEventBus.registerMessages(this);

        mChatHeadController = new ChatHeadController(this);
        mChatHeadController.requestLayout();

        registerReceiver(mRotationBroadcastReceiver, mRotationIntentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mRotationBroadcastReceiver);

        if(mChatHeadController != null) {
            mChatHeadController.hide();
            mChatHeadController.destroy();
        }
        mChatHeadController = null;
        mMessagesServiceManager.unbind();
        PeppermintEventBus.unregisterMessages(this);
    }

    private boolean refreshChatHeadController() {
        if(needsToStop()) {
            return false;
        }

        mWasVisible = true;

        if(mVisibleActivities.size() > 0) {
            return false;
        }

        mChats.clear();
        SQLiteDatabase db = DatabaseHelper.getInstance(this).getReadableDatabase();
        Cursor chatCursor = ChatManager.getAll(db);
        int i = 0;
        while(chatCursor.moveToNext() && i < ChatHeadChainView.MAX_CHAT_HEADS) {
            Chat chat = ChatManager.getChatFromCursor(db, chatCursor);
            chat.setAmountUnopened(MessageManager.getUnopenedCountByChat(db, chat.getId()));
            chat.setLastReceivedUnplayedId(MessageManager.getLastAutoPlayMessageIdByChat(db, chat.getId()));
            mChats.add(0, chat);
            i++;
        }
        chatCursor.close();

        // reverse the order (push the most recent last)
        for(Chat chat : mChats) {
            mChatHeadController.pushChat(chat);
        }

        mChatHeadController.show();

        return true;
    }

    public void onEventMainThread(ReceiverEvent event) {
        if(event.getType() == ReceiverEvent.EVENT_RECEIVED) {
            refreshChatHeadController();
        }
    }

    public void onEventMainThread(MessageEvent event) {
        if(event.getType() == MessageEvent.EVENT_MARK_PLAYED) {
            if(needsToStop()) {
                return;
            }

            if(mChats.size() > 0) {
                SQLiteDatabase db = DatabaseHelper.getInstance(this).getReadableDatabase();

                for(Chat chat : mChats) {
                    if(chat.getId() == event.getMessage().getChatId()) {
                        chat.setAmountUnopened(MessageManager.getUnopenedCountByChat(db, chat.getId()));
                        chat.setLastReceivedUnplayedId(MessageManager.getLastAutoPlayMessageIdByChat(db, chat.getId()));
                    }
                }
            }

            mChatHeadController.invalidate();
        }
    }

    private boolean needsToStop() {
        // stop in case chat heads are explicitly disabled
        if(!mPreferences.areChatHeadsEnabled()) {
            stopSelf();
            return true;
        }

        // stop in case there's no permission to draw overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!Settings.canDrawOverlays(this)) {
                mPreferences.setChatHeadsEnabled(false);
                stopSelf();
                return true;
            }
        }

        return false;
    }

}
