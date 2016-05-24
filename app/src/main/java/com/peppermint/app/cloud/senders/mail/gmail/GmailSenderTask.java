package com.peppermint.app.cloud.senders.mail.gmail;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.cloud.apis.GoogleApi;
import com.peppermint.app.cloud.apis.SparkPostApi;
import com.peppermint.app.cloud.senders.Sender;
import com.peppermint.app.cloud.senders.SenderUploadListener;
import com.peppermint.app.cloud.senders.SenderUploadTask;
import com.peppermint.app.cloud.senders.exceptions.NoInternetConnectionException;
import com.peppermint.app.cloud.senders.exceptions.NoPlayServicesException;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.utils.DateContainer;

import java.io.File;
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

    public GmailSenderTask(GmailSenderTask uploadTask) {
        super(uploadTask);
    }

    public GmailSenderTask(Sender sender, Message message, SenderUploadListener senderUploadListener) {
        super(sender, message, senderUploadListener);
    }

    @Override
    public void execute() throws Throwable {

        AuthenticationData data = setupPeppermintAuthentication();
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
        File file = getMessage().getRecordingParameter().getFile();

        // build the email body
        final SparkPostApi sparkPostApi = getSparkPostApi();
        String bodyHtml = sparkPostApi.buildEmailFromTemplate(getContext(), url, canonicalUrl, fullName, data.getEmail(), SparkPostApi.TYPE_HTML, true, getId().toString());
        String bodyPlain = sparkPostApi.buildEmailFromTemplate(getContext(), url, canonicalUrl, fullName, data.getEmail(), SparkPostApi.TYPE_TEXT, false, getId().toString());

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

            try {
                GoogleApi.DraftResponse response = googleApi.createGmailDraft(getId().toString(), getMessage().getEmailSubject(),
                        bodyPlain, bodyHtml, recipientEmails, getMessage().getRecordingParameter().getContentType(),
                        emailDate, file);
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
