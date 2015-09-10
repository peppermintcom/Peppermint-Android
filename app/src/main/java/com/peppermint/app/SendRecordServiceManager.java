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
 * Created by NunoLuz on 28/08/2015.
 */
public class SendRecordServiceManager {

    public interface Listener {
        void onBoundSendService();
        void onSendStarted(Sender.SenderEvent event);
        void onSendCancelled(Sender.SenderEvent event);
        void onSendError(Sender.SenderEvent event);
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

    public void startAndSend(Recipient to, String filePath) {
        Intent intent = new Intent(mContext, SendRecordService.class);
        intent.putExtra(SendRecordService.INTENT_DATA_FILEPATH, filePath);
        intent.putExtra(SendRecordService.INTENT_DATA_TO, to);
        mContext.startService(intent);
    }

    /**
     * Starts the service.
     * Also binds this manager to the service.
     */
    public void start() {
        Intent intent = new Intent(mContext, SendRecordService.class);
        mContext.startService(intent);
        bind();
    }

    /**
     * Tries to stop the service.
     * Also unbinds this manager from the service.
     * The service will only stop after stopping the current recording.
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

    public UUID send(Recipient to, String filePath) {
        return mService.send(to, filePath);
    }

    public void cancel(UUID uuid) {
        mService.cancel(uuid);
    }

    public Listener getListener() {
        return mListener;
    }

    public void setListener(Listener mListener) {
        this.mListener = mListener;
    }
}
