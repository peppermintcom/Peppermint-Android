package com.peppermint.app.ui.chat.head;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

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

    private static final String TAG = ChatHeadService.class.getSimpleName();
    private static final String PREF_VISIBLE = TAG + "_prefVisible";

    public static final String ACTION_ENABLE = "com.peppermint.app.ChatHeadService.ENABLE";
    public static final String ACTION_DISABLE = "com.peppermint.app.ChatHeadService.DISABLE";

    public static final String ACTION_SHOW = "com.peppermint.app.ChatHeadService.SHOW";
    public static final String ACTION_HIDE = "com.peppermint.app.ChatHeadService.HIDE";

    private List<String> mVisibleActivities = new ArrayList<>();

    private SenderPreferences mPreferences;
    private MessagesServiceManager mMessagesServiceManager;

    protected ChatHeadServiceBinder mBinder = new ChatHeadServiceBinder();

    /**
     * The service binder used by external components to interact with the service.
     */
    public class ChatHeadServiceBinder extends Binder {
        void enable() { refreshChatHeadController(); }
        void disable() { stopSelf(); }
        void show() { setVisible(true); refreshChatHeadController(); }
        boolean hide() { setVisible(false); refreshChatHeadController(); return true; }

        void addVisibleActivity(String activityFullClassName) {
            mVisibleActivities.add(activityFullClassName);
            Log.d(TAG, "addVisibleActivity() # " + activityFullClassName + " " + isVisible() + " " + mVisibleActivities);
            refreshChatHeadController();
        }

        boolean removeVisibleActivity(String activityFullClassName) {
            boolean ret = mVisibleActivities.remove(activityFullClassName);
            Log.d(TAG, "removeVisibleActivity() # " + activityFullClassName + " " + isVisible() + " " + mVisibleActivities);
            refreshChatHeadController();
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
                mBinder.enable();
            } else if(intent.getAction().compareTo(ACTION_DISABLE) == 0) {
                mBinder.disable();
            } else if(intent.getAction().compareTo(ACTION_SHOW) == 0) {
                mBinder.show();
            } else if(intent.getAction().compareTo(ACTION_HIDE) == 0) {
                mBinder.hide();
            }
        }

        return START_NOT_STICKY;
    }

    // chat head specific
    private Display mDisplay;

    // ui
    private ChatHeadGroupDisplayView mChatHeadView;
    private ChatDisplayView mChatView;

    private final BroadcastReceiver mHomeAppsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                if(mChatHeadView.isVisible() && (mChatHeadView.getState() == ChatHeadGroupDisplayView.STATE_EXPANDING || mChatHeadView.getState() == ChatHeadGroupDisplayView.STATE_EXPANDED)) {
                    mChatHeadView.shrink(null);
                }
            }
        }
    };
    private final IntentFilter mHomeAppsIntentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

    private final View.OnKeyListener mOnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                    case KeyEvent.KEYCODE_HOME:
                    case KeyEvent.KEYCODE_APP_SWITCH:
                        mChatHeadView.shrink(null);
                        break;
                }
            }
            return false;
        }
    };

    private ChatHeadGroupDisplayView.OnStateChangedListener mOnStateChangeListener = new ChatHeadGroupDisplayView.OnStateChangedListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            if(newState != ChatHeadGroupDisplayView.STATE_EXPANDED && (oldState == ChatHeadGroupDisplayView.STATE_EXPANDING || oldState == ChatHeadGroupDisplayView.STATE_EXPANDED)) {
                Log.d(TAG, "CHAT HEAD Shrink Started!");
                mChatView.hide();
            } else if(newState == ChatHeadGroupDisplayView.STATE_EXPANDING) {
                Log.d(TAG, "CHAT HEAD Expanding Started!");
                mChatView.show();
                mChatView.invalidate();
            }

            if(newState == ChatHeadGroupDisplayView.STATE_EXPANDING || newState == ChatHeadGroupDisplayView.STATE_EXPANDED || newState == ChatHeadGroupDisplayView.STATE_DRAGGING) {
                if(mDisplay.getDim() <= 0) {
                    mDisplay.dim(0.5f);
                }
            } else {
                if(mDisplay.getDim() > 0) {
                    mDisplay.dim(0f);
                }
                if (newState == ChatHeadGroupDisplayView.STATE_HIDDEN) {
                    if(mVisibleActivities.size() <= 0) {
                        setVisible(false);
                    }
                    refreshChatHeadController();
                }
            }
        }
    };

    private ChatHeadGroupDisplayView.OnChatHeadSelectedListener mOnChatHeadSelectedListener = new ChatHeadGroupDisplayView.OnChatHeadSelectedListener() {
        @Override
        public void onChatHeadSelected(Chat selectedChat) {
            Log.d(TAG, "onChatHeadSelected() # " + selectedChat);
            mChatView.setChat(selectedChat);
            mChatView.invalidate();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = new SenderPreferences(this);

        mMessagesServiceManager = new MessagesServiceManager(this);
        mMessagesServiceManager.startAndBind();

        PeppermintEventBus.registerMessages(this);

        this.mDisplay = new Display(this);

        mChatHeadView = new ChatHeadGroupDisplayView(this, mDisplay);
        mChatHeadView.addOnStateChangedListener(mOnStateChangeListener);
        mChatHeadView.setOnChatSelectedListener(mOnChatHeadSelectedListener);

        mChatView = new ChatDisplayView(this, mDisplay);
        mChatView.addKeyEventInterceptor(mOnKeyListener);
        // -2px to make sure there's no visible space
        mChatView.setTopMargin(mChatHeadView.getExpandedHeight() - 2);

        registerReceiver(mHomeAppsReceiver, mHomeAppsIntentFilter);

        refreshChatHeadController();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mChatView.hide();
        mChatHeadView.hide();

        unregisterReceiver(mHomeAppsReceiver);

        mChatHeadView.deinit();
        mChatView.deinit();
        mDisplay.deinit();

        mMessagesServiceManager.unbind();

        PeppermintEventBus.unregisterMessages(this);
    }

    private boolean refreshChatHeadController() {
        if(needsToStop()) {
            return false;
        }

        mDisplay.init();
        if(!mChatView.isInitialized()) {
            mChatView.init();
        }
        if(!mChatHeadView.isInitialized()) {
            mChatHeadView.init();
        }

        if(isVisible() && mVisibleActivities.size() <= 0) {
            List<Chat> chatList = new ArrayList<>();
            SQLiteDatabase db = DatabaseHelper.getInstance(this).getReadableDatabase();
            Cursor chatCursor = ChatManager.getAll(db, true);
            int i = 0;
            while(chatCursor.moveToNext() && i < ChatHeadGroupDisplayView.MAX_CHAT_HEADS) {
                Chat chat = ChatManager.getChatFromCursor(db, chatCursor);
                chat.setAmountUnopened(MessageManager.getUnopenedCountByChat(db, chat.getId()));
                chatList.add(0, chat);
                i++;
            }
            chatCursor.close();

            // reverse the order (push the most recent last)
            mChatHeadView.setChats(chatList);

            // pre-select the main chat
            mChatHeadView.setSelected(mChatHeadView.getMainChatHeadDisplayView());
            mChatHeadView.show();
            mChatView.start();
        } else {
            mChatView.stop();
            mChatView.hide();
            mChatHeadView.hide();
        }

        return true;
    }

    public void onEventMainThread(ReceiverEvent event) {
        if(event.getType() == ReceiverEvent.EVENT_RECEIVED) {
            mBinder.show();
        }
    }

    public void onEventMainThread(MessageEvent event) {
        if(event.getType() == MessageEvent.EVENT_MARK_PLAYED) {
            refreshChatHeadController();
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

    private boolean isVisible() {
        return mPreferences.getSharedPreferences().getBoolean(PREF_VISIBLE, false);
    }

    private void setVisible(boolean wasVisible) {
        final SharedPreferences.Editor editor = mPreferences.getSharedPreferences().edit();
        editor.putBoolean(PREF_VISIBLE, wasVisible);
        editor.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // update resource info
        final Resources res = getResources();
        final Configuration conf = res.getConfiguration();
        if (newConfig != null && newConfig.locale != null) {
            conf.locale = newConfig.locale;
        }
        res.updateConfiguration(conf, null);

        mChatHeadView.onLocaleChanged();
        mChatView.onLocaleChanged();

        refreshChatHeadController();
    }
}
