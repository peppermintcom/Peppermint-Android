package com.peppermint.app.cloud.senders.mail.gmail;

import com.peppermint.app.cloud.senders.Sender;
import com.peppermint.app.cloud.senders.SenderObject;
import com.peppermint.app.cloud.senders.SenderUploadListener;
import com.peppermint.app.cloud.senders.SenderUploadTask;
import com.peppermint.app.data.Message;

import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Sender that uses the GMail API to send audio messages through email.
 */
public class GmailSender extends Sender {

    public GmailSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
    }

    @Override
    public SenderUploadTask newTask(Message message, UUID enforcedId) {
        SenderUploadTask task = new GmailSenderTask(this, message, getSenderUploadListener());
        if(enforcedId != null) {
            task.getIdentity().setId(enforcedId);
        }
        return task;
    }
}
