package com.peppermint.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Nuno Luz on 05-01-2016.
 */
public class InstallReceiver extends BroadcastReceiver {

    private static final String ACTION_INSTALL_REFERRER = "com.android.vending.INSTALL_REFERRER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().compareTo(ACTION_INSTALL_REFERRER) == 0) {
            Intent launchServiceIntent = new Intent(context, SenderService.class);
            context.startService(launchServiceIntent);
        }
    }
}
