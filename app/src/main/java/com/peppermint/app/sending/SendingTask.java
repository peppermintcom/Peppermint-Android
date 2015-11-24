package com.peppermint.app.sending;

import android.os.AsyncTask;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.rest.HttpClientManager;
import com.peppermint.app.rest.HttpRequest;
import com.peppermint.app.rest.HttpRequestListener;
import com.peppermint.app.rest.HttpResponse;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.sending.server.AlreadyRegisteredException;
import com.peppermint.app.sending.server.HttpResponseException;
import com.peppermint.app.sending.server.InvalidAccessTokenException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     The abstract AsyncTask implementation that executes a {@link SendingRequest}.<br />
 *     Each {@link Sender} must have its {@link SendingTask} concrete implementation.
 * </p>
 * <p>
 *     As with {@link Sender}s, sending tasks are configured through Parameters and Preferences.
 *     Preferences use the native Android Shared Preferences mechanism, so that data is saved
 *     across different executions of the app. Parameters are part of a key-value map passed to
 *     the sending task instance (each implementation may have its own parameters).
 * </p>
 */
public abstract class SendingTask extends AsyncTask<Void, Float, Void> implements Cloneable, HttpRequestListener {

    private static final String TAG = SendingTask.class.getSimpleName();

    private static final int SIMPLE_REQUEST_TIMEOUT = 60000;

    public static final float PROGRESS_INDETERMINATE = -1f;
    public static final float PROGRESS_MAX = 100f;
    public static final float PROGRESS_MIN = 0f;

    private Sender mSender;
    private SendingRequest mSendingRequest;
    private Throwable mError;
    private float mProgress = PROGRESS_INDETERMINATE;

    private Map<String, Object> mParameters;
    private SenderPreferences mPreferences;

    private SenderListener mListener;

    // http request related
    private UUID mHttpRequestId;
    private boolean mGotHttpResponse = false;
    private HttpResponse mHttpResponse;
    private Thread mRunner;

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

    protected String waitForHttpResponse() throws Throwable {
        try {
            Thread.sleep(SIMPLE_REQUEST_TIMEOUT);
        } catch(InterruptedException e) {
            // nothing to do here
        }

        if(mHttpResponse == null) {
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.msg_no_internet));
        }
        if(mHttpResponse.getCode() == 401) {
            throw new InvalidAccessTokenException();
        }
        if(mHttpResponse.getCode() == 409) {
            throw new AlreadyRegisteredException();
        }
        if((mHttpResponse.getCode()/100) != 2) {
            throw new HttpResponseException();
        }
        if(mHttpResponse.getException() != null) {
            throw mHttpResponse.getException();
        }

        return mHttpResponse.getBody().toString();
    }

    @Override
    public void onRequestSuccess(HttpRequest request, HttpResponse response) {
        handleResult(request, response);
    }

    @Override
    public void onRequestError(HttpRequest request, HttpResponse response) {
        handleResult(request, response);
    }

    private void handleResult(HttpRequest request, HttpResponse response) {
        if(mHttpRequestId != null && request.getUUID().equals(mHttpRequestId)) {
            mHttpResponse = response;
            mGotHttpResponse = true;
            if(mRunner != null) {
                mRunner.interrupt();
            }
        }
    }

    @Override
    public void onRequestCancel(HttpRequest request) {
        if(mRunner != null && mHttpRequestId != null && request.getUUID().equals(mHttpRequestId)) {
            mRunner.interrupt();
            mHttpRequestId = null;
        }
    }

    protected UUID executeHttpRequest(HttpRequest request, HttpResponse response) {
        mHttpRequestId = request.getUUID();
        return getHttpClientManager().performRequest(request, response);
    }

    private HttpClientManager getHttpClientManager() {
        return (HttpClientManager) getParameter(Sender.PARAM_HTTP_CLIENT_MANAGER);
    }

    /**
     * Actually sends the audio/video file to a {@link com.peppermint.app.data.Recipient}
     * @throws Throwable
     */
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
        mRunner = Thread.currentThread();
        if(getHttpClientManager() != null) {
            getHttpClientManager().addHttpRequestListener(this);
        }
        try {
            send();
        } catch (Throwable e) {
            mError = e;
            Log.w(TAG, e);
        } finally {
            if(getHttpClientManager() != null) {
                getHttpClientManager().removeHttpRequestListener(this);
            }
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
