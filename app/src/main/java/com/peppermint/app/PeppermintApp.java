package com.peppermint.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.cloud.senders.SenderObject;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.cloud.senders.mail.gmail.GmailSender;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.chat.head.ChatHeadServiceManager;


/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Application class; entry point.
 */
public class PeppermintApp extends Application {

    public static final boolean DEBUG = true;

    private static final String PREF_LAST_VERSION = "PeppermintApp_LastVersion";

    @Override
    public void onCreate() {
        super.onCreate();

        // init tracker apis
        TrackerManager.getInstance(this);

        // start the service so that we can receive GCM notifications
        MessagesServiceManager messagesServiceManager = new MessagesServiceManager(this);
        messagesServiceManager.start();

        // try to start and enable the chat head overlay
        ChatHeadServiceManager.startAndEnable(this);

        // check onUpgrade
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int lastVersion = sharedPreferences.getInt(PREF_LAST_VERSION, 0);
        if(lastVersion < BuildConfig.VERSION_CODE) {
            // important! this will be executed if the app's data is cleared
            onUpgrade(lastVersion, BuildConfig.VERSION_CODE);
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(PREF_LAST_VERSION, BuildConfig.VERSION_CODE);
            editor.commit();
        }

        if(DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }

    private void onUpgrade(final int lastVersion, final int newVersion) {
        if(lastVersion < 15) {  // Upgrade to 1.1.13
            // always enabled (forced) gmail sender
            final SenderObject senderObject = new SenderObject(this, TrackerManager.getInstance(this), null, new SenderPreferences(this));
            final GmailSender gmailSender = new GmailSender(senderObject, null);
            gmailSender.setEnabled(true);
        }
    }
}
