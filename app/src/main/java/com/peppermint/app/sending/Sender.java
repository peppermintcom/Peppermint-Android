package com.peppermint.app.sending;

import android.content.Context;

import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.api.exceptions.PeppermintApiNoAccountException;
import com.peppermint.app.sending.mail.gmail.GmailSender;
import com.peppermint.app.tracking.TrackerManager;

import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     A Sender abstract implementation that sends {@link com.peppermint.app.data.Recording} to
 *     {@link com.peppermint.app.data.Recipient} through a particular medium/protocol.
 *</p>
 * <p>
 *     Senders are an extension of {@link SenderObject}s, thus containing common contextual data.
 *</p>
 * <p>
 *     If a sender fails to execute a request, its failure chain sender can be used to try and
 *     do it afterwards. Several senders can be setup to form a failure chain.
 * </p>
 */
public abstract class Sender extends SenderObject {

    private static final String TAG = Sender.class.getSimpleName();

    public static final String PARAM_PEPPERMINT_API = TAG + "_paramPeppermintApi";
    public static final String PARAM_GOOGLE_API = TAG + "_paramGoogleApi";

    protected SenderErrorHandler mErrorHandler;
    private SenderUploadListener mSenderUploadListener;
    private Sender mFailureChainSender;

    public Sender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, DatabaseHelper databaseHelper, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, null, databaseHelper);
        this.mSenderUploadListener = senderUploadListener;
        this.mErrorHandler = new SenderErrorHandler(this, mSenderUploadListener);
    }

    public Sender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend);
        this.mSenderUploadListener = senderUploadListener;
        this.mErrorHandler = new SenderErrorHandler(this, mSenderUploadListener);
    }

    /**
     * Initializes the sender and its error handler.
     */
    public void init() {
        // just create shared api instances
        getPeppermintApi();
        getGoogleApi(null);

        if(mErrorHandler != null) {
            mErrorHandler.init();
        }
    }

    /**
     * De-initializes the sender and its error handler.
     */
    public void deinit() {
        if(mErrorHandler != null) {
            mErrorHandler.deinit();
        }
    }

    /**
     * Creates a new {@link SenderTask} instance for the specified sending request.
     * @param message the sending request
     * @return the asynchronous task that will try to execute the sending request
     */
    public abstract SenderUploadTask newTask(Message message, UUID enforcedId);

    /**
     * Gets the sender's error handler. Useful to check if the sender can recover from
     * an error triggered during the sending task execution. If it recovers, a new sending task
     * for the same request can be launched to see if it everything runs smoothly the second time.
     *
     * @return the error handler
     */
    public SenderErrorHandler getSenderErrorHandler() {
        return mErrorHandler;
    }

    public void setSenderErrorHandler(SenderErrorHandler senderErrorHandler) {
        this.mErrorHandler = senderErrorHandler;
    }

    public SenderUploadListener getSenderUploadListener() {
        return mSenderUploadListener;
    }

    public Sender setSenderUploadListener(SenderUploadListener senderUploadListener) {
        this.mSenderUploadListener = senderUploadListener;
        return this;
    }

    public Sender getFailureChainSender() {
        return mFailureChainSender;
    }

    public void setFailureChainSender(Sender mFailureChainSender) {
        this.mFailureChainSender = mFailureChainSender;
    }

    protected GoogleApi getGoogleApi(String email) {
        GoogleApi api = (GoogleApi) getParameter(GmailSender.PARAM_GOOGLE_API);
        if(api == null) {
            api = new GoogleApi(getContext());
            setParameter(GmailSender.PARAM_GOOGLE_API, api);
        }
        if(api.getCredential() == null || api.getService() == null || api.getAccountName().compareTo(email) != 0) {
            api.setAccountName(email);
        }
        return api;
    }

    protected void setGoogleApi(GoogleApi googleApi) {
        setParameter(Sender.PARAM_GOOGLE_API, googleApi);
    }

    protected PeppermintApi getPeppermintApi() {
        PeppermintApi api = (PeppermintApi) getParameter(Sender.PARAM_PEPPERMINT_API);
        if(api == null) {
            api = new PeppermintApi();
            setPeppermintApi(api);
        }
        return api;
    }

    protected void setPeppermintApi(PeppermintApi peppermintApi) {
        setParameter(Sender.PARAM_PEPPERMINT_API, peppermintApi);
    }
}
