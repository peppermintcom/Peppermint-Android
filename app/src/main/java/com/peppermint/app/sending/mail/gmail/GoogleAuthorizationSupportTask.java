package com.peppermint.app.sending.mail.gmail;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.mail.MailPreferredAccountNotSetException;
import com.peppermint.app.sending.mail.MailSenderPreferences;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     The abstract AsyncTask implementation executed by a {@link Sender}.<br />
 *     Each {@link Sender} must have their own {@link GoogleAuthorizationSupportTask} concrete implementations.
 * </p>
 * <p>
 *     As with {@link Sender}s, {@link GoogleAuthorizationSupportTask}s are configured through Parameters and Preferences.
 *     Preferences use the native Android Shared Preferences mechanism, so that data is saved
 *     across different executions of the app. Parameters are part of a key-value map passed to
 *     the sending task instance (each implementation may have its own parameters).
 * </p>
 */
public class GoogleAuthorizationSupportTask extends SenderSupportTask implements Cloneable {

    public GoogleAuthorizationSupportTask(GoogleAuthorizationSupportTask supportTask) {
        super(supportTask);
    }

    public GoogleAuthorizationSupportTask(Sender sender, SendingRequest sendingRequest, SenderSupportListener senderSupportListener) {
        super(sender, sendingRequest, senderSupportListener);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();
        String preferredAccountName = ((MailSenderPreferences) getSenderPreferences()).getPreferredAccountName();
        if(preferredAccountName == null) {
            throw new MailPreferredAccountNotSetException();
        }
        getGoogleApi().refreshAccessToken();
    }
}
