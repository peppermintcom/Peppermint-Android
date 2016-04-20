package com.peppermint.app.cloud.senders.mail.nativemail;

import com.peppermint.app.cloud.senders.Sender;
import com.peppermint.app.cloud.senders.SenderObject;
import com.peppermint.app.cloud.senders.SenderUploadListener;
import com.peppermint.app.cloud.senders.SenderUploadTask;
import com.peppermint.app.data.Message;

import java.util.UUID;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for emails using a native email app.
 */
public class IntentMailSender extends Sender {

    public IntentMailSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
    }

    @Override
    public SenderUploadTask newTask(Message message, UUID enforcedId) {
        SenderUploadTask task = new IntentMailSenderTask(this, message, getSenderUploadListener());
        if(enforcedId != null) {
            task.getIdentity().setId(enforcedId);
        }
        return task;
    }

}
