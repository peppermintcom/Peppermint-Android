package com.peppermint.app.senders;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

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

import de.greenrobot.event.EventBus;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for Emails using the GMail API.
 */
public class GmailSender extends Sender {

    private static final int REQUEST_AUTHORIZATION = 1001;
    private static final int REQUEST_ACCOUNT_PICKER = 1002;

    public static class PreferredAccountNotSetException extends RuntimeException {
        public PreferredAccountNotSetException(String detailMessage) {
            super(detailMessage);
        }
    }

    private static final String TAG = GmailSender.class.getSimpleName();

    private static final String[] SCOPES = { GmailScopes.GMAIL_COMPOSE };
    public static final String PREF_ACCOUNT_NAME_KEY = "prefAccountName";
    public static final String PREF_SKIP_IF_PERMISSION_REQ_KEY = "prefSkipIfPermissionRequired";

    public static final String PARAM_DISPLAY_NAME = "paramDisplayName";

    protected Gmail mService;
    protected GoogleAccountCredential mCredential;

    private final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    private final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    public GmailSender(Context context, EventBus eventBus, ThreadPoolExecutor executor) {
        super(context, eventBus, executor);
    }

    @Override
    protected boolean recoverAndTryAgain(SenderTask task, Throwable e) {
        if(e instanceof UserRecoverableAuthIOException) {
            if(getSkipIfPermissionRequired()) {
                return false;
            }
            startActivityForResult(task.getUUID(), REQUEST_AUTHORIZATION, ((UserRecoverableAuthIOException) e).getIntent());
            return true;
        }

        if(e instanceof GmailSender.PreferredAccountNotSetException) {
            Account[] accounts = AccountManager.get(mContext).getAccountsByType("com.google");
            if(accounts.length <= 0) {
                return false;
            }
            if(accounts.length <= 1) {
                mCredential.setSelectedAccountName(accounts[0].name);
                setPreferredAccountName(accounts[0].name);
                task.doRecover();
                return true;
            }
            startActivityForResult(task.getUUID(), REQUEST_ACCOUNT_PICKER, mCredential.newChooseAccountIntent());
            return true;
        }

        if(e instanceof GoogleJsonResponseException) {
            try {
                GoogleAuthUtil.invalidateToken(mContext, mCredential.getToken());
                return true;
            } catch (Exception ex) {
                Log.w(TAG, ex);
                return false;
            }
        }

        return false;
    }

    @Override
    protected void onActivityResult(SenderTask recoveredTask, int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    mCredential.setSelectedAccountName(accountName);
                    setPreferredAccountName(accountName);
                }
            }

            if(getPreferredAccountName() == null) {
                recoveredTask.doNotRecover();
            } else {
                recoveredTask.doRecover();
            }
            return;
        }

        if(requestCode == REQUEST_AUTHORIZATION){
            if(resultCode == Activity.RESULT_OK) {
                recoveredTask.doRecover();
                return;
            } else if(resultCode == Activity.RESULT_CANCELED) {
                setSkipIfPermissionRequired(true);
                Toast.makeText(mContext, R.string.msg_cancelled_gmail_api, Toast.LENGTH_LONG).show();
            }
        }

        recoveredTask.doNotRecover();
    }

    @Override
    public void init(Map<String, Object> parameters) {
        super.init(parameters);

        if(!mParams.containsKey(PARAM_DISPLAY_NAME)) {
            Cursor cursor = mContext.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
            if(cursor != null) {
                if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                    String userName = cursor.getString(cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
                    if (userName != null) {
                        mParams.put(PARAM_DISPLAY_NAME, userName);
                    }
                }
                cursor.close();
            }
        }

        this.mCredential = GoogleAccountCredential.usingOAuth2(
                mContext, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(getPreferredAccountName());

        this.mService = new Gmail.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName(mContext.getString(R.string.app_name))
                .build();
    }

    @Override
    public void send(String to, String subject, String bodyText, String filePath, String contentType) throws Throwable {
        File file = validateFile(filePath);

        if(!Utils.isInternetAvailable(mContext)) {
            throw new RuntimeException("Internet connection not available!");
        }

        if(getPreferredAccountName() == null) {
            throw new PreferredAccountNotSetException("Preferred account is not set!");
        }

        MimeMessage email = createEmailWithAttachment(to, getPreferredAccountName(), mParams.get(PARAM_DISPLAY_NAME).toString(), subject, bodyText, file.getParent(), file.getName(), contentType);
        Message message = createMessageWithEmail(email);
        mService.users().messages().send("me", message).execute();

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, R.string.msg_message_sent, Toast.LENGTH_SHORT).show();
            }
        });
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
    protected static MimeMessage createEmailWithAttachment(String to, String from, String fromName, String subject,
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
    protected static Message createMessageWithEmail(MimeMessage email)
            throws MessagingException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public void setPreferredAccountName(String accountName) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(PREF_ACCOUNT_NAME_KEY, accountName);
        editor.commit();
    }

    public String getPreferredAccountName() {
        return mSettings.getString(PREF_ACCOUNT_NAME_KEY, null);
    }

    public void setSkipIfPermissionRequired(boolean doSkip) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(PREF_SKIP_IF_PERMISSION_REQ_KEY, doSkip);
        editor.commit();
    }

    public boolean getSkipIfPermissionRequired() {
        return mSettings.getBoolean(PREF_SKIP_IF_PERMISSION_REQ_KEY, false);
    }

}
