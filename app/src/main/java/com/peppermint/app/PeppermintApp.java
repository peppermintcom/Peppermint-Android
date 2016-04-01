package com.peppermint.app;

import android.app.Application;

import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.chat.head.ChatHeadServiceManager;


/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Application class; entry point.
 */
public class PeppermintApp extends Application {

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
    }

}
