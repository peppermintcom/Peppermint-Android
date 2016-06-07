package com.peppermint.app.services.messenger.handlers.gmail;

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
    protected String mSenderMail, mSenderName, mSubject, mBodyPlain, mBodyHtml, mContentType;
    protected String[] mRecipientMail;
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
        this.mBodyPlain = req.mBodyPlain;
        this.mBodyHtml = req.mBodyHtml;
        this.mContentType = req.mContentType;
        this.mTimestamp = req.mTimestamp;
    }

    public GmailAttachmentRequest(File file, String senderMail, String senderName,
                                  String subject, String bodyPlain, String bodyHtml,
                                  String contentType, Date timestamp, String... recipientMail) {

        super(ENDPOINT, HttpRequest.METHOD_POST, false);

        removeHeaderParam("Accept");
        removeUrlParam("_ts");

        this.mFile = file;
        this.mSenderMail = senderMail;
        this.mSenderName = senderName;
        this.mRecipientMail = recipientMail;
        this.mSubject = subject;
        this.mBodyPlain = bodyPlain;
        this.mBodyHtml = bodyHtml;
        this.mContentType = contentType;
        this.mTimestamp = timestamp;
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param to the email address of the receiver.
     * @param from the email address of the sender, the mailbox account.
     * @param subject the subject of the email.
     * @param bodyPlainText the body play text of the email.
     * @param bodyHtmlText the body HTML text of the email.
     * @param fileDir the path to the directory containing attachment.
     * @param filename the name of file to be attached.
     * @return the MimeMessage to be used to send email.
     * @throws MessagingException
     */
    private static MimeMessage createEmailWithAttachment(String from, String fromName, String subject,
                                                         String bodyPlainText, String bodyHtmlText, String fileDir, String filename, String contentType, Date dateSent, String... to) throws IOException, MessagingException {
        final Properties props = new Properties();
        final Session session = Session.getDefaultInstance(props, null);

        final MimeMessage email = new MimeMessage(session);

        final InternetAddress fAddress = new InternetAddress(from, fromName);
        email.setFrom(fAddress);

        for(String toString : to) {
            final InternetAddress tAddress = new InternetAddress(toString);
            email.addRecipient(javax.mail.Message.RecipientType.TO, tAddress);
        }

        email.setSubject(subject);
        email.setSentDate(dateSent);

        final Multipart multipartMixed = new MimeMultipart("mixed");

            final MimeBodyPart mixedPart = new MimeBodyPart();
            Multipart multipart = new MimeMultipart("alternative");
            mixedPart.setContent(multipart);

            // Plain text message
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(bodyPlainText, "text/plain");
            mimeBodyPart.setHeader("Content-Type", "text/plain; charset=UTF-8");
            multipart.addBodyPart(mimeBodyPart);

            // HTML message
            mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(bodyHtmlText, "text/html");
            mimeBodyPart.setHeader("Content-Type", "text/html; charset=UTF-8");
            multipart.addBodyPart(mimeBodyPart);

            multipartMixed.addBodyPart(mixedPart);

        if(fileDir != null && filename != null) {
            // Attachment
            mimeBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(fileDir + "/" + filename);
            mimeBodyPart.setDataHandler(new DataHandler(source));
            mimeBodyPart.setFileName(filename);
            mimeBodyPart.setHeader("Content-Type", contentType + "; name=\"" + filename + "\"");
            mimeBodyPart.setHeader("Content-Transfer-Encoding", "binary");
            multipartMixed.addBodyPart(mimeBodyPart);
        }

        email.setContent(multipartMixed);

        return email;
    }

    @Override
    protected long getContentLength() {
        return -1;
    }

    @Override
    public String getBody() {
        // must return something != null in order to output the body
        return mSubject; /*mFile.toString();*/
    }

    public void writeBody(OutputStream outStream) throws IOException {

        MimeMessage email;
        try {
            email = createEmailWithAttachment(mSenderMail, mSenderName,
                    mSubject, mBodyPlain, mBodyHtml,
                    mFile != null ? mFile.getParent() : null,
                    mFile != null ? mFile.getName() : null,
                    mContentType,
                    mTimestamp, mRecipientMail);
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
        out.writeString(mFile != null ? mFile.getAbsolutePath() : null);
        out.writeString(mSenderMail);
        out.writeString(mSenderName);
        out.writeInt(mRecipientMail == null ? -1 : mRecipientMail.length);
        out.writeArray(mRecipientMail);
        out.writeString(mSubject);
        out.writeString(mBodyPlain);
        out.writeString(mBodyHtml);
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
        String filePath = in.readString();
        if(filePath != null) {
            mFile = new File(filePath);
        }
        mSenderMail = in.readString();
        mSenderName = in.readString();
        int size = in.readInt();
        if(size >= 0) {
            mRecipientMail = new String[size];
            in.readStringArray(mRecipientMail);
        }
        mSubject = in.readString();
        mBodyPlain = in.readString();
        mBodyHtml = in.readString();
        mContentType = in.readString();
        mTimestamp = (Date) in.readSerializable();
    }
}

