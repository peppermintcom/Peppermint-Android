package com.peppermint.app.sending.gmail;

import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingTask;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
 * SenderManager for Emails using the GMail API.
 */
public class GmailSendingTask extends SendingTask {

    private static final String TAG = GmailSendingTask.class.getSimpleName();

    private static final String CONTENT_TYPE_AUDIO = "audio/mpeg";
    private static final String CONTENT_TYPE_VIDEO = "video/mpeg";

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
            throw new NoInternetConnectionException();
        }

        String preferredAccountName = ((GmailSenderPreferences) getSenderPreferences()).getPreferredAccountName();
        if(preferredAccountName == null) {
            throw new GmailPreferredAccountNotSetException();
        }

        try {
            MimeMessage email = createEmailWithAttachment(getSendingRequest().getRecipient().getVia(),
                    preferredAccountName, (String) getParameter(GmailSender.PARAM_DISPLAY_NAME),
                    getSendingRequest().getSubject(), getSendingRequest().getBody(),
                    file.getParent(), file.getName(),
                    (getSendingRequest().getRecording().hasVideo() ? CONTENT_TYPE_VIDEO : CONTENT_TYPE_AUDIO));
            Message message = createMessageWithEmail(email);
            ((Gmail) getParameter(GmailSender.PARAM_GMAIL_SERVICE)).users().messages().send("me", message).execute();
        } catch(IOException e) {
            throw new NoInternetConnectionException(e);
        }
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param to Email address of the receiver.
     * @param from Email address of the sender, the mailbox account.
     * @param subject Subject of the email.
     * @param bodyText Body text of the email.
     * @param fileDir Path to the directory containing attachment.
     * @param filename Name of file to be attached.
     * @return MimeMessage to be used to send email.
     * @throws MessagingException
     */
    private static MimeMessage createEmailWithAttachment(String to, String from, String fromName, String subject,
                                                        String bodyText, String fileDir, String filename, String contentType) throws IOException, MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        InternetAddress tAddress = new InternetAddress(to);
        InternetAddress fAddress = new InternetAddress(from, fromName);

        email.setFrom(fAddress);
        email.addRecipient(javax.mail.Message.RecipientType.TO, tAddress);
        email.setSubject(subject);

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
     * Create a Message from an email
     *
     * @param email Email to be set to raw of message
     * @return Message containing base64url encoded email.
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