package com.peppermint.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.peppermint.app.data.Chat;
import com.peppermint.app.data.Recording;

/**
 * Created by Nuno Luz on 28/08/2015.
 *
 * Manages the Android Service that records audio/video files.
 * It allows an easier interaction with the Android Service API.
 */
public class RecordServiceManager {

    /**
     * RecordServiceListener for binding the service.
     */
    public interface RecordServiceListener {
        /**
         * Invoked when a binding to the service is performed.
         */
        void onBoundRecording(Recording recording, Chat currentChat, float currentLoudness);
    }

    private static final String TAG = RecordServiceManager.class.getSimpleName();

    private Context mContext;
    private RecordService.RecordServiceBinder mService;
    private RecordServiceListener mListener;
    protected boolean mIsBound = false;                                         // if the manager is bound to the service
    protected boolean mIsBinding = false;

    /**
     * Event listener associated with the service bind/unbind.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (RecordService.RecordServiceBinder) binder;

            mIsBound = true;
            mIsBinding = false;

            if(mListener != null) {
                mListener.onBoundRecording(mService.getCurrentRecording(), mService.getCurrentChat(), mService.getCurrentLoudness());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // this is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            mIsBound = false;
            mIsBinding = false;
        }
    };

    public RecordServiceManager(Context context) {
        this.mContext = context;
    }

    /**
     * Starts the service.
     * <b>Also binds this manager to the service.</b>
     */
    public void start(boolean bind) {
        Intent intent = new Intent(mContext, RecordService.class);
        mContext.startService(intent);
        if(bind) {
            bind();
        }
    }

    /**
     * Tries to stop the service.
     * <b>Also unbinds this manager from the service.</b>
     * The service will only stop after stopping the current recording.
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
        mContext.bindService(new Intent(mContext, RecordService.class), mConnection, Context.BIND_AUTO_CREATE);
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

    /**
     * Start a recording. You can only start a recording if no other recording is currently
     * active (even if it is paused).
     * @param filePrefix the filename prefix of the record
     * @param chat the chat of the record
     */
    public void startRecording(String filePrefix, Chat chat, long maxDurationMillis) {
        mService.start(filePrefix, chat, maxDurationMillis);
    }

    /**
     * Stop and finish the current recording.
     */
    public void stopRecording(boolean discard) {
        mService.stop(discard);
    }

    /**
     * Pause the current recording.
     */
    public void pauseRecording() {
        mService.pause();
    }

    /**
     * Resume the current recording.
     */
    public void resumeRecording() {
        mService.resume();
    }

    /**
     * Checks if the service is recording. Notice that the recording process can be paused.
     * It is still considered as an ongoing recording.
     * @return true if recording; false if not
     */
    public boolean isRecording() {
        return mService.isRecording();
    }

    /**
     * Checks if the service is recording and paused.
     * @return true if recording and paused; false if not
     */
    public boolean isPaused() {
        return mService.isPaused();
    }

    public RecordServiceListener getListener() {
        return mListener;
    }

    public void setListener(RecordServiceListener mRecordServiceListener) {
        this.mListener = mRecordServiceListener;
    }

    public boolean isBound() {
        return mIsBound;
    }

    public Chat getCurrentChat() {
        return mService.getCurrentChat();
    }

    public Recording getCurrentRecording() {
        return mService.getCurrentRecording();
    }
}
