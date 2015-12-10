package com.peppermint.app.sending;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.rest.HttpAsyncTask;

import java.util.Map;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     The abstract AsyncTask implementation executed by a {@link Sender}.<br />
 *     Each {@link Sender} must have their own {@link SenderTask} concrete implementations.
 * </p>
 * <p>
 *     As with {@link Sender}s, {@link SenderTask}s are configured through Parameters and Preferences.
 *     Preferences use the native Android Shared Preferences mechanism, so that data is saved
 *     across different executions of the app. Parameters are part of a key-value map passed to
 *     the sending task instance (each implementation may have its own parameters).
 * </p>
 */
public abstract class SenderTask extends HttpAsyncTask implements Cloneable {

    public static final float PROGRESS_INDETERMINATE = -1f;
    public static final float PROGRESS_MAX = 100f;
    public static final float PROGRESS_MIN = 0f;

    private float mProgress = PROGRESS_INDETERMINATE;

    private Sender mSender;
    private SenderPreferences mPreferences;
    private SenderListener mListener;

    private SendingRequest mSendingRequest;
    private boolean mRecovering = false;

    public SenderTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        super(sender.getContext());
        this.mSender = sender;
        this.mListener = listener;
        this.mSendingRequest = sendingRequest;
        if(sendingRequest != null) {
            setId(sendingRequest.getId());
        }
    }

    public SenderTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender.getContext(), parameters);
        this.mSender = sender;
        this.mListener = listener;
        this.mPreferences = preferences;
        this.mSendingRequest = sendingRequest;
        if(sendingRequest != null) {
            setId(sendingRequest.getId());
        }
    }

    public SenderTask(SenderTask sendingTask) {
        super(sendingTask);
        this.mSender = sendingTask.mSender;
        this.mListener = sendingTask.mListener;
        this.mPreferences = sendingTask.mPreferences;
        this.mSendingRequest = sendingTask.mSendingRequest;
    }

    /**
     * Actually sends the audio/video file to a {@link com.peppermint.app.data.Recipient}
     * @throws Throwable
     */
    protected abstract void send() throws Throwable;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(mListener != null && !mRecovering) {
            mListener.onSendingTaskStarted(this);
        }
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        mProgress = values[0];
        if(mListener != null) {
            mListener.onSendingTaskProgress(this, mProgress);
        }
    }

    @Override
    protected void onCancelled(Void aVoid) {
        if(mListener != null) {
            mListener.onSendingTaskCancelled(this);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(getError() == null) {
            if(mListener != null) {
                mListener.onSendingTaskFinished(this);
            }
        } else {
            if (mListener != null) {
                mListener.onSendingTaskError(this, getError());
            }
        }
    }

    public Sender getSender() {
        return mSender;
    }

    public SenderListener getSenderListener() {
        return mListener;
    }

    public void setSenderListener(SenderListener mListener) {
        this.mListener = mListener;
    }

    public float getProgressMax() {
        return PROGRESS_MAX;
    }

    public float getProgressMin() {
        return PROGRESS_MIN;
    }

    public float getProgress() {
        return mProgress;
    }

    public SenderPreferences getSenderPreferences() {
        return mPreferences;
    }

    public SendingRequest getSendingRequest() {
        return mSendingRequest;
    }

    public void setSendingRequest(SendingRequest mSendingRequest) {
        this.mSendingRequest = mSendingRequest;
    }

    public boolean isRecovering() {
        return mRecovering;
    }

    public void setRecovering(boolean mRecovering) {
        this.mRecovering = mRecovering;
    }
}
