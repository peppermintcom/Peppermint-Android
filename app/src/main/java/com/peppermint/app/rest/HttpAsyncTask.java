package com.peppermint.app.rest;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.sending.server.AlreadyRegisteredException;
import com.peppermint.app.sending.server.InvalidAccessTokenException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 25-11-2015.
 *
 * Abstract implementation with the capability to execute and monitor asynchronous HTTP requests
 * through an {@link HttpClientManager} passed on as a parameter.
 */
public abstract class HttpAsyncTask extends AsyncTask<Void, Float, Void> implements Cloneable, HttpRequestListener {

    private static final String TAG = HttpAsyncTask.class.getSimpleName();

    public static final String PARAM_HTTP_CLIENT_MANAGER = TAG + "_ParamHttpClientManager";
    public static final String PARAM_HTTP_REQUEST_TIMEOUT = TAG + "_ParamHttpRequestTimeout";

    // default request timeout values (for connection + data transfer)
    private static final int SIMPLE_REQUEST_TIMEOUT = 60000;

    private UUID mId = UUID.randomUUID();

    // error thrown while executing the async task's doInBackground
    private Throwable mError;

    private Context mContext;
    private Map<String, Object> mParameters;

    // http request related
    private UUID mHttpRequestId;
    private String mHttpRequestUrl;
    private HttpResponse mHttpResponse;
    private Thread mRunner;

    public HttpAsyncTask(Context context) {
        this.mContext = context;
        this.mParameters = new HashMap<>();
    }

    public HttpAsyncTask(Context context, Map<String, Object> parameters) {
        this.mContext = context;
        if(parameters != null) {
            this.mParameters = new HashMap<>(parameters);
        } else {
            this.mParameters = new HashMap<>();
        }
    }

    public HttpAsyncTask(HttpAsyncTask task) {
        this(task.mContext, task.mParameters);
        this.mId = task.mId;
    }

    /**
     * Actual async task implementation
     * @throws Throwable
     */
    protected abstract void send() throws Throwable;

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

    protected String waitForHttpResponse() throws Throwable {
        try {
            Thread.sleep(getHttpRequestTimeout());
        } catch(InterruptedException e) {
            // nothing to do here
        }

        if(mHttpResponse == null) {
            throw new NoInternetConnectionException(mContext.getString(R.string.sender_msg_no_internet));
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
            throw new HttpResponseException("Exception running HTTP request to " + mHttpRequestUrl, mHttpResponse.getException());
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
        mHttpRequestUrl = request.getEndpoint();
        return getHttpClientManager().performRequest(request, response);
    }

    protected UUID executeHttpRequest(HttpRequest request) {
        mHttpRequestId = request.getUUID();
        mHttpRequestUrl = request.getEndpoint();
        return getHttpClientManager().performRequest(request);
    }

    protected UUID executeHttpRequest(String endpoint, int requestMethod, String body, String headerContentType, String headerAuth) {
        HttpRequest request = new HttpRequest(endpoint, requestMethod);
        if(headerContentType != null) {
            request.setHeaderParam("Content-Type", headerContentType);
        }
        if(headerAuth != null) {
            request.setHeaderParam("Authorization", headerAuth);
        }
        if(body != null) {
            request.setBody(body);
        }

        mHttpRequestId = request.getUUID();
        mHttpRequestUrl = request.getEndpoint();
        return getHttpClientManager().performRequest(request);
    }

    private long getHttpRequestTimeout() {
        Object timeout = getParameter(PARAM_HTTP_REQUEST_TIMEOUT);
        if (timeout == null) {
            return SIMPLE_REQUEST_TIMEOUT;
        }
        return (Long) timeout;
    }

    private HttpClientManager getHttpClientManager() {
        return (HttpClientManager) getParameter(PARAM_HTTP_CLIENT_MANAGER);
    }

    public Context getContext() {
        return mContext;
    }

    public Throwable getError() {
        return mError;
    }

    protected void setError(Throwable error) {
        this.mError = error;
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

    public UUID getId() {
        return mId;
    }

    public void setId(UUID mId) {
        this.mId = mId;
    }
}
