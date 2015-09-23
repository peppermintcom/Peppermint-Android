package com.peppermint.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.peppermint.app.data.Recipient;
import com.peppermint.app.senders.Sender;

import java.util.UUID;

/**
 * Created by Nuno Luz on 28/08/2015.
 *
 * Manages the Android Service that sends files through different methods.
 * Allows sending multiple files concurrently.
 * It allows an easier interaction with the Android Service API.
 */
public class SendRecordServiceManager {

    /**
     * Listener for file send events (see {@link com.peppermint.app.senders.Sender.SenderEvent}).
     */
    public interface Listener {
        /**
         * Invoked when a binding to the service is performed.
         */
        void onBoundSendService();

        /**
         * Invoked when a send file request starts.
         * @param event the event
         */
        void onSendStarted(Sender.SenderEvent event);

        /**
         * Invoked when a send file request is cancelled.
         * @param event the event
         */
        void onSendCancelled(Sender.SenderEvent event);

        /**
         * Invoked when a send file request fails.
         * @param event the event
         */
        void onSendError(Sender.SenderEvent event);

        /**
         * Invoked when a send file request finishes.
         * @param event the event
         */
        void onSendFinished(Sender.SenderEvent event);
    }

    private static final String TAG = SendRecordServiceManager.class.getSimpleName();

    private Context mContext;
    private SendRecordService.SendRecordServiceBinder mService;
    private Listener mListener;
    protected boolean mIsBound = false;                                         // if the manager is bound to the service

    /**
     * Event callback triggered by the {@link RecordService} through an {@link de.greenrobot.event.EventBus}.<br />
     * @param event the event (see {@link RecordService.Event})
     */
    public void onEventMainThread(Sender.SenderEvent event) {
        switch (event.getType()) {
            case Sender.SenderEvent.EVENT_STARTED:
                mListener.onSendStarted(event);
                break;
            case Sender.SenderEvent.EVENT_ERROR:
                mListener.onSendError(event);
                break;
            case Sender.SenderEvent.EVENT_CANCELLED:
                mListener.onSendCancelled(event);
                break;
            case Sender.SenderEvent.EVENT_FINISHED:
                mListener.onSendFinished(event);
                break;
        }
    }

    /**
     * Event listener associated with the service bind/unbind.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (SendRecordService.SendRecordServiceBinder) binder;
            mService.register(SendRecordServiceManager.this);

            mListener.onBoundSendService();
            Log.d(TAG, "onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName className) {
            // this is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    public SendRecordServiceManager(Context context) {
        this.mContext = context;
    }

    /**
     * Starts the service and sends an intent to start sending the supplied file to the supplied recipient.
     * @param recipient the recipient of the file
     * @param filePath the location of the file to send
     */
    public void startAndSend(Recipient recipient, String filePath) {
        Intent intent = new Intent(mContext, SendRecordService.class);
        intent.putExtra(SendRecordService.INTENT_DATA_FILEPATH, filePath);
        intent.putExtra(SendRecordService.INTENT_DATA_RECIPIENT, recipient);
        mContext.startService(intent);
    }

    /**
     * Starts the service.
     * <b>Also binds this manager to the service.</b>
     */
    public void start() {
        Intent intent = new Intent(mContext, SendRecordService.class);
        mContext.startService(intent);
        bind();
    }

    /**
     * Tries to stop the service.
     * <b>Also unbinds this manager from the service.</b>
     */
    public void shouldStop() {
        mService.shutdown();
        unbind();
    }

    /**
     * Binds this manager to the service.
     */
    public void bind() {
        mContext.bindService(new Intent(mContext, SendRecordService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * Unbinds this manager from the service.
     */
    public void unbind() {
        if (mIsBound) {
            // if we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                mService.unregister(SendRecordServiceManager.this);
            }
            // detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    /**
     * Sends the supplied file to the supplied recipient.
     * Can only be used if the manager is bound to the service.
     * @param recipient the recipient of the file
     * @param filePath the file location
     * @return the {@link UUID} of the send file request/task
     */
    public UUID send(Recipient recipient, String filePath) {
        return mService.send(recipient, filePath);
    }

    /**
     * Cancels the send file request with the supplied {@link UUID}.
     * @param uuid the {@link UUID} of the send file request/task
     */
    public void cancel(UUID uuid) {
        mService.cancel(uuid);
    }

    public boolean isSending() {
        return mService.isSending();
    }

    public Listener getListener() {
        return mListener;
    }

    public void setListener(Listener mListener) {
        this.mListener = mListener;
    }
}
