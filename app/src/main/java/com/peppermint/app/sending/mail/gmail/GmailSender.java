package com.peppermint.app.sending.mail.gmail;

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
 * Created by Nuno Luz on 01-10-2015.
 *
 * Sender that uses the GMail API to send audio messages through email.
 */
public class GmailSender extends Sender {

    private static final String TAG = GmailSender.class.getSimpleName();

    public GmailSender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, DatabaseHelper databaseHelper, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, databaseHelper, senderUploadListener);
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
