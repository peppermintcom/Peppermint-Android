package com.peppermint.app.cloud.senders;

import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.cloud.apis.PeppermintApi;
import com.peppermint.app.cloud.apis.data.MessagesResponse;
import com.peppermint.app.cloud.apis.data.UploadsResponse;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiRecipientNoAppException;
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
    private boolean mNonCancellable = false;

    public SenderUploadTask(final SenderUploadTask uploadTask) {
        super(uploadTask);
        this.mRecovering = uploadTask.mRecovering;
        this.mSenderUploadListener = uploadTask.mSenderUploadListener;
    }

    public SenderUploadTask(final Sender sender, final Message message, final SenderUploadListener senderUploadListener) {
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
        final File recordedFile = getMessage().getRecordingParameter().getValidatedFile();

        if(getMessage().getServerShortUrl() != null && getMessage().getServerCanonicalUrl() != null) {
            return;
        }

        final PeppermintApi peppermintApi = getPeppermintApi();
        final AuthenticationData authenticationData = getAuthenticationData();
        final String contentType = getMessage().getRecordingParameter().getContentType();
        String fullName = getSenderPreferences().getFullName();
        if(fullName == null) {
            fullName = authenticationData.getEmail();
        }

        // get AWS signed URL
        final UploadsResponse uploadsResponse = peppermintApi.getSignedUrl(getId().toString(), fullName, authenticationData.getEmail(), contentType);
        final String signedUrl = uploadsResponse.getSignedUrl();
        getMessage().setServerCanonicalUrl(uploadsResponse.getCanonicalUrl());
        getMessage().setServerShortUrl(uploadsResponse.getShortUrl());

        // upload to AWS
        if(!isCancelled()) {
            peppermintApi.uploadMessage(getId().toString(), signedUrl, recordedFile, contentType);
        }
    }

    protected boolean sendPeppermintMessage() throws Exception {
        boolean sentInApp;

        final AuthenticationData data = getAuthenticationData();
        final Message message = getMessage();
        final String canonicalUrl = message.getServerCanonicalUrl();

        final Recipient recipient = message.getChatParameter().getRecipientList().get(0);
        final String recipientVia = recipient.getVia();

        setNonCancellable();

        if(isCancelled()) {
            return false;
        }

        final PeppermintApi peppermintApi = getPeppermintApi();

        try {
            final MessagesResponse response = peppermintApi.sendMessage(getId().toString(), null, canonicalUrl,
                    data.getEmail(), recipientVia, (int) (message.getRecordingParameter().getDurationMillis() / 1000));
            message.setServerId(response.getMessageId());
            try {
                GlobalManager.markAsPeppermint(getContext(), recipient, data.getEmail());
            } catch (SQLException e) {
                getTrackerManager().logException(e);
            }
            message.addConfirmedSentRecipientId(recipient.getId());
            sentInApp = true;
        } catch(PeppermintApiRecipientNoAppException e) {
            getTrackerManager().log("Unable to send through Peppermint", e);
            message.removeConfirmedSentRecipientId(recipient.getId());
            sentInApp = false;
            try {
                GlobalManager.unmarkAsPeppermint(getContext(), recipient);
            } catch (SQLException e1) {
                getTrackerManager().logException(e1);
            }
        }

        // immediately update message with serverId and sent recipients
        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getContext());
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

    public void setRecovering(final boolean mRecovering) {
        this.mRecovering = mRecovering;
    }

    public SenderUploadListener getSenderUploadListener() {
        return mSenderUploadListener;
    }

    public void setSenderUploadListener(final SenderUploadListener mListener) {
        this.mSenderUploadListener = mListener;
    }

    @Override
    public boolean cancel() {
        if(mNonCancellable) {
            return false;
        }
        return super.cancel();
    }

    protected void setNonCancellable() {
        mNonCancellable = true;
        if(mSenderUploadListener != null) {
            mSenderUploadListener.onSendingUploadNonCancellable(this);
        }
    }

    public boolean isNonCancellable() {
        return mNonCancellable;
    }
}
