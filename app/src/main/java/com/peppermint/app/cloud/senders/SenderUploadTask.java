package com.peppermint.app.cloud.senders;

import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.cloud.apis.PeppermintApi;
import com.peppermint.app.cloud.apis.data.MessagesResponse;
import com.peppermint.app.cloud.apis.data.UploadsResponse;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiRecipientNoAppException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiResponseCodeException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiTooManyRequestsException;
import com.peppermint.app.cloud.senders.exceptions.NoInternetConnectionException;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.GlobalManager;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.data.Recipient;

import java.io.File;
import java.sql.SQLException;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     The abstract AsyncTask implementation executed by a {@link Sender}.<br />
 *     Each {@link Sender} must have their own {@link SenderUploadTask} concrete implementations.
 * </p>
 * <p>
 *     Check {@link SenderTask} for further information.
 * </p>
 */
public abstract class SenderUploadTask extends SenderTask implements Cloneable {

    private SenderUploadListener mSenderUploadListener;
    private boolean mRecovering = false;

    public SenderUploadTask(SenderUploadTask uploadTask) {
        super(uploadTask);
        this.mRecovering = uploadTask.mRecovering;
        this.mSenderUploadListener = uploadTask.mSenderUploadListener;
    }

    public SenderUploadTask(Sender sender, Message message, SenderUploadListener senderUploadListener) {
        super(sender, message);
        this.mSenderUploadListener = senderUploadListener;
    }

    /**
     * Uploads the {@link Message} to the Peppermint server.<br />
     * If successful, access URLs will be accessible through {@link Message#getServerShortUrl()}
     * and {@link Message#getServerCanonicalUrl()}.
     *
     * @throws Throwable
     */
    protected void uploadPeppermintMessage() throws Throwable {
        File recordedFile = getMessage().getRecordingParameter().getValidatedFile();

        if(getMessage().getServerShortUrl() != null && getMessage().getServerCanonicalUrl() != null) {
            return;
        }

        AuthenticationData data = getAuthenticationData();

        PeppermintApi api = getPeppermintApi();
        String contentType = getMessage().getRecordingParameter().getContentType();

        String fullName = getSenderPreferences().getFullName();
        if(fullName == null) {
            fullName = data.getEmail();
        }

        // get AWS signed URL
        UploadsResponse uploadsResponse = api.getSignedUrl(getId().toString(), fullName, data.getEmail(), contentType);
        String signedUrl = uploadsResponse.getSignedUrl();
        getMessage().setServerCanonicalUrl(uploadsResponse.getCanonicalUrl());
        getMessage().setServerShortUrl(uploadsResponse.getShortUrl());

        // upload to AWS
        if(!isCancelled()) {
            api.uploadMessage(getId().toString(), signedUrl, recordedFile, contentType);
        }
    }

    protected boolean sendPeppermintMessage() throws PeppermintApiTooManyRequestsException, PeppermintApiInvalidAccessTokenException, PeppermintApiResponseCodeException, ContactManager.InvalidEmailException, NoInternetConnectionException {
        boolean sentInApp;

        AuthenticationData data = getAuthenticationData();
        Message message = getMessage();
        String canonicalUrl = message.getServerCanonicalUrl();

        Recipient recipient = message.getChatParameter().getRecipientList().get(0);
        String recipientVia = recipient.getVia();

        if(isCancelled()) {
            return false;
        }

        try {
            MessagesResponse response = getPeppermintApi().sendMessage(getId().toString(), null, canonicalUrl,
                    data.getEmail(), recipientVia, (int) (message.getRecordingParameter().getDurationMillis() / 1000));
            message.setServerId(response.getMessageId());
            try {
                GlobalManager.markAsPeppermint(getContext(), recipient, data.getEmail());
            } catch (SQLException e) {
                getTrackerManager().logException(e);
            }
            sentInApp = true;
        } catch(PeppermintApiRecipientNoAppException e) {
            getTrackerManager().log("Unable to send through Peppermint", e);
            sentInApp = false;
            try {
                GlobalManager.unmarkAsPeppermint(getContext(), recipient);
            } catch (SQLException e1) {
                getTrackerManager().logException(e1);
            }
        }

        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getContext());
        databaseHelper.lock();
        try {
            MessageManager.update(databaseHelper.getWritableDatabase(), message);
        } catch (SQLException e) {
            getTrackerManager().logException(e);
        }
        databaseHelper.unlock();

        return sentInApp;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(mSenderUploadListener != null && !mRecovering) {
            mSenderUploadListener.onSendingUploadStarted(this);
        }
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        mProgress = values[0];
        if(mSenderUploadListener != null) {
            mSenderUploadListener.onSendingUploadProgress(this, mProgress);
        }
    }

    @Override
    protected void onCancelled(Void aVoid) {
        if(mSenderUploadListener != null) {
            mSenderUploadListener.onSendingUploadCancelled(this);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(getError() == null) {
            if(mSenderUploadListener != null) {
                mSenderUploadListener.onSendingUploadFinished(this);
            }
        } else {
            if (mSenderUploadListener != null) {
                mSenderUploadListener.onSendingUploadError(this, getError());
            }
        }
    }

    public void setRecovering(boolean mRecovering) {
        this.mRecovering = mRecovering;
    }

    public SenderUploadListener getSenderUploadListener() {
        return mSenderUploadListener;
    }

    public void setSenderUploadListener(SenderUploadListener mListener) {
        this.mSenderUploadListener = mListener;
    }
}
