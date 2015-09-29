package com.peppermint.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.peppermint.app.data.Recipient;

/**
 * Created by Nuno Luz on 28/08/2015.
 *
 * Manages the Android Service that records audio/video files.
 * It allows an easier interaction with the Android Service API.
 */
public class RecordServiceManager {

    /**
     * Listener for recording events (see {@link com.peppermint.app.RecordService.Event}).
     */
    public interface Listener {
        /**
         * Invoked when a binding to the service is performed.
         */
        void onBoundRecording(Recipient currentRecipient, String currentFilePath, long currentFullDuration, float currentLoudness);

        /**
         * Invoked when a new recording starts.
         * @param event the event
         */
        void onStartRecording(RecordService.Event event);

        /**
         * Invoked when the current recording stops.
         * @param event the event
         */
        void onStopRecording(RecordService.Event event);

        /**
         * Invoked when the current recording is resumed (only after being paused).
         * @param event the event
         */
        void onResumeRecording(RecordService.Event event);

        /**
         * Invoked when the current recording is paused.
         * @param event the event
         */
        void onPauseRecording(RecordService.Event event);

        /**
         * Invoked in 100 ms intervals while the recording is in progress.
         * It provides loudness (wave amplitude) information.
         * @param event the event
         */
        void onLoudnessRecording(RecordService.Event event);

        /**
         * Invoked when an error occurs. The error can be obtained through
         * {@link RecordService.Event#getError()}
         * @param event the event
         */
        void onErrorRecording(RecordService.Event event);
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
        if(mListener == null) {
            return;
        }

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
            case RecordService.EVENT_ERROR:
                mListener.onErrorRecording(event);
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

            if(mListener != null) {
                mListener.onBoundRecording(mService.getCurrentRecipient(), mService.getCurrentFilePath(), mService.getCurrentFullDuration(), mService.getCurrentLoudness());
            }
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
        mService.shutdown();
        if(mIsBound) {
            unbind();
        }
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

    /**
     * Start a recording. You can only start a recording if no other recording is currently
     * active (even if it is paused).
     * @param filePrefix the filename prefix of the record
     * @param recipient the recipient of the record
     */
    public void startRecording(String filePrefix, Recipient recipient) {
        mService.start(filePrefix, recipient);
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

    public Listener getListener() {
        return mListener;
    }

    public void setListener(Listener mListener) {
        this.mListener = mListener;
    }

    public boolean isBound() {
        return mIsBound;
    }
}
