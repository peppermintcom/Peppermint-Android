package com.peppermint.app.sending.mail.gmail;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.rest.HttpResponse;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;
import com.peppermint.app.sending.mail.MailSenderPreferences;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingTask;
import com.peppermint.app.sending.exceptions.ElectableForQueueingException;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.sending.server.ServerSendingTask;
import com.peppermint.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * SendingTask for emails using the Gmail API.
 */
public class GmailSendingTask extends SendingTask {

    private static final String TAG = GmailSendingTask.class.getSimpleName();

    private static final long MIN_SEND_TIME = 5000; // ms

    // FIXME the content type value should be stored in the Recording instance to avoid redundancy
    private static final String CONTENT_TYPE_AUDIO = "audio/mp4";
    private static final String CONTENT_TYPE_VIDEO = "video/mp4";

    public GmailSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        super(sender, sendingRequest, listener);
    }

    public GmailSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, sendingRequest, listener, parameters, preferences);
    }

    public GmailSendingTask(SendingTask sendingTask) {
        super(sendingTask);
    }

    private Draft getDraftFromResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        if(!jsonResponse.isNull("error")) {
            throw new RuntimeException("Google API error - " + response);
        }

        JSONObject jsonMessage = jsonResponse.getJSONObject("message");
        Message responseMessage = new Message();
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
    public void send() throws Throwable {
        File file = getSendingRequest().getRecording().getValidatedFile();

        if(!Utils.isInternetAvailable(getSender().getContext())) {
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.msg_no_internet));
        }

        String preferredAccountName = ((MailSenderPreferences) getSenderPreferences()).getPreferredAccountName();
        if(preferredAccountName == null) {
            throw new MailPreferredAccountNotSetException();
        }

        long now = android.os.SystemClock.uptimeMillis();

        String displayName = ((MailSenderPreferences) getSenderPreferences()).getDisplayName();

        // build the email body
        String url = (String) getSendingRequest().getParameter(ServerSendingTask.PARAM_SHORT_URL);
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("<p>");
        bodyBuilder.append(String.format(getSender().getContext().getString(R.string.default_mail_body_url), url,
                (getSendingRequest().getRecording().hasVideo() ? CONTENT_TYPE_VIDEO : CONTENT_TYPE_AUDIO)));
        bodyBuilder.append("</p><br />");
        bodyBuilder.append(String.format(getSender().getContext().getString(R.string.default_mail_body_reply),
                displayName == null ? "" : URLEncoder.encode(displayName, "UTF-8"),
                URLEncoder.encode(preferredAccountName, "UTF-8")));
        getSendingRequest().setBody(bodyBuilder.toString());

        try {
            Draft draft = null;

            try {
                // custom, performance optimized code to create the draft
                GmailAttachmentRequest request = new GmailAttachmentRequest(file, preferredAccountName, displayName, getSendingRequest().getRecipient().getVia(),
                        getSendingRequest().getSubject(), getSendingRequest().getBody(), (getSendingRequest().getRecording().hasVideo() ? CONTENT_TYPE_VIDEO : CONTENT_TYPE_AUDIO),
                        Utils.parseTimestamp(getSendingRequest().getRegistrationTimestamp()));
                request.setHeaderParam("Authorization", "Bearer " + ((GoogleAccountCredential) getParameter(GmailSender.PARAM_GMAIL_CREDENTIAL)).getToken());
                request.setHeaderParam("Content-Type", "application/json; charset=UTF-8");
                // perform request to the Gmail API endpoint
                executeHttpRequest(request, new HttpResponse());
                // wait for response and parse into a Gmail API Draft
                draft = getDraftFromResponse(waitForHttpResponse());

                Log.d(TAG, "Finished creating draft in " + (android.os.SystemClock.uptimeMillis() - now) + " ms");
            } catch (InterruptedIOException e) {
                Log.d(TAG, "Interrupted creating draft! Likely user cancelled.", e);
                if(!isCancelled()) {
                    Crashlytics.logException(e);
                    throw e;
                }
            }

            // make the sending process last at least 5 secs
            if(!isCancelled()) {
                long duration = android.os.SystemClock.uptimeMillis() - now;
                if (duration < MIN_SEND_TIME) {
                    try {
                        Thread.sleep(MIN_SEND_TIME - duration);
                    } catch (InterruptedException e) {
                        // do nothing here; just skip
                    }
                }
            }

            if(!isCancelled()) {
                try {
                    // send the draft
                    ((Gmail) getParameter(GmailSender.PARAM_GMAIL_SERVICE)).users().drafts().send("me", draft).execute();
                } catch (InterruptedIOException e) {
                    Log.d(TAG, "Interrupted sending draft! Likely user cancelled.", e);
                    if(!isCancelled()) {
                        Crashlytics.logException(e);
                        // try to delete the draft if something goes wrong
                        ((Gmail) getParameter(GmailSender.PARAM_GMAIL_SERVICE)).users().drafts().delete("me", draft.getId());
                        throw e;
                    }
                }
            }

            if(isCancelled()) {
                // just delete the draft if cancelled
                ((Gmail) getParameter(GmailSender.PARAM_GMAIL_SERVICE)).users().drafts().delete("me", draft.getId());
            }

            Log.d(TAG, "Finished sending email in " + (android.os.SystemClock.uptimeMillis() - now) + " ms");

        } catch(GoogleJsonResponseException e) {
            // when the Google JSON payload contains an error message
            throw e;
        } catch(GooglePlayServicesAvailabilityIOException|GooglePlayServicesAvailabilityException e) {
            // no google play services installed on device
            throw new ElectableForQueueingException(getSender().getContext().getString(R.string.msg_no_gplay), e);
        } catch(UserRecoverableAuthIOException|UserRecoverableAuthException e) {
            // recover by requesting permission to access the api
            throw e;
        } catch(IOException e) {
            Crashlytics.log(Log.ERROR, TAG, "Throwing NoInternetConnectionException: " + e.getMessage());
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.msg_no_internet), e);
        }
    }

}
