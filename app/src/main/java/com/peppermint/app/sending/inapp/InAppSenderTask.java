package com.peppermint.app.sending.inapp;

import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.data.Message;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * SenderTask for emails using the Gmail API.
 */
public class InAppSenderTask extends SenderUploadTask {

    public InAppSenderTask(InAppSenderTask uploadTask) {
        super(uploadTask);
    }

    public InAppSenderTask(Sender sender, Message message, SenderUploadListener senderUploadListener) {
        super(sender, message, senderUploadListener);
    }

    @Override
    public void execute() throws Throwable {
        AuthenticationData data = setupPeppermintAuthentication();
        uploadPeppermintMessage();
        String url = getMessage().getServerShortUrl();

        getPeppermintApi().sendMessage(null, url, data.getEmail(), getMessage().getRecipient().getVia());
    }

}
