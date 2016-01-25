package com.peppermint.app.sending;

import com.peppermint.app.data.SendingRequest;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     The abstract AsyncTask implementation executed by a {@link Sender}.<br />
 *     Each {@link Sender} must have their own {@link SenderSupportTask} concrete implementations.
 * </p>
 * <p>
 *     Check {@link SenderTask} for further information.
 * </p>
 */
public abstract class SenderSupportTask extends SenderTask implements Cloneable {

    private SenderSupportListener mSenderSupportListener;

    public SenderSupportTask(SenderSupportTask supportTask) {
        super(supportTask);
        this.mSenderSupportListener = supportTask.mSenderSupportListener;
    }

    public SenderSupportTask(Sender sender, SendingRequest sendingRequest, SenderSupportListener senderSupportListener) {
        super(sender, sendingRequest);
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

    public SenderSupportListener getSenderSupportListener() {
        return mSenderSupportListener;
    }

    public void setSenderSupportListener(SenderSupportListener mListener) {
        this.mSenderSupportListener = mListener;
    }
}
