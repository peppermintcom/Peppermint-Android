package com.peppermint.app.sending.sms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;

import com.peppermint.app.R;
import com.peppermint.app.data.Message;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderUploadListener;
import com.peppermint.app.sending.SenderUploadTask;

/**
 * Created by Nuno Luz on 02-10-2015.
 *
 * SenderTask for SMS/text messages using the native Android API.
 * This is not thread-safe! An instance should only be used by a single thread.
 */
public class SMSSenderTask extends SenderUploadTask {

    private static final long SMS_REQUEST_TIMEOUT = 30000;
    private static final String SMS_SENT = "com.peppermint.app.SMS_SENT";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    mSent = true;
                    mException = null;
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    mException = new SMSNotSentException("No Service");
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    mException = new SMSNotSentException("Radio Off");
                    break;
                default:
                    mException = new SMSNotSentException("Error: " + getResultCode());
            }

            if(mRunner != null) {
                mRunner.interrupt();
            }
        }
    };
    private final IntentFilter mIntentFilter = new IntentFilter(SMS_SENT);
    private Thread mRunner;
    private boolean mSent = false;
    private SMSNotSentException mException;

    public SMSSenderTask(SMSSenderTask uploadTask) {
        super(uploadTask);
    }

    public SMSSenderTask(Sender sender, Message message, SenderUploadListener senderUploadListener) {
        super(sender, message, senderUploadListener);
    }

    @Override
    protected void execute() throws Throwable {
        setupPeppermintAuthentication();
        uploadPeppermintMessage();

        String url = getMessage().getServerShortUrl();

        mSent = false;
        mRunner = Thread.currentThread();
        getSender().getContext().registerReceiver(mReceiver, mIntentFilter);

        try {
            getMessage().setEmailBody(String.format(getSender().getContext().getString(R.string.sender_default_sms_body), url));

            Intent sentIntent = new Intent(SMS_SENT);
            PendingIntent sentPI = PendingIntent.getBroadcast(
                    getSender().getContext().getApplicationContext(), 0, sentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(getMessage().getRecipient().getVia(), null, getMessage().getEmailBody(), sentPI, null);

            try {
                // wait for SMS to be sent
                Thread.sleep(SMS_REQUEST_TIMEOUT);
            } catch(InterruptedException e) { /* nothing to do here */ }

            if(mException != null) {
                throw mException;
            }

            if(!mSent) {
                throw new SMSNotSentException("Timeout");
            }
        } finally {
            getSender().getContext().unregisterReceiver(mReceiver);
            mRunner = null;
        }
    }
}
