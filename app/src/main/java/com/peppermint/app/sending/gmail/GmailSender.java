package com.peppermint.app.sending.gmail;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

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
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingErrorHandler;
import com.peppermint.app.sending.SendingTask;

import java.util.Arrays;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Sender that uses the GMail API to send audio messages through email.
 */
public class GmailSender extends Sender {

    // GmailSendingTask parameter keys
    public static final String PARAM_DISPLAY_NAME = "GmailSendingTask_paramDisplayName";
    public static final String PARAM_GMAIL_SERVICE = "GmailSendingTask_paramGmailService";
    public static final String PARAM_GMAIL_CREDENTIAL = "GmailSendingTask_paramGmailCredentials";

    // Gmail API required permissions
    private static final String[] SCOPES = { GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_MODIFY };

    protected Gmail mService;
    protected GoogleAccountCredential mCredential;

    private final HttpTransport mTransport = AndroidHttp.newCompatibleTransport();
    private final JsonFactory mJsonFactory = GsonFactory.getDefaultInstance();

    private GmailSendingErrorHandler mErrorHandler;
    private GmailSenderPreferences mPreferences;

    public GmailSender(Context context, SenderListener senderListener) {
        super(context, senderListener);
        mPreferences = new GmailSenderPreferences(getContext());
    }

    @Override
    public void init() {
        if(getParameter(PARAM_DISPLAY_NAME) == null) {
            Cursor cursor = getContext().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
            if(cursor != null) {
                if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                    String userName = cursor.getString(cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
                    if (userName != null) {
                        setParameter(PARAM_DISPLAY_NAME, userName);
                    }
                }
                cursor.close();
            }
        }

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

        super.init();
    }

    @Override
    public SendingTask newTask(SendingRequest sendingRequest) {
        return new GmailSendingTask(this, sendingRequest, getSenderListener(), getParameters(), getSenderPreferences());
    }

    @Override
    public SendingErrorHandler getErrorHandler() {
        if(mErrorHandler == null) {
            mErrorHandler = new GmailSendingErrorHandler(getContext(), getSenderListener(), getParameters(), getSenderPreferences());
        }
        return mErrorHandler;
    }

    @Override
    public SenderPreferences getSenderPreferences() {
        return mPreferences;
    }
}
