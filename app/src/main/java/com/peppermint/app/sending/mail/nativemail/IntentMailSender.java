package com.peppermint.app.sending.mail.nativemail;

import android.content.Context;

import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderObject;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;
import com.peppermint.app.tracking.TrackerManager;

import java.util.Map;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for emails using a native email app.
 */
public class IntentMailSender extends Sender {

    public IntentMailSender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, DatabaseHelper databaseHelper, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, databaseHelper, senderUploadListener);
        construct();
    }

    public IntentMailSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
        construct();
    }

    private void construct() {
        mPreferences = new IntentMailSenderPreferences(getContext());
        mErrorHandler = new IntentMailSenderErrorHandler(this, getSenderUploadListener());
    }

    @Override
    public SenderUploadTask newTask(SendingRequest sendingRequest) {
        return new IntentMailSenderTask(this, sendingRequest, getSenderUploadListener());
    }

}
