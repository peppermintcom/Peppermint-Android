package com.peppermint.app.gcm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Nuno Luz on 08-02-2016.
 */
public class TestService extends Service {

    public TestService() {
    }

    public final int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TEST SERVICE", intent.getAction());
        return START_STICKY;
    }

    public final IBinder onBind(Intent intent) {
        return null;
    }

}
