package com.peppermint.app.cloud.senders.sms.nativesms;

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
 * Sender messages using a native SMS app.
 */
public class IntentSMSSender extends Sender {

    public IntentSMSSender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, senderUploadListener);
    }

    public IntentSMSSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
    }

    @Override
    public SenderUploadTask newTask(Message message, UUID enforcedId) {
        SenderUploadTask task = new IntentSMSSenderTask(this, message, getSenderUploadListener());
        if(enforcedId != null) {
            task.getIdentity().setId(enforcedId);
        }
        return task;
    }
}
