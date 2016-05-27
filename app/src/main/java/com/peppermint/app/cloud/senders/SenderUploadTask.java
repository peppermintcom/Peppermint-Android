package com.peppermint.app.cloud.senders;

import android.content.Intent;

import com.peppermint.app.RecordService;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.cloud.apis.PeppermintApi;
import com.peppermint.app.cloud.apis.data.MessagesResponse;
import com.peppermint.app.cloud.apis.data.TranscriptionResponse;
import com.peppermint.app.cloud.apis.data.UploadsResponse;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiRecipientNoAppException;
import com.peppermint.app.cloud.apis.speech.GoogleSpeechRecognizeClient;
import com.peppermint.app.cloud.senders.exceptions.NoInternetConnectionException;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.GlobalManager;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.MessageManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.Recording;
import com.peppermint.app.data.RecordingManager;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.RecorderEvent;

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

    protected String waitTranscription() {
        final Recording recording = getMessage().getRecordingParameter();

        if(!getSenderPreferences().isAutomaticTranscription()) {
            return recording.getTranscription();
        }

        int waitCount = 20; // 30 secs tops

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

    protected Object[] getTranscription() throws IOException, NoInternetConnectionException {
        final Message message = getMessage();
        final Recording recording = message.getRecordingParameter();
        final File mintFile = new File(recording.getFilePath() + ".mint");

        if(!mintFile.exists()) {
            return null;
        }

        final GoogleSpeechRecognizeClient googleSpeechRecognizeClient = new GoogleSpeechRecognizeClient(getContext(), message.getUUID().toString());
        return googleSpeechRecognizeClient.getTranscriptionSync(mintFile.getAbsolutePath());
    }

    protected String sendPeppermintTranscription() throws Exception {
        final Message message = getMessage();
        final Recording recording = message.getRecordingParameter();

        if(recording.getTranscriptionUrl() != null) {
            return recording.getTranscriptionUrl();
        }

        if(waitTranscription() == null) {
            Object[] transcriptionResults = getTranscription();
            recording.setTranscription((String) transcriptionResults[0]);
            recording.setTranscriptionConfidence((float) transcriptionResults[1]);
            recording.setTranscriptionLanguage((String) transcriptionResults[2]);
        }

        if(recording.getTranscription() != null) {
            final PeppermintApi peppermintApi = getPeppermintApi();

            TranscriptionResponse response = peppermintApi.sendTranscription(getId().toString(), message.getServerCanonicalUrl(),
                    recording.getTranscriptionLanguage(), recording.getTranscriptionConfidence(), recording.getTranscription());
            recording.setTranscriptionUrl(response.getTranscriptionUrl());

            // immediately update recording with transcription data
            final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getContext());
            databaseHelper.lock();
            try {
                RecordingManager.update(databaseHelper.getWritableDatabase(), recording);
            } catch (SQLException e) {
                getTrackerManager().logException(e);
            }
            databaseHelper.unlock();

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
        PeppermintEventBus.registerAudio(this);
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
        PeppermintEventBus.unregisterAudio(this);
        if(mSenderUploadListener != null) {
            mSenderUploadListener.onSendingUploadCancelled(this);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        PeppermintEventBus.unregisterAudio(this);
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
