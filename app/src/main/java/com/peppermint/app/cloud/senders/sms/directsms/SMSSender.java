package com.peppermint.app.cloud.senders.sms.directsms;

import android.content.Context;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.cloud.senders.Sender;
import com.peppermint.app.cloud.senders.SenderObject;
import com.peppermint.app.cloud.senders.SenderUploadListener;
import com.peppermint.app.cloud.senders.SenderUploadTask;
import com.peppermint.app.data.Message;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Sender for SMS/text messages using the native Android API.
 */
public class SMSSender extends Sender {

    public SMSSender(Context context, TrackerManager trackerManager, Map<String, Object> parameters, SenderUploadListener senderUploadListener) {
        super(context, trackerManager, parameters, senderUploadListener);
    }

    public SMSSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
    }

    @Override
    public SenderUploadTask newTask(Message message, UUID enforcedId) {
        if (!Utils.isSimAvailable(getContext())) {
            Toast.makeText(getContext(), R.string.sender_msg_sms_disabled, Toast.LENGTH_LONG).show();
            throw new UnsupportedSMSException();
        }
        SenderUploadTask task = new SMSSenderTask(this, message, getSenderUploadListener());
        if(enforcedId != null) {
            task.getIdentity().setId(enforcedId);
        }
        return task;
    }
}
