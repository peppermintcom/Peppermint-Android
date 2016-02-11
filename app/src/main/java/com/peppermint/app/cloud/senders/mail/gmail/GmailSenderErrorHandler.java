package com.peppermint.app.cloud.senders.mail.gmail;

import com.peppermint.app.cloud.senders.Sender;
import com.peppermint.app.cloud.senders.SenderErrorHandler;
import com.peppermint.app.cloud.senders.SenderUploadListener;

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
