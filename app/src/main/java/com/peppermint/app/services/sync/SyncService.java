package com.peppermint.app.services.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.peppermint.app.BuildConfig;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * Created by Nuno Luz on 07-06-2016.
 */
public class SyncService extends Service {

    private static final String TAG = SyncService.class.getSimpleName();

    private static final EventBus EVENT_BUS = new EventBus();

    static {
        if(BuildConfig.DEBUG) {
            EVENT_BUS.register(new Object() {
                public void onEventBackgroundThread(SyncEvent event) {
                    Log.d(TAG, event.toString());
                }
            });
        }
    }

    public static void registerEventListener(Object listener) {
        EVENT_BUS.register(listener);
    }

    public static void unregisterEventListener(Object listener) {
        EVENT_BUS.unregister(listener);
    }

    protected static void postSyncEvent(int type, Set<Long> receivedMessageIds, Set<Long> sentMessageIds, Set<Long> affectedChatIds, Throwable error) {
        if(EVENT_BUS.hasSubscriberForEvent(SyncEvent.class)) {
            EVENT_BUS.post(new SyncEvent(type,
                    receivedMessageIds != null ? new HashSet<>(receivedMessageIds) : null,
                    sentMessageIds != null ? new HashSet<>(sentMessageIds) : null,
                    affectedChatIds != null ? new HashSet<>(affectedChatIds) : null,
                    error));
        }
    }

    private static SyncAdapter mSyncAdapter = null;

    private static final Object mSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        synchronized (mSyncAdapterLock) {
            if (mSyncAdapter == null) {
                mSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mSyncAdapter.getSyncAdapterBinder();
    }

}
