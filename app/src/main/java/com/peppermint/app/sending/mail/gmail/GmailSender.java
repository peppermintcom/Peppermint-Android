package com.peppermint.app.sending.mail.gmail;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.common.Scopes;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.mail.MailSenderPreferences;

import java.util.Arrays;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Sender that uses the GMail API to send audio messages through email.
 */
public class GmailSender extends Sender {

    // GmailSenderTask parameter keys
    public static final String PARAM_GMAIL_SERVICE = "GmailSendingTask_paramGmailService";
    public static final String PARAM_GMAIL_CREDENTIAL = "GmailSendingTask_paramGmailCredentials";

    // Gmail API required permissions
    protected static final String[] SCOPES = { GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_MODIFY, "https://www.googleapis.com/auth/userinfo.profile" };

    protected Gmail mService;
    protected GoogleAccountCredential mCredential;

    private final HttpTransport mTransport = AndroidHttp.newCompatibleTransport();
    private final JsonFactory mJsonFactory = GsonFactory.getDefaultInstance();

    private GmailSenderErrorHandler mErrorHandler;
    private GmailSenderPreferences mPreferences;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if((key.compareTo(MailSenderPreferences.ACCOUNT_NAME_KEY) == 0 ||
                    key.compareTo(GmailSenderPreferences.getEnabledPreferenceKey(GmailSenderPreferences.class)) == 0)
                    && mPreferences.isEnabled()) {
                setupCredentials();
            }
        }
    };

    public GmailSender(Context context, SenderListener senderListener) {
        super(context, senderListener);
        mPreferences = new GmailSenderPreferences(getContext());
        setUseHttpManager(true);
    }

    private void setupCredentials() {
        // initialize the Gmail API objects and pass them as parameters to the error handler
        // and to all associated sending tasks
        this.mCredential = GoogleAccountCredential.usingOAuth2(
                getContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(mPreferences.getPreferredAccountName());

        this.mService = new Gmail.Builder(
                mTransport, mJsonFactory, mCredential)
                .setApplicationName(getContext().getString(R.string.app_name))
                .build();

        setParameter(PARAM_GMAIL_CREDENTIAL, mCredential);
        setParameter(PARAM_GMAIL_SERVICE, mService);
    }

    @Override
    public void init() {
        setupCredentials();
        mPreferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        super.init();
    }

    @Override
    public void deinit() {
        mPreferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        super.deinit();
    }

    @Override
    public SenderTask newAuthorizationTask() {
        return new GmailAuthorizationTask(this, getSenderListener(), getParameters(), getSenderPreferences());
    }

    @Override
    public SenderTask newTask(SendingRequest sendingRequest) {
        return new GmailSenderTask(this, sendingRequest, getSenderListener(), getParameters(), getSenderPreferences());
    }

    @Override
    public SenderErrorHandler getErrorHandler() {
        if(mErrorHandler == null) {
            mErrorHandler = new GmailSenderErrorHandler(getContext(), getSenderListener(), getParameters(), getSenderPreferences());
        }
        return mErrorHandler;
    }

    @Override
    public SenderPreferences getSenderPreferences() {
        return mPreferences;
    }

    public GoogleAccountCredential getCredential() {
        return mCredential;
    }

    public Gmail getService() {
        return mService;
    }
}
