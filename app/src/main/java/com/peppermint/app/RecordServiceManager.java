package com.peppermint.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by NunoLuz on 28/08/2015.
 * Manages the Android Service that records audio/video files.
 * It allows an easier interaction with the Android Service API.
 */
public class RecordServiceManager {

    public interface Listener {
        void onBoundRecording();
        void onStartRecording(RecordService.Event event);
        void onStopRecording(RecordService.Event event);
        void onResumeRecording(RecordService.Event event);
        void onPauseRecording(RecordService.Event event);
        void onLoudnessRecording(RecordService.Event event);
    }

    private static final String TAG = RecordServiceManager.class.getSimpleName();

    private Context mContext;
    private RecordService.RecordServiceBinder mService;
    private Listener mListener;
    protected boolean mIsBound = false;                                         // if the manager is bound to the service

    /**
     * Event callback triggered by the {@link RecordService} through an {@link de.greenrobot.event.EventBus}.<br />
     * @param event the event (see {@link com.peppermint.app.RecordService.Event})
     */
    public void onEventMainThread(RecordService.Event event) {
        switch (event.getType()) {
            case RecordService.EVENT_START:
                mListener.onStartRecording(event);
                break;
            case RecordService.EVENT_STOP:
                mListener.onStopRecording(event);
                break;
            case RecordService.EVENT_RESUME:
                mListener.onResumeRecording(event);
                break;
            case RecordService.EVENT_PAUSE:
                mListener.onPauseRecording(event);
                break;
            default:
                mListener.onLoudnessRecording(event);
        }
    }

    /**
     * Event listener associated with the service bind/unbind.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (RecordService.RecordServiceBinder) binder;
            mService.register(RecordServiceManager.this);

            mListener.onBoundRecording();

            Log.d(TAG, "onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName className) {
            // this is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    public RecordServiceManager(Context context) {
        this.mContext = context;
    }

    public ArrayList<String> getFullFilePaths() {
        return mService.getFullFilePaths();
    }

    /**
     * Starts the service.
     * Also binds this manager to the service.
     */
    public void start() {
        Intent intent = new Intent(mContext, RecordService.class);
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
        mContext.bindService(new Intent(mContext, RecordService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * Unbinds this manager from the service.
     */
    public void unbind() {
        if (mIsBound) {
            // if we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                mService.unregister(RecordServiceManager.this);
            }
            // detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    public void startRecording(String filename) {
        mService.start(filename);
    }

    public void stopRecording() {
        mService.stop();
    }

    public void pauseRecording() {
        mService.pause();
    }

    public void resumeRecording() {
        mService.resume();
    }

    public boolean isRecording() {
        return mService.isRecording();
    }

    public boolean isPaused() {
        return mService.isPaused();
    }

    public void discard() {
        mService.discard();
    }

    public Listener getListener() {
        return mListener;
    }

    public void setListener(Listener mListener) {
        this.mListener = mListener;
    }
}
