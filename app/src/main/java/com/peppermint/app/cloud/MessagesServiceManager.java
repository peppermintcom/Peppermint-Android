package com.peppermint.app.cloud;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.peppermint.app.data.Chat;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recording;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 28/08/2015.
 *
 * Manages the Android Service that sends messages through different methods.
 * It allows an easier interaction with the Android Service API.
 */
public class MessagesServiceManager {

    public interface ServiceListener {
        /**
         * Invoked when a binding to the service is performed.
         */
        void onBoundSendService();
    }

    private Context mContext;
    private MessagesService.SendRecordServiceBinder mService;
    private List<ServiceListener> mServiceListenerList = new ArrayList<>();
    protected boolean mIsBound = false;                                         // if the manager is bound to the service
    protected boolean mIsBinding = false;

    /**
     * Event listener associated with the service bind/unbind.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (MessagesService.SendRecordServiceBinder) binder;

            mIsBound = true;
            mIsBinding = false;

            for(ServiceListener listener : mServiceListenerList) {
                listener.onBoundSendService();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // this is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            mIsBound = false;
            mIsBinding = false;
        }
    };

    public MessagesServiceManager(Context context) {
        this.mContext = context;
    }

    /**
     * Starts the service and sends an intent to start sending the supplied file to the supplied chat.
     * @param chat the chat to send the file to
     * @param recording the recording with the file to send
     */
    public void startAndSend(Chat chat, Recording recording) {
        Intent intent = new Intent(mContext, MessagesService.class);
        intent.putExtra(MessagesService.PARAM_MESSAGE_SEND_RECORDING, recording);
        intent.putExtra(MessagesService.PARAM_MESSAGE_SEND_CHAT, chat);
        mContext.startService(intent);
    }

    public void startAndSync() {
        Intent intent = new Intent(mContext, MessagesService.class);
        intent.putExtra(MessagesService.PARAM_DO_SYNC, true);
        mContext.startService(intent);
    }

    public void start() {
        Intent intent = new Intent(mContext, MessagesService.class);
        mContext.startService(intent);
    }

    /**
     * Starts the service.
     * <b>Also binds this manager to the service.</b>
     */
    public void startAndBind() {
        Intent intent = new Intent(mContext, MessagesService.class);
        mContext.startService(intent);
        bind();
    }

    /**
     * Tries to stop the service.
     * <b>Also unbinds this manager from the service.</b>
     */
    public void shouldStop() {
        unbind();
        mService.shutdown();
    }

    /**
     * Binds this manager to the service.
     */
    public void bind() {
        mIsBinding = true;
        mContext.bindService(new Intent(mContext, MessagesService.class), mConnection, Context.BIND_AUTO_CREATE);
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

    /**
     * Sends the supplied recording to the supplied chat.
     * Can only be used if the manager is bound to the service.
     * @param chat the chat to send the recording to
     * @param recording the recording and file location
     * @return the {@link Message}
     */
    public Message send(Chat chat, Recording recording) {
        return mService.send(chat, recording);
    }

    public void removeAllNotifications() {
        mService.removeAllNotifications();
    }

    public void markAsPlayed(Message message) {
        mService.markAsPlayed(message);
    }

    public boolean retry(Message message) {
        return mService.retry(message);
    }

    /**
     * Cancels the send/upload task for the supplied message.
     * @param message the message
     */
    public boolean cancel(Message message) {
        return mService.cancel(message);
    }

    public boolean cancel() {
        if(mService != null) {
            return mService.cancel();
        }

        Intent intent = new Intent(mContext, MessagesService.class);
        intent.setAction(MessagesService.ACTION_CANCEL);
        mContext.startService(intent);
        return true;
    }

    public void doPendingLogouts() {
        Intent intent = new Intent(mContext, MessagesService.class);
        intent.setAction(MessagesService.ACTION_DO_PENDING_LOGOUTS);
        mContext.startService(intent);
    }

    public boolean isSending(Message message) {
        return mService.isSending(message);
    }

    public boolean isSending() {
        return mService.isSending();
    }

    public boolean isSendingAndCancellable(Message message) { return mService.isSendingAndCancellable(message); }

    public void addServiceListener(ServiceListener listener) {
        mServiceListenerList.add(listener);
    }

    public boolean removeServiceListener(ServiceListener listener) {
        return mServiceListenerList.remove(listener);
    }

}
