package com.peppermint.app;

import android.app.Application;
import android.graphics.Typeface;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

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
        //Fabric.with(this, new Crashlytics());

        mFontSemibold = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Semibold.ttf");
        mFontBold = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Bold.ttf");
        mFontRegular = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Regular.ttf");

        Log.d("PeppermintApp", "App has started!");
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
