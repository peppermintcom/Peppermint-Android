package com.peppermint.app.services.messenger.handlers;

import android.content.Intent;
import android.util.Log;

import com.peppermint.app.cloud.apis.peppermint.PeppermintApi;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiRecipientNoAppException;
import com.peppermint.app.cloud.apis.peppermint.objects.MessagesResponse;
import com.peppermint.app.cloud.apis.peppermint.objects.TranscriptionResponse;
import com.peppermint.app.cloud.apis.peppermint.objects.UploadsResponse;
import com.peppermint.app.cloud.apis.speech.GoogleSpeechRecognizeClient;
import com.peppermint.app.dal.DataObjectManager;
import com.peppermint.app.dal.GlobalManager;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.message.MessageManager;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.dal.recording.Recording;
import com.peppermint.app.dal.recording.RecordingManager;
import com.peppermint.app.services.authenticator.AuthenticationData;
import com.peppermint.app.services.recorder.RecordService;
import com.peppermint.app.services.recorder.RecorderEvent;

import java.io.File;
import java.io.IOException;
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

    private static final String TAG = SenderUploadTask.class.getSimpleName();

    private SenderUploadListener mSenderUploadListener;
    private boolean mRecovering = false;
    private boolean mNonCancellable = false;
    private boolean mMoveOnTranscription = false;

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
            final MessagesResponse response = peppermintApi.sendMessage(getId().toString(),
                    message.getRecordingParameter().getTranscriptionUrl(), canonicalUrl,
                    data.getEmail(), recipientVia);
            message.setServerId(response.getMessageId());
            message.setRegistrationTimestamp(response.getCreatedTimestamp());
            try {
                GlobalManager.getInstance(getContext()).markAsPeppermint(recipient, data.getEmail());
            } catch (SQLException e) {
                getTrackerManager().logException(e);
            }
            message.addConfirmedSentRecipientId(recipient.getId());
            sentInApp = true;
        } catch(PeppermintApiRecipientNoAppException e) {
            Log.w(TAG, "Unable to send through Peppermint", e);
            message.removeConfirmedSentRecipientId(recipient.getId());
            sentInApp = false;
            try {
                GlobalManager.getInstance(getContext()).unmarkAsPeppermint(recipient);
            } catch (SQLException e1) {
                getTrackerManager().logException(e1);
            }
        }

        // immediately update message with serverId and sent recipients
        DataObjectManager.update(MessageManager.getInstance(getContext()), message);

        return sentInApp;
    }

    protected String waitTranscription() {
        final Recording recording = getMessage().getRecordingParameter();

        if(!getSenderPreferences().isAutomaticTranscription()) {
            return recording.getTranscription();
        }

        int waitCount = 20; // 20 secs tops

        if(recording.getTranscriptionConfidence() < 0) {
            Intent popTranscriptionIntent = new Intent(getContext(), RecordService.class);
            popTranscriptionIntent.setAction(RecordService.ACTION_POP_TRANSCRIPTION);
            popTranscriptionIntent.putExtra(RecordService.INTENT_DATA_TRANSCRIPTION_FILEPATH, recording.getFilePath());
            getContext().startService(popTranscriptionIntent);

            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    /* nothing to do here */
                }
                waitCount--;
            } while(waitCount > 0 && !mMoveOnTranscription);
        }

        return recording.getTranscription();
    }

    protected String getTranscription() throws IOException, NoInternetConnectionException {
        final Message message = getMessage();
        final Recording recording = message.getRecordingParameter();

        if(waitTranscription() == null) {
            final File mintFile = new File(recording.getFilePath() + ".mint");
            if (!mintFile.exists()) {
                return null;
            }

            final GoogleSpeechRecognizeClient googleSpeechRecognizeClient = new GoogleSpeechRecognizeClient(getContext(), message.getUUID().toString());
            Object[] transcriptionResults = googleSpeechRecognizeClient.getTranscriptionSync(mintFile.getAbsolutePath());
            if(transcriptionResults != null) {
                recording.setTranscription((String) transcriptionResults[0]);
                recording.setTranscriptionConfidence((float) transcriptionResults[1]);
                recording.setTranscriptionLanguage((String) transcriptionResults[2]);
            }
        }

        if(recording.getTranscription() != null) {
            // immediately update recording with transcription data
            DataObjectManager.update(RecordingManager.getInstance(), recording);

            if(mSenderUploadListener != null && !mRecovering) {
                mSenderUploadListener.onSendingUploadProgress(this, 0);
            }
        }

        return recording.getTranscription();
    }

    protected String sendPeppermintTranscription() throws Exception {
        final Message message = getMessage();
        final Recording recording = message.getRecordingParameter();

        if(recording.getTranscriptionUrl() != null) {
            return recording.getTranscriptionUrl();
        }

        if(recording.getTranscription() != null) {
            final PeppermintApi peppermintApi = getPeppermintApi();

            TranscriptionResponse response = peppermintApi.sendTranscription(getId().toString(), message.getServerCanonicalUrl(),
                    recording.getTranscriptionLanguage(), recording.getTranscriptionConfidence(), recording.getTranscription());
            recording.setTranscriptionUrl(response.getTranscriptionUrl());

            return response.getTranscriptionUrl();
        }

        return null;
    }

    public void onEventMainThread(RecorderEvent event) {
        final Recording recording = getMessage() != null ? getMessage().getRecordingParameter() : null;
        if(recording != null && recording.getFilePath().compareTo(event.getRecording().getFilePath()) == 0) {
            recording.setTranscription(event.getRecording().getTranscription());
            recording.setTranscriptionConfidence(event.getRecording().getTranscriptionConfidence());
            recording.setTranscriptionLanguage(event.getRecording().getTranscriptionLanguage());
            mMoveOnTranscription = true;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(mSenderUploadListener != null && !mRecovering) {
            mSenderUploadListener.onSendingUploadStarted(this);
        }
        RecordService.registerEventListener(this);
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
        RecordService.unregisterEventListener(this);
        if(mSenderUploadListener != null) {
            mSenderUploadListener.onSendingUploadCancelled(this);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        RecordService.unregisterEventListener(this);
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
