package com.peppermint.app.sending;

import android.content.Context;
import android.os.AsyncTask;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.exceptions.ElectableForQueueingException;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLException;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     The abstract AsyncTask implementation executed by {@link Sender} or {@link SenderErrorHandler}.<br />
 *     Each {@link Sender} must have their own concrete implementations of either {@link SenderUploadTask} or {@link SenderSupportTask}.<br />
 *     <ul>
 *     <li>{@link SenderUploadTask} perform main routines and are handled directly by the {@link SenderManager}.</li>
 *     <li>{@link SenderSupportTask} run support routines and are usually launched by the {@link SenderErrorHandler} to handle errors asynchronously.</li>
 *     </ul>
 * </p>
 * <p>
 *     As with {@link Sender}s, {@link SenderTask}s require the base contextual data found in {@link SenderObject}.
 *     This data is accessible through the instance returned by {@link #getIdentity()}.
 * </p>
 */
public abstract class SenderTask extends AsyncTask<Void, Float, Void> implements Cloneable {

    public static final float PROGRESS_INDETERMINATE = -1f;
    public static final float PROGRESS_MAX = 100f;
    public static final float PROGRESS_MIN = 0f;

    protected float mProgress = PROGRESS_INDETERMINATE;

    private SenderObject mIdentity;

    // error thrown while executing the async task's doInBackground
    private Throwable mError;
    private Sender mSender;
    private SendingRequest mSendingRequest;

    public SenderTask(Sender sender, SendingRequest sendingRequest) {
        this.mIdentity = new SenderObject(sender);
        this.mSender = sender;
        this.mSendingRequest = sendingRequest;
        if(sendingRequest != null) {
            mIdentity.setId(sendingRequest.getId());
        }
    }

    public SenderTask(SenderTask sendingTask) {
        this.mIdentity = sendingTask.mIdentity;
        this.mSender = sendingTask.mSender;
        this.mSendingRequest = sendingTask.mSendingRequest;
    }

    protected void checkInternetConnection() throws NoInternetConnectionException {
        if(!Utils.isInternetAvailable(getSender().getContext()) || !Utils.isInternetActive(getSender().getContext())) {
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.sender_msg_no_internet));
        }
    }

    /**
     * Concrete routine implementation of the task.
     * @throws Throwable any error/exception thrown while executing
     */
    protected abstract void execute() throws Throwable;

    @Override
    protected Void doInBackground(Void... params) {
        try {
            execute();
        } catch (Throwable e) {
            // hack to queue SSL errors
            // some wifis might require proxies that override security certificates
            // this queues the message and retries sending after connection is established
            // on some other network
            if(e instanceof SSLException) {
                mError = new ElectableForQueueingException(e);
            } else {
                mError = e;
            }
        }
        return null;
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

    public Throwable getError() {
        return mError;
    }

    protected void setError(Throwable error) {
        this.mError = error;
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

    public SenderObject getIdentity() {
        return mIdentity;
    }

    public SenderPreferences getSenderPreferences() {
        return mIdentity.getPreferences();
    }

    public Context getContext() {
        return mIdentity.getContext();
    }

    public Map<String, Object> getParameters() {
        return mIdentity.getParameters();
    }

    public void setParameters(Map<String, Object> mParameters) {
        mIdentity.setParameters(mParameters);
    }

    public Object getParameter(String key) {
        return mIdentity.getParameter(key);
    }

    public void setParameter(String key, Object value) {
        mIdentity.setParameter(key, value);
    }

    public UUID getId() {
        return mIdentity.getId();
    }

    public TrackerManager getTrackerManager() {
        return mIdentity.getTrackerManager();
    }

    protected PeppermintApi getPeppermintApi() {
        return (PeppermintApi) mIdentity.getParameter(Sender.PARAM_PEPPERMINT_API);
    }

    protected GoogleApi getGoogleApi() {
        return (GoogleApi) mIdentity.getParameter(Sender.PARAM_GOOGLE_API);
    }
}
