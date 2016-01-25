package com.peppermint.app.sending.mail.gmail;

import android.content.Context;
import android.content.SharedPreferences;

import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderObject;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.sending.mail.MailSenderPreferences;
import com.peppermint.app.tracking.TrackerManager;

import java.util.Map;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Sender that uses the GMail API to send audio messages through email.
 */
public class GmailSender extends Sender {

    private static final String TAG = GmailSender.class.getSimpleName();

    protected GoogleApi mGoogleApi;

    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if((key.compareTo(MailSenderPreferences.ACCOUNT_NAME_KEY) == 0 ||
                    key.compareTo(GmailSenderPreferences.getEnabledPreferenceKey(GmailSenderPreferences.class)) == 0)
                    && getPreferences().isEnabled()) {
                setupCredentials();
            }
        }
    };

    public GmailSender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, DatabaseHelper databaseHelper, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, databaseHelper, senderUploadListener);
        construct();
    }

    public GmailSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
        construct();
    }

    private void construct() {
        mPreferences = new GmailSenderPreferences(getContext());
        mErrorHandler = new GmailSenderErrorHandler(this, getSenderUploadListener());
    }

    private void setupCredentials() {
        // initialize the Gmail API objects and pass them as parameters to the error handler
        // and to all associated sending tasks
        this.mGoogleApi = new GoogleApi(getContext());
        this.mGoogleApi.setAccountName(((MailSenderPreferences) getPreferences()).getPreferredAccountName());

        setParameter(PARAM_GOOGLE_API, mGoogleApi);
    }

    @Override
    public void init() {
        setupCredentials();
        getPreferences().getSharedPreferences().registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        super.init();
    }

    @Override
    public void deinit() {
        getPreferences().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        super.deinit();
    }

    @Override
    public SenderUploadTask newTask(SendingRequest sendingRequest) {
        return new GmailSenderTask(this, sendingRequest, getSenderUploadListener());
    }
}
