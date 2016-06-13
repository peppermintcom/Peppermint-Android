package com.peppermint.app.services.messenger.handlers;

import android.content.Context;
import android.os.AsyncTask;

import com.peppermint.app.cloud.apis.google.GoogleApi;
import com.peppermint.app.cloud.apis.google.GoogleApiNoAuthorizationException;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApi;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiNoAccountException;
import com.peppermint.app.cloud.apis.sparkpost.SparkPostApi;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.services.authenticator.AuthenticationData;
import com.peppermint.app.services.authenticator.AuthenticatorUtils;
import com.peppermint.app.services.messenger.handlers.gmail.GmailSender;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     The abstract AsyncTask implementation executed by a {@link Sender} or {@link SenderErrorHandler}.<br />
 *     Each {@link Sender} and {@link SenderErrorHandler} must have their own concrete implementations
 *     of either {@link SenderUploadTask} or {@link SenderSupportTask}.<br />
 *     <ul>
 *     <li>{@link SenderUploadTask} perform main routines and are handled directly by the
 *     {@link SenderManager}.</li>
 *     <li>{@link SenderSupportTask} run support routines and are usually launched by the
 *     {@link SenderErrorHandler} to handle errors asynchronously.</li>
 *     </ul>
 * </p>
 * <p>
 *     As with {@link Sender}s, {@link SenderTask}s require the base contextual data found in {@link SenderObject}.
 *     This data is accessible through the instance returned by {@link #getIdentity()}.
 * </p>
 */
public abstract class SenderTask extends AsyncTask<Void, Float, Void> implements Cloneable {

    private static final String TAG = SenderTask.class.getSimpleName();

    protected static final String PARAM_AUTHENTICATION_DATA = TAG + "_paramAuthenticationData";

    public static final float PROGRESS_INDETERMINATE = -1f;

    protected float mProgress = PROGRESS_INDETERMINATE;

    private SenderObject mIdentity;

    // error thrown while executing the async task's doInBackground
    private Throwable mError;
    private transient Sender mSender;
    private Message mMessage;

    public SenderTask(Sender sender, Message message) {
        if(sender != null) {
            this.mIdentity = new SenderObject(sender);
        } else {
            this.mIdentity = new SenderObject(null, null, null, null);
        }
        this.mSender = sender;
        this.mMessage = message;
    }

    public SenderTask(SenderTask sendingTask) {
        this.mIdentity = sendingTask.mIdentity;
        this.mSender = sendingTask.mSender;
        this.mMessage = sendingTask.mMessage;
    }

    protected void checkInternetConnection() throws NoInternetConnectionException {
        if(!Utils.isInternetAvailable(getContext()) || !Utils.isInternetActive()) {
            throw new NoInternetConnectionException();
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
            mError = e;
        }
        return null;
    }

    public Sender getSender() {
        return mSender;
    }

    public Message getMessage() {
        return mMessage;
    }

    public void setMessage(Message mMessage) {
        this.mMessage = mMessage;
    }

    public Throwable getError() {
        return mError;
    }

    protected void setError(Throwable error) {
        this.mError = error;
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

    protected GoogleApi getGoogleApi(String email) {
        GoogleApi api = (GoogleApi) getParameter(GmailSender.PARAM_GOOGLE_API);
        if(api == null) {
            api = new GoogleApi(getContext());
            setParameter(GmailSender.PARAM_GOOGLE_API, api);
        }
        if(api.getCredential() == null || api.getAccountName().compareTo(email) != 0) {
            api.setAccountName(email);
        }
        return api;
    }

    protected PeppermintApi getPeppermintApi() {
        PeppermintApi api = (PeppermintApi) mIdentity.getParameter(Sender.PARAM_PEPPERMINT_API);
        if(api == null) {
            api = new PeppermintApi(getContext());
            setParameter(Sender.PARAM_PEPPERMINT_API, api);
        }
        return api;
    }

    protected SparkPostApi getSparkPostApi() {
        SparkPostApi api = (SparkPostApi) mIdentity.getParameter(Sender.PARAM_SPARKPOST_API);
        if(api == null) {
            api = new SparkPostApi(getContext());
            setParameter(Sender.PARAM_SPARKPOST_API, api);
        }
        return api;
    }

    protected void setAuthenticationData(AuthenticationData data) {
        mIdentity.setParameter(PARAM_AUTHENTICATION_DATA, data);
    }

    protected AuthenticationData getAuthenticationData() {
        return (AuthenticationData) mIdentity.getParameter(PARAM_AUTHENTICATION_DATA);
    }

    protected AuthenticationData setupPeppermintAuthentication() throws Exception {
        return setupPeppermintAuthentication(false);
    }

    /**
     * Setups the PeppermintApi.<br />
     * <ol>
     *     <li>Checks internet connection</li>
     *     <li>Checks the {@link PeppermintApi} access token</li>
     * </ol>
     * @param invalidateToken true to forcefully invalidate the Peppermint API access token
     * @throws NoInternetConnectionException
     * @throws PeppermintApiNoAccountException
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws IOException
     * @throws GoogleApiNoAuthorizationException
     */
    protected AuthenticationData setupPeppermintAuthentication(final boolean invalidateToken) throws Exception {
        checkInternetConnection();

        final PeppermintApi peppermintApi = getPeppermintApi();
        String accessToken = peppermintApi.peekAuthenticationToken();

        if(invalidateToken || accessToken == null) {
            accessToken = peppermintApi.renewAuthenticationToken();
            if(accessToken == null) {
                throw new PeppermintApiInvalidAccessTokenException("Token is null!");
            }
        }

        final AuthenticatorUtils authenticatorUtils = new AuthenticatorUtils(getContext());
        final AuthenticationData authenticationData = authenticatorUtils.getAccountData();
        setAuthenticationData(authenticationData);
        return authenticationData;
    }

    public boolean cancel() {
        boolean cancelled = cancel(true);
        // necessary to effectively stop HttpUrlConnections
        final PeppermintApi peppermintApi = (PeppermintApi) getParameter(Sender.PARAM_PEPPERMINT_API);
        if(peppermintApi != null) {
            peppermintApi.cancelPendingRequests(mIdentity.getId().toString());
        }
        final GoogleApi googleApi = (GoogleApi) getParameter(Sender.PARAM_GOOGLE_API);
        if(googleApi != null) {
            googleApi.cancelPendingRequests(mIdentity.getId().toString());
        }
        final SparkPostApi sparkPostApi = (SparkPostApi) getParameter(Sender.PARAM_SPARKPOST_API);
        if(sparkPostApi != null) {
            sparkPostApi.cancelPendingRequests(mIdentity.getId().toString());
        }
        return cancelled;
    }
}
