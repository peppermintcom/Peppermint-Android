package com.peppermint.app.tracking;

import android.content.Context;
import android.util.Log;

import com.flurry.android.FlurryAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz on 17-12-2015.
 *
 * Flurry implementation for the Tracker API.
 */
public class FlurryTracker extends TrackerApi {

    private static final String TAG = FlurryTracker.class.getSimpleName();

    private static final String API_KEY = "R4TH2QMMV3C86GS6WYNC";

    private static final String SCREEN_KEY = "ScreenId";

    private Map<String, String> mValueMap;

    public FlurryTracker(Context applicationContext) {
        super(applicationContext);
        mValueMap = new HashMap<>();

        // configure Flurry
        FlurryAgent.setCaptureUncaughtExceptions(true);
        FlurryAgent.setLogEnabled(true);
        FlurryAgent.setLogEvents(true);
        FlurryAgent.setLogLevel(Log.VERBOSE);

        // init Flurry
        FlurryAgent.init(applicationContext, API_KEY);
    }

    @Override
    public void startSession() {
        FlurryAgent.onStartSession(getApplicationContext());
    }

    @Override
    public void endSession() {
        FlurryAgent.onEndSession(getApplicationContext());
    }

    @Override
    public void startTrack(int type, String name, String category) {
        Map<String, String> map = new HashMap<>(mValueMap);
        if(category != null) {
            map.put("Category", category);
        }

        switch(type) {
            case TYPE_SCREEN_VIEW:
                map.put("Category", "ScreenView");
            case TYPE_EVENT:
                FlurryAgent.logEvent(name, map, true);
                break;
            default:
                Log.w(TAG, "Invalid action type: " + type);
        }
    }

    @Override
    public void endTrack(String name) {
        FlurryAgent.endTimedEvent(name);
    }

    @Override
    public void track(final int type, String name, String category) {
        Map<String, String> map = new HashMap<>(mValueMap);
        if(category != null) {
            map.put("Category", category);
        }

        switch(type) {
            case TYPE_SCREEN_VIEW:
                map.put("Category", "ScreenView");
            case TYPE_EVENT:
                FlurryAgent.logEvent(name, map);
                break;
            default:
                Log.w(TAG, "Invalid action type: " + type);
        }
    }

    @Override
    public void logException(Throwable t) {
        /* disabled; not supported */
    }

    @Override
    public void log(String log) {
        /* disabled; not supported */
    }

    @Override
    public void setUserEmail(String email) {
        FlurryAgent.setUserId(email);
    }

    @Override
    public void setScreen(String screenId) {
        mValueMap.put(SCREEN_KEY, screenId);
    }

    @Override
    public void set(String key, String value) {
        mValueMap.put(key, value);
    }
}
