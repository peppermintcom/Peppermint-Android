package com.peppermint.app.services.messenger.handlers.gmail;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.peppermint.app.R;
import com.peppermint.app.services.authenticator.AuthenticationData;
import com.peppermint.app.cloud.apis.google.GoogleApi;
import com.peppermint.app.cloud.apis.sparkpost.SparkPostApi;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.services.messenger.handlers.NoInternetConnectionException;
import com.peppermint.app.services.messenger.handlers.NoPlayServicesException;
import com.peppermint.app.services.messenger.handlers.Sender;
import com.peppermint.app.services.messenger.handlers.SenderUploadListener;
import com.peppermint.app.services.messenger.handlers.SenderUploadTask;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * SenderUploadTask for emails using the Gmail API.
 */
public class GmailSenderTask extends SenderUploadTask {

    private static final int MAX_SUBJECT_TRANSCRIPTION_CHARS = 50;

    public GmailSenderTask(GmailSenderTask uploadTask) {
        super(uploadTask);
    }

    public GmailSenderTask(Sender sender, Message message, SenderUploadListener senderUploadListener) {
        super(sender, message, senderUploadListener);
    }

    @Override
    public void execute() throws Throwable {

        AuthenticationData data = setupPeppermintAuthentication();
        getTranscription();

        uploadPeppermintMessage();
        sendPeppermintTranscription();
        if(sendPeppermintMessage()) {
            return;
        }

        if(isCancelled()) { return; }

        String url = getMessage().getServerShortUrl();
        String canonicalUrl = getMessage().getServerCanonicalUrl();

        long now = android.os.SystemClock.uptimeMillis();
        String fullName = getSenderPreferences().getFullName();

        GoogleApi googleApi = getGoogleApi(data.getEmail());
        googleApi.setDisplayName(fullName);

        final String transcription = getMessage().getRecordingParameter().getTranscription();

        // build the email body
        final SparkPostApi sparkPostApi = getSparkPostApi();
        String bodyHtml = sparkPostApi.buildEmailFromTemplate(getContext(), url, canonicalUrl, fullName, data.getEmail(), SparkPostApi.TYPE_HTML, true, getId().toString(), transcription);
        String bodyPlain = sparkPostApi.buildEmailFromTemplate(getContext(), url, canonicalUrl, fullName, data.getEmail(), SparkPostApi.TYPE_TEXT, false, getId().toString(), transcription);

        if(isCancelled()) { return; }

        getMessage().setEmailBody(bodyPlain);

        try {
            // send gmail
            String draftId = null;
            Date emailDate = new Date();
            try {
                emailDate = DateContainer.parseUTCTimestamp(getMessage().getRegistrationTimestamp()).getTime();
            } catch(ParseException e) {
                getTrackerManager().logException(e);
            }

            final List<Recipient> chatRecipientList = getMessage().getChatParameter().getRecipientList();
            final int chatRecipientListSize = chatRecipientList.size();
            String[] recipientEmails = new String[chatRecipientListSize];
            for(int i=0; i<chatRecipientListSize; i++) {
                recipientEmails[i] = chatRecipientList.get(i).getVia();
            }

            String subject = getMessage().getEmailSubject();
            if(transcription != null && transcription.length() > 0) {
                String simplifiedTranscription = Utils.getSimplifiedString(transcription, MAX_SUBJECT_TRANSCRIPTION_CHARS);
                if(simplifiedTranscription != null) {
                    subject = String.format(getContext().getString(R.string.sender_transcription_mail_subject), simplifiedTranscription);
                }
            }

            try {
                GoogleApi.DraftResponse response = googleApi.createGmailDraft(getId().toString(), subject,
                        bodyPlain, bodyHtml, recipientEmails, getMessage().getRecordingParameter().getContentType(),
                        emailDate, null);
                draftId = (String) response.getBody();

                getTrackerManager().log("Gmail # Created Draft at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");
            } catch (InterruptedIOException e) {
                if(!isCancelled()) {
                    throw e;
                }
            }

            if(!isCancelled()) {
                try {
                    // send the draft
                    googleApi.sendGmailDraft(getId().toString(), draftId);
                    getTrackerManager().log("Gmail # Sent Draft at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");
                } catch (InterruptedIOException e) {
                    // try to delete the draft if something goes wrong
                    googleApi.deleteGmailDraft(getId().toString(), draftId);
                    throw e;
                }
            }

            if(isCancelled() && draftId != null) {
                // just delete the draft if cancelled
                googleApi.deleteGmailDraft(getId().toString(), draftId);
                getTrackerManager().log("Gmail # Delete draft after cancel.");
            }

            getTrackerManager().log("Gmail # Finished at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");

        } catch(GoogleJsonResponseException e) {
            // when the Google JSON payload contains an error message
            throw e;
        } catch(GooglePlayServicesAvailabilityIOException|GooglePlayServicesAvailabilityException e) {
            // no google play services installed on device
            throw new NoPlayServicesException();
        } catch(UserRecoverableAuthIOException|UserRecoverableAuthException e) {
            // recover by requesting permission to access the api
            throw e;
        } catch(IOException e) {
            throw new NoInternetConnectionException(e);
        }
    }

}
