package com.peppermint.app.sending.sms;

import android.content.Context;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderObject;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.util.Map;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for SMS/text messages using the native Android API.
 */
public class SMSSender extends Sender {

    public SMSSender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, DatabaseHelper databaseHelper, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, databaseHelper, senderUploadListener);
    }

    public SMSSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
    }

    @Override
    public SenderUploadTask newTask(SendingRequest sendingRequest) {
        if (!Utils.isSimAvailable(getContext())) {
            Toast.makeText(getContext(), R.string.sender_msg_sms_disabled, Toast.LENGTH_LONG).show();
            throw new UnsupportedSMSException();
        }
        return new SMSSenderTask(this, sendingRequest, getSenderUploadListener());
    }
}
