package com.peppermint.app.sending.mail.gmail;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.model.Draft;
import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.data.Message;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.sending.api.exceptions.PeppermintApiRecipientNoAppException;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.sending.exceptions.TryAgainException;
import com.peppermint.app.sending.mail.MailUtils;
import com.peppermint.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * SenderTask for emails using the Gmail API.
 */
public class GmailSenderTask extends SenderUploadTask {

    public GmailSenderTask(GmailSenderTask uploadTask) {
        super(uploadTask);
    }

    public GmailSenderTask(Sender sender, Message message, SenderUploadListener senderUploadListener) {
        super(sender, message, senderUploadListener);
    }

    private Draft getDraftFromResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        if(!jsonResponse.isNull("error")) {
            throw new RuntimeException("Google API error - " + response);
        }

        JSONObject jsonMessage = jsonResponse.getJSONObject("message");
        com.google.api.services.gmail.model.Message responseMessage = new com.google.api.services.gmail.model.Message();
        responseMessage.setId(jsonMessage.getString("id"));
        responseMessage.setThreadId(jsonMessage.getString("threadId"));
        JSONArray jsonLabels = jsonMessage.getJSONArray("labelIds");
        List<String> labels = new ArrayList<>();
        for(int i=0; i<jsonLabels.length(); i++) {
            labels.add(jsonLabels.getString(i));
        }
        responseMessage.setLabelIds(labels);
        Draft responseDraft = new Draft();
        responseDraft.setId(jsonResponse.getString("id"));
        responseDraft.setMessage(responseMessage);

        return responseDraft;
    }

    @Override
    public void execute() throws Throwable {

        AuthenticationData data = setupPeppermintAuthentication();
        uploadPeppermintMessage();
        String url = getMessage().getServerShortUrl();

        long now = android.os.SystemClock.uptimeMillis();
        String fullName = getSenderPreferences().getFullName();

        GoogleApi googleApi = getGoogleApi(data.getEmail());
        googleApi.setDisplayName(fullName);
        File file = getMessage().getRecording().getFile();

        // build the email body
        getMessage().setEmailBody(MailUtils.buildEmailFromTemplate(getContext(), R.raw.email_template, url,
                getMessage().getRecording().getDurationMillis(),
                getMessage().getRecording().getContentType(),
                fullName, data.getEmail(), true));

        try {
            Draft draft = null;

            Date emailDate = new Date();
            try {
                emailDate = Utils.parseTimestamp(getMessage().getRegistrationTimestamp());
            } catch(ParseException e) {
                getTrackerManager().logException(e);
            }

            try {
                GoogleApi.DraftResponse response = googleApi.createGmailDraft(getMessage().getEmailSubject(), getMessage().getEmailBody(), getMessage().getRecipient().getVia(),
                        getMessage().getRecording().getContentType(), emailDate, file);
                draft = (Draft) response.getBody();

                getTrackerManager().log("Gmail # Created Draft at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");
            } catch (InterruptedIOException e) {
                if(!isCancelled()) {
                    throw e;
                }
            }

            if(!isCancelled()) {
                try {
                    getPeppermintApi().sendMessage(null, url, data.getEmail(), getMessage().getRecipient().getVia());
                } catch(PeppermintApiRecipientNoAppException e) {
                    getTrackerManager().log("Unable to send through Peppermint", e);
                } catch(Throwable e) {
                    googleApi.deleteGmailDraft(draft);
                    throw e;
                }
            }

            if(!isCancelled()) {
                try {
                    // send the draft
                    googleApi.sendGmailDraft(draft);
                    getTrackerManager().log("Gmail # Sent Draft at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");
                } catch (InterruptedIOException e) {
                    if(!isCancelled()) {
                        // try to delete the draft if something goes wrong
                        googleApi.deleteGmailDraft(draft);
                        throw e;
                    }
                }
            }

            if(isCancelled()) {
                // just delete the draft if cancelled
                googleApi.deleteGmailDraft(draft);
                getTrackerManager().log("Gmail # Delete draft after cancel.");
            }

            getTrackerManager().log("Gmail # Finished at " + (android.os.SystemClock.uptimeMillis() - now) + " ms");

        } catch(GoogleJsonResponseException e) {
            // when the Google JSON payload contains an error message
            throw e;
        } catch(GooglePlayServicesAvailabilityIOException|GooglePlayServicesAvailabilityException e) {
            // no google play services installed on device
            throw new TryAgainException(getSender().getContext().getString(R.string.sender_msg_no_gplay), e);
        } catch(UserRecoverableAuthIOException|UserRecoverableAuthException e) {
            // recover by requesting permission to access the api
            throw e;
        } catch(IOException e) {
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.sender_msg_no_internet), e);
        }
    }

}
