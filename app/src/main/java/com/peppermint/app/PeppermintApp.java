package com.peppermint.app;

import android.app.Activity;
import android.app.Application;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;

import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.tracking.TrackerManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Application class; entry point.
 */
public class PeppermintApp extends Application implements Application.ActivityLifecycleCallbacks {

    private Typeface mFontSemibold, mFontBold, mFontRegular;
    private Set<String> mVisibleActivities;

    @Override
    public void onCreate() {
        super.onCreate();

        mVisibleActivities = new HashSet<>();

        TrackerManager.getInstance(this);

        mFontSemibold = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Semibold.ttf");
        mFontBold = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Bold.ttf");
        mFontRegular = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Regular.ttf");

        Log.d("PeppermintApp", "App has started!");

        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onTerminate() {
        unregisterActivityLifecycleCallbacks(this);
        super.onTerminate();
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

    private void setBackgroundForegroundFlag() {
        SenderErrorHandler.IS_APP_ON_BACKGROUND = mVisibleActivities.size() <= 0;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        mVisibleActivities.add(activity.getClass().getCanonicalName());
        setBackgroundForegroundFlag();
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mVisibleActivities.remove(activity.getClass().getCanonicalName());
        setBackgroundForegroundFlag();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
