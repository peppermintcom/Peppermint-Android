package com.peppermint.app.sending.inapp;

import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderUploadListener;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Error handler for the {@link InAppSender}.
 */
public class InAppSenderErrorHandler extends SenderErrorHandler {

    public InAppSenderErrorHandler(Sender sender, SenderUploadListener senderUploadListener) {
        super(sender, senderUploadListener);
    }
}
