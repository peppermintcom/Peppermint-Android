package com.peppermint.app;

import android.app.Application;
import android.graphics.Typeface;
import android.util.Log;

import com.peppermint.app.tracking.TrackerManager;

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

        TrackerManager.getInstance(this);

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
