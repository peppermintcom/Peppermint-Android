package com.peppermint.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.google.android.gms.security.ProviderInstaller;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApi;
import com.peppermint.app.cloud.apis.sparkpost.SparkPostApi;
import com.peppermint.app.services.messenger.MessengerServiceManager;
import com.peppermint.app.services.messenger.handlers.SenderObject;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.services.messenger.handlers.gmail.GmailSender;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.chat.head.ChatHeadServiceManager;


/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Application class; entry point.
 */
public class PeppermintApp extends MultiDexApplication {

    private static final String TAG = PeppermintApi.class.getSimpleName();

    private static final String PREF_LAST_VERSION = "PeppermintApp_LastVersion";

    @Override
    public void onCreate() {
        super.onCreate();

        // try to keep SSL up to date
        ProviderInstaller.installIfNeededAsync(this, new ProviderInstaller.ProviderInstallListener() {
            @Override
            public void onProviderInstalled() {
                Log.i(TAG, "Security Provider Installed");
            }
            @Override
            public void onProviderInstallFailed(int i, Intent intent) {
                Log.e(TAG, "Security Provider Install Failed!");
            }
        });

        // init tracker apis
        TrackerManager.getInstance(this);

        // start the service so that we can receive GCM notifications
        MessengerServiceManager messengerServiceManager = new MessengerServiceManager(this);
        messengerServiceManager.start();

        // try to start and enable the chat head overlay
        ChatHeadServiceManager.startAndEnable(this);

        // check onUpgrade
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int lastVersion = sharedPreferences.getInt(PREF_LAST_VERSION, 0);
        if(lastVersion < BuildConfig.VERSION_CODE) {
            // important! this will be executed if the app's data is cleared
            onUpgrade(lastVersion, BuildConfig.VERSION_CODE, sharedPreferences);
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(PREF_LAST_VERSION, BuildConfig.VERSION_CODE);
            editor.commit();
        }

        if(BuildConfig.DEBUG) {
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

    private void onUpgrade(final int lastVersion, final int newVersion, final SharedPreferences sharedPreferences) {
        if(lastVersion < 15) {  // Upgrade to 1.1.13
            // always enabled (forced) gmail sender
            final SenderObject senderObject = new SenderObject(this, TrackerManager.getInstance(this), null, new SenderPreferences(this));
            final GmailSender gmailSender = new GmailSender(senderObject, null);
            gmailSender.setEnabled(true);
        }
        if(lastVersion < 22) {
            // force refresh the email template
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(SparkPostApi.PREF_LAST_TEMPLATE_UPDATE_TIMESTAMP);
            editor.commit();
        }
    }
}
