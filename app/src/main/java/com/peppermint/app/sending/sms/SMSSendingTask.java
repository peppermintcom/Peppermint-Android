package com.peppermint.app.sending.sms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.telephony.SmsManager;

import com.peppermint.app.R;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SendingTask;
import com.peppermint.app.sending.server.ServerSendingTask;

import java.util.Map;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SendingTask for SMS/text messages using the native Android API.
 * This is not thread-safe! An instance should only be used by a single thread.
 */
public class SMSSendingTask extends SendingTask {

    private static final long SMS_REQUEST_TIMEOUT = 60000;
    private static final String SMS_SENT = "com.peppermint.app.SMS_SENT";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    mSent = true;
                    if(mRunner != null) {
                        mRunner.interrupt();
                    }
                    break;
                default:
                    // error
            }
        }
    };
    private final IntentFilter mIntentFilter = new IntentFilter(SMS_SENT);
    private Thread mRunner;
    private boolean mSent = false;

    public SMSSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener) {
        super(sender, sendingRequest, listener);
    }

    public SMSSendingTask(Sender sender, SendingRequest sendingRequest, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, sendingRequest, listener, parameters, preferences);
    }

    public SMSSendingTask(SendingTask sendingTask) {
        super(sendingTask);
    }

    @Override
    protected void send() throws Throwable {
        mSent = false;
        mRunner = Thread.currentThread();
        getSender().getContext().registerReceiver(mReceiver, mIntentFilter);

        try {
            String url = (String) getSendingRequest().getParameter(ServerSendingTask.PARAM_SHORT_URL);
            getSendingRequest().setBody(String.format(getSender().getContext().getString(R.string.default_sms_body), url));

            Intent sentIntent = new Intent(SMS_SENT);
            PendingIntent sentPI = PendingIntent.getBroadcast(
                    getSender().getContext().getApplicationContext(), 0, sentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(getSendingRequest().getRecipient().getVia(), null, getSendingRequest().getBody(), sentPI, null);

            try {
                Thread.sleep(SMS_REQUEST_TIMEOUT);
            } catch(InterruptedException e) {
                // nothing to do here
            }

            if(!mSent) {
                throw new SMSNotSentException();
            }

            // the following code does not seem to be necessary (at least for Android 6)
            // it adds the SMS to the content provider so that it shows up on the native SMS app
            ContentValues values = new ContentValues();
            values.put("address", getSendingRequest().getRecipient().getVia()); // phone number to send
            values.put("date", String.valueOf(System.currentTimeMillis()));
            values.put("read", "1"); // if you want to mark is as unread set to 0
            values.put("type", "2"); // 2 means sent message
            values.put("body", getSendingRequest().getBody());

            Uri uri = Uri.parse("content://sms/");
            getSender().getContext().getContentResolver().insert(uri, values);
        } finally {
            getSender().getContext().unregisterReceiver(mReceiver);
            mRunner = null;
        }
    }
}
