package com.peppermint.app.ui.chat.head;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.peppermint.app.tracking.TrackerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 22/03/2016.
 *
 * Manager that helps external components to interact with the {@link ChatHeadService}.
 */
public class ChatHeadServiceManager {

    public interface ChatHeadServiceBinderListener {
        /**
         * Invoked when a binding to the service is performed.
         */
        void onBoundChatHeadService();
    }

    private Context mContext;
    private ChatHeadService.ChatHeadServiceBinder mService;
    private List<ChatHeadServiceBinderListener> mBinderListeners = new ArrayList<>();
    protected boolean mIsBound = false;
    protected boolean mIsBinding = false;

    /**
     * Event listener associated with the service bind/unbind.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (ChatHeadService.ChatHeadServiceBinder) binder;

            mIsBound = true;
            mIsBinding = false;

            for(ChatHeadServiceBinderListener listener : mBinderListeners) {
                listener.onBoundChatHeadService();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // this is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            mIsBound = false;
            mIsBinding = false;
        }
    };

    public ChatHeadServiceManager(Context context) {
        this.mContext = context;
    }

    public static void startAndEnable(Context context) {
        Intent intent = new Intent(context, ChatHeadService.class);
        intent.setAction(ChatHeadService.ACTION_ENABLE);
        context.startService(intent);
    }

    public static void startAndDisable(Context context) {
        Intent intent = new Intent(context, ChatHeadService.class);
        intent.setAction(ChatHeadService.ACTION_DISABLE);
        context.startService(intent);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, ChatHeadService.class);
        context.startService(intent);
    }

    /**
     * Starts the service.
     * <b>Also binds this manager to the service.</b>
     */
    public void startAndBind() {
        start(mContext);
        bind();
    }

    /**
     * Binds this manager to the service.
     */
    public void bind() {
        mIsBinding = true;
        mContext.bindService(new Intent(mContext, ChatHeadService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Unbinds this manager from the service.
     */
    public void unbind() {
        if (mIsBound || mIsBinding) {
             // detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
            mIsBinding = false;
        }
    }

    public void enable() {
        if(mService != null) {
            mService.enable();
        } else {
            TrackerManager.getInstance(mContext).logException(new NullPointerException("Service not bound! mService is null!"));
        }
    }
    public void disable() {
        if(mService != null) {
            mService.disable();
        } else {
            TrackerManager.getInstance(mContext).logException(new NullPointerException("Service not bound! mService is null!"));
        }
    }
    public void show() {
        if(mService != null) {
            mService.show();
        } else {
            TrackerManager.getInstance(mContext).logException(new NullPointerException("Service not bound! mService is null!"));
        }
    }
    public void hide() {
        if(mService != null) {
            mService.hide();
        } else {
            TrackerManager.getInstance(mContext).logException(new NullPointerException("Service not bound! mService is null!"));
        }
    }

    public void addVisibleActivity(String activityFullClassName) {
        if(mService != null) {
            mService.addVisibleActivity(activityFullClassName);
        } else {
            TrackerManager.getInstance(mContext).logException(new NullPointerException("Service not bound! mService is null!"));
        }
    }
    public boolean removeVisibleActivity(String activityFullClassName) {
        if(mService == null) {
            TrackerManager.getInstance(mContext).logException(new NullPointerException("Service not bound! mService is null!"));
            return false;
        }
        return mService.removeVisibleActivity(activityFullClassName);
    }

    public void addServiceBinderListener(ChatHeadServiceBinderListener listener) {
        mBinderListeners.add(listener);
    }

    public boolean removeServiceBinderListener(ChatHeadServiceBinderListener listener) {
        return mBinderListeners.remove(listener);
    }
}
