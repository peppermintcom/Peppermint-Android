package com.peppermint.app.cloud.senders;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.peppermint.app.data.Message;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     The abstract support AsyncTask implementation executed by a {@link Sender} or {@link SenderErrorHandler}.<br />
 *     Each {@link Sender} and {@link SenderErrorHandler} must have their own {@link SenderSupportTask} concrete implementations.
 * </p>
 * <p>
 *     Check {@link SenderTask} for further information.
 * </p>
 */
public abstract class SenderSupportTask extends SenderTask implements Cloneable {

    private static final String TAG = SenderSupportTask.class.getSimpleName();

    public static final String INTENT_ACTION_FINISHED = TAG + "_Finished";
    public static final String INTENT_TASK_ID = TAG + "_TaskId";
    public static final String INTENT_THROWABLE = TAG + "_Throwable";
    public static final String INTENT_TASK_TYPE = TAG + "_TaskType";
    public static final String INTENT_SUCCESS = TAG + "_Success";

    private final Intent mFinishedIntent = new Intent(INTENT_ACTION_FINISHED);
    private SenderSupportListener mSenderSupportListener;
    private SenderUploadTask mRelatedUploadTask;

    public SenderSupportTask(SenderSupportTask supportTask) {
        super(supportTask);
        this.mSenderSupportListener = supportTask.mSenderSupportListener;
    }

    public SenderSupportTask(Sender sender, Message message, SenderSupportListener senderSupportListener) {
        super(sender, message);
        this.mSenderSupportListener = senderSupportListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(mSenderSupportListener != null) {
            mSenderSupportListener.onSendingSupportStarted(this);
        }
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        mProgress = values[0];
        if(mSenderSupportListener != null) {
            mSenderSupportListener.onSendingSupportProgress(this, mProgress);
        }
    }

    @Override
    protected void onCancelled(Void aVoid) {
        if(mSenderSupportListener != null) {
            mSenderSupportListener.onSendingSupportCancelled(this);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(getError() == null) {
            if(mSenderSupportListener != null) {
                mSenderSupportListener.onSendingSupportFinished(this);
            }
        } else {
            if (mSenderSupportListener != null) {
                mSenderSupportListener.onSendingSupportError(this, getError());
            }
        }
    }

    /**
     * Conclude the process by sending a local broadcast with the task and result information.
     * @param success if it finished successfully
     */
    public void conclude(boolean success) {
        mFinishedIntent.putExtra(INTENT_TASK_TYPE, getClass());
        mFinishedIntent.putExtra(INTENT_THROWABLE, getError());
        mFinishedIntent.putExtra(INTENT_TASK_ID, getId());
        mFinishedIntent.putExtra(INTENT_SUCCESS, success);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(mFinishedIntent);
    }

    public void setSenderSupportListener(SenderSupportListener mListener) {
        this.mSenderSupportListener = mListener;
    }

    public SenderUploadTask getRelatedUploadTask() {
        return mRelatedUploadTask;
    }

    public void setRelatedUploadTask(SenderUploadTask mUploadTask) {
        this.mRelatedUploadTask = mUploadTask;
    }
}
