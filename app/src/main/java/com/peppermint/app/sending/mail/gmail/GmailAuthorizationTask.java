package com.peppermint.app.sending.mail.gmail;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.peppermint.app.R;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderAuthorizationTask;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.exceptions.NoInternetConnectionException;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;
import com.peppermint.app.sending.mail.MailSenderPreferences;
import com.peppermint.app.utils.Utils;

import java.util.Map;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Authorization task for the Gmail API.
 */
public class GmailAuthorizationTask extends SenderAuthorizationTask {

    public GmailAuthorizationTask(Sender sender, SenderListener listener) {
        super(sender, listener);
    }

    public GmailAuthorizationTask(Sender sender, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, listener, parameters, preferences);
    }

    public GmailAuthorizationTask(SenderTask sendingTask) {
        super(sendingTask);
    }

    @Override
    protected void send() throws Throwable {
        if(!Utils.isInternetAvailable(getContext()) || !Utils.isInternetActive(getContext())) {
            throw new NoInternetConnectionException(getContext().getString(R.string.msg_no_internet));
        }

        String preferredAccountName = ((MailSenderPreferences) getSenderPreferences()).getPreferredAccountName();
        if(preferredAccountName == null) {
            throw new MailPreferredAccountNotSetException();
        }

        // just get the access token
        GoogleAuthUtil.clearToken(getContext(), ((GoogleAccountCredential) getParameter(GmailSender.PARAM_GMAIL_CREDENTIAL)).getToken());
    }

}
