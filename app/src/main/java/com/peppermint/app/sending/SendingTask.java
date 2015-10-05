package com.peppermint.app.sending;

import android.os.AsyncTask;
import android.util.Log;

import com.peppermint.app.data.SendingRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * The AsyncTask that performs the sending for a particular Sender.
 */
public abstract class SendingTask extends AsyncTask<Void, Float, Void> implements Cloneable {

    private static final String TAG = SendingTask.class.getSimpleName();

    protected static final float PROGRESS_INDETERMINATE = -1f;
    protected static final float PROGRESS_MAX = 100f;
    protected static final float PROGRESS_MIN = 0f;

    private Sender mSender;
    private Throwable mError;
    private SendingRequest mSendingRequest;
    private SenderListener mListener;
    private Map<String, Object> mParameters;
    private SenderPreferences mPreferences;
    private float mProgress = PROGRESS_INDETERMINATE;

    public SendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        this.mSender = sender;
        this.mSendingRequest = sendingRequest;
        this.mListener = listener;
        this.mParameters = new HashMap<>();
    }

    public SendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        this.mSender = sender;
        this.mSendingRequest = sendingRequest;
        this.mListener = listener;
        this.mPreferences = preferences;
        if(parameters != null) {
            this.mParameters = new HashMap<>(parameters);
        }
    }

    public SendingTask(SendingTask sendingTask) {
        this.mSender = sendingTask.mSender;
        this.mSendingRequest = sendingTask.mSendingRequest;
        this.mListener = sendingTask.mListener;
        this.mPreferences = sendingTask.mPreferences;
        if(sendingTask.mParameters != null) {
            this.mParameters = new HashMap<>(sendingTask.mParameters);
        } else {
            this.mParameters = new HashMap<>();
        }
    }

    protected abstract void send() throws Throwable;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(mListener != null) {
            mListener.onSendingTaskStarted(this, mSendingRequest);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            send();
        } catch (Throwable e) {
            mError = e;
            Log.w(TAG, e);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        mProgress = values[0];
        if(mListener != null) {
            mListener.onSendingTaskProgress(this, mSendingRequest, mProgress);
        }
    }

    @Override
    protected void onCancelled(Void aVoid) {
        if(mListener != null) {
            mListener.onSendingTaskCancelled(this, mSendingRequest);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(mError == null) {
            if(mListener != null) {
                mListener.onSendingTaskFinished(this, mSendingRequest);
            }
        } else {
            if (mListener != null) {
                mListener.onSendingTaskError(this, mSendingRequest, mError);
            }
        }
    }

    public Throwable getError() {
        return mError;
    }

    public Sender getSender() {
        return mSender;
    }

    public SendingRequest getSendingRequest() {
        return mSendingRequest;
    }

    public void setSendingRequest(SendingRequest mSendingRequest) {
        this.mSendingRequest = mSendingRequest;
    }

    public SenderListener getSenderListener() {
        return mListener;
    }

    public void setSenderListener(SenderListener mListener) {
        this.mListener = mListener;
    }

    public Map<String, Object> getParameters() {
        return mParameters;
    }

    public void setParameters(Map<String, Object> mParameters) {
        this.mParameters = mParameters;
    }

    public Object getParameter(String key) {
        if(!mParameters.containsKey(key)) {
            return null;
        }
        return mParameters.get(key);
    }

    public void setParameter(String key, Object value) {
        mParameters.put(key, value);
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
}
