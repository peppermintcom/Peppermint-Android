package com.peppermint.app.cloud.senders;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.peppermint.app.cloud.apis.GoogleApi;
import com.peppermint.app.cloud.apis.PeppermintApi;
import com.peppermint.app.cloud.senders.mail.gmail.GmailSender;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.Message;
import com.peppermint.app.tracking.TrackerManager;

import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     A Sender abstract implementation that sends a {@link Message} ({@link com.peppermint.app.data.Recording} to
 *     {@link ContactRaw}) through a particular medium/protocol.
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

    public Sender(final Context context, final TrackerManager trackerManager, final Map<String, Object> parameters, final SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, new SenderPreferences(context));
        this.mSenderUploadListener = senderUploadListener;
        this.mErrorHandler = new SenderErrorHandler(this, mSenderUploadListener);
    }

    public Sender(final SenderObject objToExtend, final SenderUploadListener senderUploadListener) {
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
     * Creates a new {@link SenderUploadTask} instance for the specified message.
     * @param message the message
     * @param enforcedId the id of the task or null for a random id
     * @return the asynchronous task that will try to execute the sending request
     */
    public abstract SenderUploadTask newTask(Message message, UUID enforcedId);

    /**
     * Gets the {@link SenderErrorHandler}. Useful to check if the sender can recover from
     * an error triggered during the {@link SenderUploadTask} execution. If it recovers, a new
     * {@link SenderUploadTask} for the same message can be launched to see if it everything
     * runs smoothly the second time.
     *
     * @return the error handler instance
     */
    public SenderErrorHandler getSenderErrorHandler() {
        return mErrorHandler;
    }

    public void setSenderErrorHandler(final SenderErrorHandler senderErrorHandler) {
        this.mErrorHandler = senderErrorHandler;
    }

    public SenderUploadListener getSenderUploadListener() {
        return mSenderUploadListener;
    }

    public Sender setSenderUploadListener(final SenderUploadListener senderUploadListener) {
        this.mSenderUploadListener = senderUploadListener;
        return this;
    }

    public Sender getFailureChainSender() {
        return mFailureChainSender;
    }

    public void setFailureChainSender(final Sender mFailureChainSender) {
        this.mFailureChainSender = mFailureChainSender;
    }

    protected GoogleApi getGoogleApi(final String email) {
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

    protected PeppermintApi getPeppermintApi() {
        PeppermintApi api = (PeppermintApi) getParameter(Sender.PARAM_PEPPERMINT_API);
        if(api == null) {
            api = new PeppermintApi(mContext);
            setParameter(Sender.PARAM_PEPPERMINT_API, api);
        }
        return api;
    }

    public void setEnabled(boolean val) {
        setEnabled(mContext, this.getClass(), val);
    }

    public boolean isEnabled() {
        return isEnabled(mContext, this.getClass());
    }

    public static void setEnabled(final Context context, final Class<? extends Sender> clazz, final boolean val) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getEnabledPreferenceKey(clazz), val);
        editor.commit();
    }

    public static boolean isEnabled(final Context context, final Class<? extends Sender> clazz) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(getEnabledPreferenceKey(clazz), true);
    }

    public static String getEnabledPreferenceKey(final Class<? extends Sender> clazz) {
        return clazz.getSimpleName() + "_isEnabled";
    }
}
