package com.peppermint.app.sending.server;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.rest.HttpRequest;
import com.peppermint.app.rest.HttpRequestListener;
import com.peppermint.app.rest.HttpResponse;
import com.peppermint.app.rest.HttpResponseException;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * SenderTask for emails using the Gmail API.
 */
public class ServerSenderTask extends SenderTask implements HttpRequestListener {

    public static final String PARAM_SHORT_URL = "ServerSender_ShortMessageUrl";
    public static final String PARAM_CANONICAL_URL = "ServerSender_CanonicalMessageUrl";

    private static final String TAG = ServerSenderTask.class.getSimpleName();
    private static final int SIMPLE_REQUEST_TIMEOUT = 60000;

    private String mRequestUrl;
    private UUID mRequestId;
    private HttpResponse mResponse;
    private Thread mRunner;

    public ServerSenderTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        super(sender, sendingRequest, listener);
    }

    public ServerSenderTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, sendingRequest, listener, parameters, preferences);
    }

    public ServerSenderTask(ServerSenderTask sendingTask) {
        super(sendingTask);
    }

    private String waitForResponse() throws Throwable {
        try {
            Thread.sleep(SIMPLE_REQUEST_TIMEOUT);
        } catch(InterruptedException e) {
            // nothing to do here
        }

        if(mResponse == null) {
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.msg_no_internet));
        }
        if(mResponse.getCode() == 401) {
            throw new InvalidAccessTokenException();
        }
        if(mResponse.getCode() == 409) {
            throw new AlreadyRegisteredException();
        }
        if((mResponse.getCode()/100) != 2) {
            throw new HttpResponseException();
        }
        if(mResponse.getException() != null) {
            throw new HttpResponseException("Exception running HTTP request to " + mRequestUrl, mResponse.getException());
        }

        return mResponse.getBody().toString();
    }

    @Override
    public void send() throws Throwable {
        mRunner = Thread.currentThread();

        File file = getSendingRequest().getRecording().getValidatedFile();

        if(!Utils.isInternetAvailable(getSender().getContext()) || !Utils.isInternetActive(getSender().getContext())) {
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.msg_no_internet));
        }

        ServerClientManager manager = (ServerClientManager) getParameter(ServerSender.PARAM_MANAGER);
        if(manager.getAccessToken() == null) {
            throw new InvalidAccessTokenException();
        }

        manager.addHttpRequestListener(this);
        final String contentType = "audio/mp4";

        try {
            // UPLOADS ENDPOINT INVOCATION
            mRequestUrl = ServerClientManager.UPLOADS_ENDPOINT;
            mResponse = null;
            mRequestId = manager.startUpload(contentType);
            JSONObject response = new JSONObject(waitForResponse());
            String signedUrl = response.getString("signed_url");

            // UPLOAD TO AWS HERE!
            if(!isCancelled()) {
                mRequestUrl = signedUrl;
                mResponse = null;
                mRequestId = manager.performUpload(signedUrl, getSendingRequest().getRecording().getFile(), contentType, null);
                waitForResponse();
            }

            // RECORD ENDPOINT INVOCATION
            if(!isCancelled()) {
                mRequestUrl = ServerClientManager.RECORD_ENDPOINT;
                mResponse = null;
                mRequestId = manager.finishUpload(signedUrl);
                response = new JSONObject(waitForResponse());
                String shortUrl = response.getString("short_url");
                String canonicalUrl = response.getString("canonical_url");
                getSendingRequest().setParameter(PARAM_SHORT_URL, shortUrl);
                getSendingRequest().setParameter(PARAM_CANONICAL_URL, canonicalUrl);
            }

        } finally {
            manager.removeHttpRequestListener(this);
            mRunner = null;
        }
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
        if(mRequestId != null && request.getUUID().equals(mRequestId)) {
            mResponse = response;
            if(mRunner != null) {
                mRunner.interrupt();
            }
        }
    }

    @Override
    public void onRequestCancel(HttpRequest request) {
        if(mRunner != null) {
            mRunner.interrupt();
        }
    }
}
