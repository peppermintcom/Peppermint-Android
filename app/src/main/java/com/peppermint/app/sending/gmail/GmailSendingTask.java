package com.peppermint.app.sending.gmail;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingTask;
import com.peppermint.app.sending.exceptions.ElectableForQueueingException;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.sending.server.ServerSendingTask;
import com.peppermint.app.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

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

    @Override
    public void send() throws Throwable {
        File file = getSendingRequest().getRecording().getValidatedFile();

        if(!Utils.isInternetAvailable(getSender().getContext())) {
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.msg_no_internet));
        }

        String preferredAccountName = ((GmailSenderPreferences) getSenderPreferences()).getPreferredAccountName();
        if(preferredAccountName == null) {
            throw new GmailPreferredAccountNotSetException();
        }

        String displayName = ((GmailSenderPreferences) getSenderPreferences()).getDisplayName();

        String url = (String) getSendingRequest().getParameter(ServerSendingTask.PARAM_SHORT_URL);
        String body = "<p>" + String.format(getSender().getContext().getString(R.string.default_mail_body_url), url,
                (getSendingRequest().getRecording().hasVideo() ? CONTENT_TYPE_VIDEO : CONTENT_TYPE_AUDIO)) + "</p><br />" +
                    String.format(getSender().getContext().getString(R.string.default_mail_body_reply), displayName == null ? "" : URLEncoder.encode(displayName, "UTF-8"), URLEncoder.encode(preferredAccountName, "UTF-8"));
        getSendingRequest().setBody(body);

        try {
            long now = android.os.SystemClock.uptimeMillis();

            MimeMessage email = createEmailWithAttachment(getSendingRequest().getRecipient().getVia(),
                    preferredAccountName, displayName,
                    getSendingRequest().getSubject(), getSendingRequest().getBody(),
                    file.getParent(), file.getName(),
                    (getSendingRequest().getRecording().hasVideo() ? CONTENT_TYPE_VIDEO : CONTENT_TYPE_AUDIO),
                    Utils.parseTimestamp(getSendingRequest().getRegistrationTimestamp()));
            Message message = createMessageWithEmail(email);
            Draft draft = new Draft();
            draft.setMessage(message);

            try {
                draft = ((Gmail) getParameter(GmailSender.PARAM_GMAIL_SERVICE)).users().drafts().create("me", draft).execute();
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
                    ((Gmail) getParameter(GmailSender.PARAM_GMAIL_SERVICE)).users().drafts().send("me", draft).execute();
                } catch (InterruptedIOException e) {
                    Log.d(TAG, "Interrupted sending draft! Likely user cancelled.", e);
                    if(!isCancelled()) {
                        Crashlytics.logException(e);
                        ((Gmail) getParameter(GmailSender.PARAM_GMAIL_SERVICE)).users().drafts().delete("me", draft.getId());
                        throw e;
                    }
                }
            }

            if(isCancelled()) {
                ((Gmail) getParameter(GmailSender.PARAM_GMAIL_SERVICE)).users().drafts().delete("me", draft.getId());
            }

        } catch(GoogleJsonResponseException e) {
            throw e;
        } catch(GooglePlayServicesAvailabilityIOException e) {
            throw new ElectableForQueueingException(getSender().getContext().getString(R.string.msg_no_gplay), e);
        } catch(UserRecoverableAuthIOException e) {
            throw e;
        } catch(IOException e) {
            Crashlytics.log(Log.ERROR, TAG, "Throwing NoInternetConnectionException: " + e.getMessage());
            throw new NoInternetConnectionException(getSender().getContext().getString(R.string.msg_no_internet), e);
        }
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param to the email address of the receiver.
     * @param from the email address of the sender, the mailbox account.
     * @param subject the subject of the email.
     * @param bodyText the body text of the email.
     * @param fileDir the path to the directory containing attachment.
     * @param filename the name of file to be attached.
     * @return the MimeMessage to be used to send email.
     * @throws MessagingException
     */
    private static MimeMessage createEmailWithAttachment(String to, String from, String fromName, String subject,
                                                        String bodyText, String fileDir, String filename, String contentType, Date dateSent) throws IOException, MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        InternetAddress tAddress = new InternetAddress(to);
        InternetAddress fAddress = new InternetAddress(from, fromName);

        email.setFrom(fAddress);
        email.addRecipient(javax.mail.Message.RecipientType.TO, tAddress);
        email.setSubject(subject);
        email.setSentDate(dateSent);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(bodyText, "text/html");
        mimeBodyPart.setHeader("Content-Type", "text/html; charset=\"UTF-8\"");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        mimeBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(fileDir + "/" + filename);

        mimeBodyPart.setDataHandler(new DataHandler(source));
        mimeBodyPart.setFileName(filename);
        mimeBodyPart.setHeader("Content-Type", contentType + "; name=\"" + filename + "\"");
        mimeBodyPart.setHeader("Content-Transfer-Encoding", "base64");

        multipart.addBodyPart(mimeBodyPart);
        email.setContent(multipart);
        return email;
    }

    /**
     * Create a Message from an email.
     *
     * @param email the email to be set to raw of message
     * @return the Message containing base64url encoded email.
     * @throws IOException
     * @throws MessagingException
     */
    private static Message createMessageWithEmail(MimeMessage email)
            throws MessagingException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
