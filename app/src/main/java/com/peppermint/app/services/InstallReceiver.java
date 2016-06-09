package com.peppermint.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.peppermint.app.services.messenger.MessengerService;

/**
 * Created by Nuno Luz on 05-01-2016.
 */
public class InstallReceiver extends BroadcastReceiver {

    private static final String ACTION_INSTALL_REFERRER = "com.android.vending.INSTALL_REFERRER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().compareTo(ACTION_INSTALL_REFERRER) == 0) {
            // startup all necessary services
            Intent launchServiceIntent = new Intent(context, MessengerService.class);
            context.startService(launchServiceIntent);
        }
    }
}
