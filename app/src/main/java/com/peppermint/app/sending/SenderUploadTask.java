package com.peppermint.app.sending;

import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.data.Message;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.api.data.RecordResponse;

import java.io.File;

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
     * Uploads the {@link Message} recording to the Peppermint server.<br />
     * If successful, access URLs will be accessible through {@link Message#getServerShortUrl()} and {@link Message#getServerCanonicalUrl()}.
     * @throws Throwable
     */
    protected void uploadPeppermintMessage() throws Throwable {
        File recordedFile = getMessage().getRecording().getValidatedFile();

        if(getMessage().getServerShortUrl() != null && getMessage().getServerCanonicalUrl() != null) {
            return;
        }

        AuthenticationData data = getAuthenticationData();

        PeppermintApi api = getPeppermintApi();
        String contentType = getMessage().getRecording().getContentType();
        long now = android.os.SystemClock.uptimeMillis();
        String fullName = getSenderPreferences().getFullName();
        if(fullName == null) {
            fullName = data.getEmail();
        }

        // get AWS signed URL
        String signedUrl = api.getSignedUrl(fullName, data.getEmail(), contentType).getSignedUrl();
        getTrackerManager().log("Peppermint # Obtained AWS Signed URL at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");

        // upload to AWS
        if(!isCancelled()) {
            api.uploadMessage(signedUrl, recordedFile, contentType);
            getTrackerManager().log("Peppermint # Uploaded to AWS at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");
        }

        // confirm that the upload to AWS was successfully performed
        if(!isCancelled()) {
            RecordResponse response = api.confirmUploadMessage(signedUrl);
            getMessage().setServerCanonicalUrl(String.valueOf(response.getCanonicalUrl()));
            getMessage().setServerShortUrl(String.valueOf(response.getShortUrl()));
            getTrackerManager().log("Peppermint # Confirmed Upload at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");
        }
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

    public boolean isRecovering() {
        return mRecovering;
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
