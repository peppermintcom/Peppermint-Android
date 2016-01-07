package com.peppermint.app.sending.server;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.rest.HttpRequest;
import com.peppermint.app.rest.HttpRequestFileData;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * SenderTask for uploading files to the Peppermint backend.
 */
public class ServerSenderTask extends SenderTask {

    public static final String PARAM_SHORT_URL = "ServerSender_ShortMessageUrl";
    public static final String PARAM_CANONICAL_URL = "ServerSender_CanonicalMessageUrl";

    public ServerSenderTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        super(sender, sendingRequest, listener);
    }

    public ServerSenderTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, sendingRequest, listener, parameters, preferences);
    }

    public ServerSenderTask(ServerSenderTask sendingTask) {
        super(sendingTask);
    }

    @Override
    public void send() throws Throwable {
        getSendingRequest().getRecording().getValidatedFile();

        if(!Utils.isInternetAvailable(getSender().getContext()) || !Utils.isInternetActive(getSender().getContext())) {
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.msg_no_internet));
        }

        ServerClientManager manager = (ServerClientManager) getParameter(ServerSender.PARAM_MANAGER);
        if(manager.getAccessToken() == null) {
            throw new InvalidAccessTokenException();
        }

        PepperMintPreferences globalPrefs = new PepperMintPreferences(getContext());
        String senderName = globalPrefs.getFullName();
        String senderEmail = globalPrefs.getGmailPreferences().getPreferredAccountName();

        // UPLOADS ENDPOINT INVOCATION
        // get AWS signed URL to upload the file
        executeHttpRequest(ServerClientManager.UPLOADS_ENDPOINT, HttpRequest.METHOD_POST, "{\"content_type\":\"" + getSendingRequest().getRecording().getContentType() + "\", \"sender_name\":\"" + senderName + "\", \"sender_email\":\"" + senderEmail + "\"}",
                null, "Bearer " + manager.getAccessToken());
        JSONObject response = new JSONObject(waitForHttpResponse());
        String signedUrl = response.getString("signed_url");

        // UPLOAD TO AWS HERE!
        if(!isCancelled()) {
            HttpRequestFileData request = new HttpRequestFileData(signedUrl, HttpRequest.METHOD_PUT, false, getSendingRequest().getRecording().getFile());
            request.setHeaderParam("Content-Type", getSendingRequest().getRecording().getContentType());
            request.removeHeaderParam("Accept");
            request.removeUrlParam("_ts");
            request.setUploadResultReceiver(null);  // for now, no progress
            executeHttpRequest(request);
            waitForHttpResponse();
        }

        // RECORD ENDPOINT INVOCATION
        // confirm that the upload to AWS was successfully performed
        if(!isCancelled()) {
            executeHttpRequest(ServerClientManager.RECORD_ENDPOINT, HttpRequest.METHOD_POST, "{ \"signed_url\": \"" + signedUrl + "\" }", null, "Bearer " + manager.getAccessToken());
            response = new JSONObject(waitForHttpResponse());
            String shortUrl = response.getString("short_url");
            String canonicalUrl = response.getString("canonical_url");
            getSendingRequest().setParameter(PARAM_SHORT_URL, shortUrl);
            getSendingRequest().setParameter(PARAM_CANONICAL_URL, canonicalUrl);
        }
    }
}
