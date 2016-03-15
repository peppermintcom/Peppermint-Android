package com.peppermint.app.cloud.senders.mail.gmail;

import android.content.Context;

import com.peppermint.app.cloud.senders.Sender;
import com.peppermint.app.cloud.senders.SenderObject;
import com.peppermint.app.cloud.senders.SenderUploadListener;
import com.peppermint.app.cloud.senders.SenderUploadTask;
import com.peppermint.app.data.Message;
import com.peppermint.app.tracking.TrackerManager;

import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Sender that uses the GMail API to send audio messages through email.
 */
public class GmailSender extends Sender {

    public GmailSender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, senderUploadListener);
        construct();
    }

    public GmailSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
        construct();
    }

    private void construct() {
        mPreferences = new GmailSenderPreferences(getContext());
        mErrorHandler = new GmailSenderErrorHandler(this, getSenderUploadListener());
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
