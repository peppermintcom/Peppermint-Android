package com.peppermint.app.cloud.senders.mail.nativemail;

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
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for emails using a native email app.
 */
public class IntentMailSender extends Sender {

    public IntentMailSender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, senderUploadListener);
        construct();
    }

    public IntentMailSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
        construct();
    }

    private void construct() {
        mPreferences = new IntentMailSenderPreferences(getContext());
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
