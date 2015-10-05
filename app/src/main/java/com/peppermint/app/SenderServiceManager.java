package com.peppermint.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.Recording;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.SendingEvent;

import java.util.UUID;

/**
 * Created by Nuno Luz on 28/08/2015.
 *
 * Manages the Android Service that sends files through different methods.
 * Allows sending multiple files concurrently.
 * It allows an easier interaction with the Android Service API.
 */
public class SenderServiceManager {

    /**
     * Listener for file send events (see {@link SendingEvent}).
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
        void onSendStarted(SendingEvent event);

        /**
         * Invoked when a send file request is cancelled.
         * @param event the event
         */
        void onSendCancelled(SendingEvent event);

        /**
         * Invoked when a send file request fails.
         * @param event the event
         */
        void onSendError(SendingEvent event);

        /**
         * Invoked when a send file request finishes.
         * @param event the event
         */
        void onSendFinished(SendingEvent event);

        /**
         * Invoked when a send file request progresses.
         * @param event the event
         */
        void onSendProgress(SendingEvent event);

        /**
         * Invoked when a send file request has been queued due to a recoverable error.
         * @param event the event
         */
        void onSendQueued(SendingEvent event);
    }

    private static final String TAG = SenderServiceManager.class.getSimpleName();

    private Context mContext;
    private SenderService.SendRecordServiceBinder mService;
    private Listener mListener;
    protected boolean mIsBound = false;                                         // if the manager is bound to the service

    /**
     * Event callback triggered by the {@link RecordService} through an {@link de.greenrobot.event.EventBus}.<br />
     * @param event the event (see {@link RecordService.Event})
     */
    public void onEventMainThread(SendingEvent event) {
        if(mListener == null) {
            return;
        }

        switch (event.getType()) {
            case SendingEvent.EVENT_STARTED:
                mListener.onSendStarted(event);
                break;
            case SendingEvent.EVENT_ERROR:
                mListener.onSendError(event);
                break;
            case SendingEvent.EVENT_CANCELLED:
                mListener.onSendCancelled(event);
                break;
            case SendingEvent.EVENT_FINISHED:
                mListener.onSendFinished(event);
                break;
            case SendingEvent.EVENT_PROGRESS:
                mListener.onSendProgress(event);
                break;
            case SendingEvent.EVENT_QUEUED:
                mListener.onSendQueued(event);
                break;
        }
    }

    /**
     * Event listener associated with the service bind/unbind.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (SenderService.SendRecordServiceBinder) binder;
            mService.register(SenderServiceManager.this);

            if(mListener != null) {
                mListener.onBoundSendService();
            }
            Log.d(TAG, "onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName className) {
            // this is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    public SenderServiceManager(Context context) {
        this.mContext = context;
    }

    /**
     * Starts the service and sends an intent to start sending the supplied file to the supplied recipient.
     * @param recipient the recipient of the file
     * @param recording the recording with the file to send
     */
    public void startAndSend(Recipient recipient, Recording recording) {
        Intent intent = new Intent(mContext, SenderService.class);
        intent.putExtra(SenderService.INTENT_DATA_RECORDING, recording);
        intent.putExtra(SenderService.INTENT_DATA_RECIPIENT, recipient);
        mContext.startService(intent);
    }

    /**
     * Starts the service.
     * <b>Also binds this manager to the service.</b>
     */
    public void start() {
        Intent intent = new Intent(mContext, SenderService.class);
        mContext.startService(intent);
        bind();
    }

    /**
     * Tries to stop the service.
     * <b>Also unbinds this manager from the service.</b>
     */
    public void shouldStop() {
        mService.shutdown();
        if(mIsBound) {
            unbind();
        }
    }

    /**
     * Binds this manager to the service.
     */
    public void bind() {
        mContext.bindService(new Intent(mContext, SenderService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * Unbinds this manager from the service.
     */
    public void unbind() {
        if (mIsBound) {
            // if we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                mService.unregister(SenderServiceManager.this);
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
     * @param recording the recording and file location
     * @return the {@link SendingRequest} of the send file request/task
     */
    public SendingRequest send(Recipient recipient, Recording recording) {
        return mService.send(recipient, recording);
    }

    /**
     * Cancels the send file request with the supplied {@link UUID}.
     * @param uuid the {@link UUID} of the send file request/task
     */
    public void cancel(UUID uuid) {
        mService.cancel(uuid);
    }

    public void cancel() {
        mService.cancel();
    }

    public boolean isSending(UUID uuid) {
        return mService.isSending(uuid);
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
