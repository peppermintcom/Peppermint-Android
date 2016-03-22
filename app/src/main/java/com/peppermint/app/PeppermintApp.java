package com.peppermint.app;

import android.app.Application;
import android.content.Intent;
import android.graphics.Typeface;

import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.chat.head.ChatHeadService;


/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Application class; entry point.
 */
public class PeppermintApp extends Application {

    private Typeface mFontSemibold, mFontBold, mFontRegular;

    @Override
    public void onCreate() {
        super.onCreate();

        // init tracker apis
        TrackerManager.getInstance(this);

        mFontSemibold = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Semibold.ttf");
        mFontBold = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Bold.ttf");
        mFontRegular = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Regular.ttf");

        // start the service so that we can receive GCM notifications
        MessagesServiceManager messagesServiceManager = new MessagesServiceManager(this);
        messagesServiceManager.start();

        startService(new Intent(ChatHeadService.ACTION_ENABLE, null, this, ChatHeadService.class));
    }

    public Typeface getFontSemibold() {
        return mFontSemibold;
    }

    public Typeface getFontBold() {
        return mFontBold;
    }

    public Typeface getFontRegular() {
        return mFontRegular;
    }
}
