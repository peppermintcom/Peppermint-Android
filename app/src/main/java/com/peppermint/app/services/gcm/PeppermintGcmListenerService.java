package com.peppermint.app.services.gcm;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;
import com.peppermint.app.services.messenger.MessengerService;

/**
 * Created by Nuno Luz on 02-02-2016.
 *
 * GCM Service that receives GCM notifications.
 */
public class PeppermintGcmListenerService extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Intent relayIntent = new Intent(this, MessengerService.class);
        relayIntent.putExtra(MessengerService.PARAM_MESSAGE_RECEIVE_DATA, data);
        startService(relayIntent);
    }
}
