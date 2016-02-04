package com.peppermint.app.gcm;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;
import com.peppermint.app.MessagesService;

/**
 * Created by Nuno Luz on 02-02-2016.
 */
public class PeppermintGcmListenerService extends GcmListenerService {
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Intent relayIntent = new Intent(this, MessagesService.class);
        relayIntent.putExtra(MessagesService.PARAM_MESSAGE_RECEIVE_DATA, data);
        startService(relayIntent);
    }
}
