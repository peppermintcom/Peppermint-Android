package com.peppermint.app.cloud.senders.mail.gmail;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Base64OutputStream;

import com.peppermint.app.cloud.rest.HttpRequest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
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
 * Created by Nuno Luz on 02-07-2015.
 *
 * <p>
 *     Represents an HttpRequest that sends a file in the request body.
 * </p>
 */
public class GmailAttachmentRequest extends HttpRequest implements Parcelable {

    // the Gmail API endpoint for creating a draft with an attached file
    private static final String ENDPOINT = "https://www.googleapis.com/gmail/v1/users/me/drafts";

    protected File mFile;
    protected String mSenderMail, mSenderName, mRecipientMail, mSubject, mBody, mContentType;
    protected Date mTimestamp;

    /**
     * Copy constructor
     * @param req the {@link GmailAttachmentRequest to be copied}
     */
    public GmailAttachmentRequest(GmailAttachmentRequest req) {
        super(req);
        this.mFile = req.mFile;
        this.mSenderMail = req.mSenderMail;
        this.mSenderName = req.mSenderName;
        this.mRecipientMail = req.mRecipientMail;
        this.mSubject = req.mSubject;
        this.mBody = req.mBody;
        this.mContentType = req.mContentType;
        this.mTimestamp = req.mTimestamp;
    }

    public GmailAttachmentRequest(File file, String senderMail, String senderName, String recipientMail,
                                  String subject, String body, String contentType, Date timestamp) {

        super(ENDPOINT, HttpRequest.METHOD_POST, false);

        removeHeaderParam("Accept");
        removeUrlParam("_ts");

        this.mFile = file;
        this.mSenderMail = senderMail;
        this.mSenderName = senderName;
        this.mRecipientMail = recipientMail;
        this.mSubject = subject;
        this.mBody = body;
        this.mContentType = contentType;
        this.mTimestamp = timestamp;
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
        mimeBodyPart.setHeader("Content-Transfer-Encoding", "binary");

        multipart.addBodyPart(mimeBodyPart);
        email.setContent(multipart);
        return email;
    }

    @Override
    protected long getContentLength() {
        return -1;
    }

    @Override
    public String getBody() {
        // must return something != null in order to output the body
        return mFile.toString();
    }

    public void writeBody(OutputStream outStream) throws IOException {

        MimeMessage email = null;
        try {
            email = createEmailWithAttachment(mRecipientMail,
                    mSenderMail, mSenderName,
                    mSubject, mBody,
                    mFile.getParent(), mFile.getName(),
                    mContentType,
                    mTimestamp);
        } catch (MessagingException e) {
            throw new IOException(e);
        }

        // this really improves performance since we skip all the Gson parsing
        // of the Gmail API implementation
        outStream.write("{\"message\":{\"raw\":\"".getBytes("UTF-8"));
        outStream.flush();

        // especially when we write the file directly to the outputstream instead of
        // generating a full string beforehand
        try {
            Base64OutputStream base64OutStream = new Base64OutputStream(outStream, Base64.NO_WRAP|Base64.NO_PADDING|Base64.URL_SAFE);
            email.writeTo(base64OutStream);
            base64OutStream.flush();
        } catch (MessagingException e) {
            throw new IOException(e);
        }

        if(!isCancelled()) {
            outStream.write("\"}}".getBytes("UTF-8"));
            outStream.flush();
        }
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mFile.getAbsolutePath());
        out.writeString(mSenderMail);
        out.writeString(mSenderName);
        out.writeString(mRecipientMail);
        out.writeString(mSubject);
        out.writeString(mBody);
        out.writeString(mContentType);
        out.writeSerializable(mTimestamp);
    }

    public static final Creator<GmailAttachmentRequest> CREATOR = new Creator<GmailAttachmentRequest>() {
        public GmailAttachmentRequest createFromParcel(Parcel in) { return new GmailAttachmentRequest(in); }
        public GmailAttachmentRequest[] newArray(int size) {
            return new GmailAttachmentRequest[size];
        }
    };

    protected GmailAttachmentRequest(Parcel in) {
        super(in);
        mFile = new File(in.readString());
        mSenderMail = in.readString();
        mSenderName = in.readString();
        mRecipientMail = in.readString();
        mSubject = in.readString();
        mBody = in.readString();
        mContentType = in.readString();
        mTimestamp = (Date) in.readSerializable();
    }
}

