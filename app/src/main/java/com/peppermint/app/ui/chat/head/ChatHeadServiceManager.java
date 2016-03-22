package com.peppermint.app.ui.chat.head;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.peppermint.app.data.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 22/03/2016.
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

    /**
     * Starts the service and plays the supplied message.
     * @param message the message
     * @param startProgress the starting point in percentage of the total duration of the message
     */
    public void startAndEnable(Message message, int startProgress) {
        Intent intent = new Intent(mContext, ChatHeadService.class);
        intent.setAction(ChatHeadService.ACTION_ENABLE);
        mContext.startService(intent);
    }

    public void start() {
        Intent intent = new Intent(mContext, ChatHeadService.class);
        mContext.startService(intent);
    }

    /**
     * Starts the service.
     * <b>Also binds this manager to the service.</b>
     */
    public void startAndBind() {
        start();
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

    public boolean isBound() {
        return mIsBound;
    }

    public void enable() { mService.enable(); }
    public void disable() { mService.disable(); }
    public void show() { mService.show(); }
    public void hide() { mService.hide(); }

    public void addServiceBinderListener(ChatHeadServiceBinderListener listener) {
        mBinderListeners.add(listener);
    }

    public boolean removeServiceBinderListener(ChatHeadServiceBinderListener listener) {
        return mBinderListeners.remove(listener);
    }
}
