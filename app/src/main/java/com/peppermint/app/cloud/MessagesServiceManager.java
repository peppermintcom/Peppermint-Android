package com.peppermint.app.cloud;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.peppermint.app.RecordService;
import com.peppermint.app.cloud.senders.SenderEvent;
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

    /**
     * Listener for send message events (see {@link SenderEvent}).
     */
    public interface SenderListener {
        /**
         * Invoked when a send message starts.
         * @param event the event
         */
        void onSendStarted(SenderEvent event);

        /**
         * Invoked when a send message is cancelled.
         * @param event the event
         */
        void onSendCancelled(SenderEvent event);

        /**
         * Invoked when a send message fails.
         * @param event the event
         */
        void onSendError(SenderEvent event);

        /**
         * Invoked when a send message finishes.
         * @param event the event
         */
        void onSendFinished(SenderEvent event);

        /**
         * Invoked when a send message progresses.
         * @param event the event
         */
        void onSendProgress(SenderEvent event);

        /**
         * Invoked when a send message has been queued due to a recoverable error.
         * @param event the event
         */
        void onSendQueued(SenderEvent event);
    }

    public interface ReceiverListener {
        void onReceivedMessage(ReceiverEvent event);
    }

    public interface SyncListener {
        void onSyncStarted(SyncEvent event);
        void onSyncFinished(SyncEvent event);
    }

    private Context mContext;
    private MessagesService.SendRecordServiceBinder mService;
    private List<SenderListener> mSenderListenerList = new ArrayList<>();
    private List<ReceiverListener> mReceiverListenerList = new ArrayList<>();
    private List<ServiceListener> mServiceListenerList = new ArrayList<>();
    private List<SyncListener> mSyncListenerList = new ArrayList<>();
    protected boolean mIsBound = false;                                         // if the manager is bound to the service
    protected boolean mIsBinding = false;

    public void onEventMainThread(SyncEvent event) {
        for(SyncListener listener : mSyncListenerList) {
            switch (event.getType()) {
                case SyncEvent.EVENT_STARTED:
                    listener.onSyncStarted(event);
                    break;
                default:
                    listener.onSyncFinished(event);
                    break;
            }
        }
    }

    public void onEventMainThread(ReceiverEvent event) {
        switch(event.getType()) {
            case ReceiverEvent.EVENT_RECEIVED:
                for(ReceiverListener listener : mReceiverListenerList) {
                    listener.onReceivedMessage(event);
                }
                break;
        }
    }

    /**
     * Event callback triggered by the {@link RecordService} through an {@link de.greenrobot.event.EventBus}.<br />
     * @param event the event (see {@link RecordService.Event})
     */
    public void onEventMainThread(SenderEvent event) {
        for(SenderListener listener : mSenderListenerList) {
            switch (event.getType()) {
                case SenderEvent.EVENT_STARTED:
                    listener.onSendStarted(event);
                    break;
                case SenderEvent.EVENT_ERROR:
                    listener.onSendError(event);
                    break;
                case SenderEvent.EVENT_CANCELLED:
                    listener.onSendCancelled(event);
                    break;
                case SenderEvent.EVENT_FINISHED:
                    listener.onSendFinished(event);
                    break;
                case SenderEvent.EVENT_PROGRESS:
                    listener.onSendProgress(event);
                    break;
                case SenderEvent.EVENT_QUEUED:
                    listener.onSendQueued(event);
                    break;
            }
        }
    }

    /**
     * Event listener associated with the service bind/unbind.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (MessagesService.SendRecordServiceBinder) binder;
            mService.register(MessagesServiceManager.this);

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
            // if we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                mService.unregister(MessagesServiceManager.this);
            }
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
        return mService.cancel();
    }

    public boolean isSending(Message message) {
        return mService.isSending(message);
    }

    public boolean isSending() {
        return mService.isSending();
    }

    public void addServiceListener(ServiceListener listener) {
        mServiceListenerList.add(listener);
    }

    public boolean removeServiceListener(ServiceListener listener) {
        return mServiceListenerList.remove(listener);
    }

    public void addReceiverListener(ReceiverListener listener) {
        mReceiverListenerList.add(listener);
    }

    public boolean removeReceiverListener(ReceiverListener listener) {
        return mReceiverListenerList.remove(listener);
    }

    public void addSyncListener(SyncListener listener) {
        mSyncListenerList.add(listener);
    }

    public boolean removeSyncListener(SyncListener listener) {
        return mSyncListenerList.remove(listener);
    }

    public void addSenderListener(SenderListener listener) {
        mSenderListenerList.add(listener);
    }

    public boolean removeSenderListener(SenderListener listener) {
        return mSenderListenerList.remove(listener);
    }
}
