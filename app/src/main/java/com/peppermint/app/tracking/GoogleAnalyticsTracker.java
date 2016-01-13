package com.peppermint.app.tracking;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by Nuno Luz on 17-12-2015.
 *
 * Google Analytics implementation for the Tracker API.
 */
public class GoogleAnalyticsTracker extends TrackerApi {

    private static final String TAG = GoogleAnalyticsTracker.class.getSimpleName();

    private static final String GA_ID = "UA-71499085-1";

    private Tracker mTracker;

    public GoogleAnalyticsTracker(Context applicationContext) {
        super(applicationContext);
    }

    private void initTracker() {
        if(mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(getApplicationContext());
            // to enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(GA_ID);
        }
    }

    @Override
    public void track(final int type, String name, String category) {
        initTracker();

        switch(type) {
            case TYPE_SCREEN_VIEW:
                if(name != null) {
                    setScreen(name);
                }
                mTracker.send(new HitBuilders.ScreenViewBuilder().build());
                break;
            case TYPE_EVENT:
                mTracker.send(new HitBuilders.EventBuilder().setCategory(category).setAction(name).build());
                break;
            default:
                Log.w(TAG, "Invalid action type: " + type);
        }
    }

    @Override
    public void logException(Throwable t) {
        /* nothing to do here */
    }

    @Override
    public void log(String log) {
        /* nothing to do here */
    }

    @Override
    public void setUserEmail(String email) {
        initTracker();
        mTracker.set("User-Email", email);
    }

    @Override
    public void setScreen(String screenId) {
        initTracker();
        mTracker.setScreenName(screenId);
    }

    @Override
    public void set(String key, String value) {
        initTracker();
        mTracker.set(key, value);
    }
}
