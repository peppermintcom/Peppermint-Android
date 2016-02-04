package com.peppermint.app.sending.mail.gmail;

import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderUploadListener;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Error handler for the {@link GmailSender}.
 */
public class GmailSenderErrorHandler extends SenderErrorHandler {

    public GmailSenderErrorHandler(Sender sender, SenderUploadListener senderUploadListener) {
        super(sender, senderUploadListener);
    }

}
