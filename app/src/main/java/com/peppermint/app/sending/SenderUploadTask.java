package com.peppermint.app.sending;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.api.exceptions.PeppermintApiInvalidAccessTokenException;

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

    public SenderUploadTask(Sender sender, SendingRequest sendingRequest, SenderUploadListener senderUploadListener) {
        super(sender, sendingRequest);
        this.mSenderUploadListener = senderUploadListener;
    }

    /**
     * Does the verifications necessary to perform {@link #uploadPeppermintMessage()}.<br />
     * <ol>
     *     <li>Validates the {@link SendingRequest#getRecording()} file</li>
     *     <li>Checks internet connection</li>
     *     <li>Checks the {@link PeppermintApi} access token</li>
     * </ol>
     * @throws Throwable
     */
    protected void uploadPeppermintMessageDoChecks() throws Throwable {
        getSendingRequest().getRecording().getValidatedFile();

        checkInternetConnection();

        if(getPeppermintApi().getAccessToken() == null) {
            throw new PeppermintApiInvalidAccessTokenException("Token is null!");
        }
    }

    /**
     * Uploads the {@link SendingRequest} recording to the Peppermint server.<br />
     * If successful, access URLs will be accessible through {@link SendingRequest#getServerShortUrl()} and {@link SendingRequest#getServerCanonicalUrl()}.
     * @throws Throwable
     */
    protected void uploadPeppermintMessage() throws Throwable {
        if(getSendingRequest().getServerShortUrl() != null && getSendingRequest().getServerCanonicalUrl() != null) {
            return;
        }

        PeppermintApi api = getPeppermintApi();
        String contentType = getSendingRequest().getRecording().getContentType();
        long now = android.os.SystemClock.uptimeMillis();

        // get AWS signed URL
        String signedUrl = (String) api.getSignedUrl(getSenderPreferences().getFullName(),
                getSenderPreferences().getGmailSenderPreferences().getPreferredAccountName(), contentType).getBody();
        getTrackerManager().log("Peppermint # Obtained AWS Signed URL at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");

        // upload to AWS
        if(!isCancelled()) {
            api.uploadMessage(signedUrl, getSendingRequest().getRecording().getFile(), contentType);
            getTrackerManager().log("Peppermint # Uploaded to AWS at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");
        }

        // confirm that the upload to AWS was successfully performed
        if(!isCancelled()) {
            PeppermintApi.JSONObjectItemResponse response = api.confirmUploadMessage(signedUrl);
            getSendingRequest().setServerCanonicalUrl(String.valueOf(response.getBody("canonical_url")));
            getSendingRequest().setServerShortUrl(String.valueOf(response.getBody("short_url")));
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
