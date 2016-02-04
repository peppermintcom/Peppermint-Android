package com.peppermint.app.sending.nativesms;

import android.content.Context;

import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderObject;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;
import com.peppermint.app.tracking.TrackerManager;

import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender messages using a native SMS app.
 */
public class IntentSMSSender extends Sender {

    public IntentSMSSender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, DatabaseHelper databaseHelper, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, databaseHelper, senderUploadListener);
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
