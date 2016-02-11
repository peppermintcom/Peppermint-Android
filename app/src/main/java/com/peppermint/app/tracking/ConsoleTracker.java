package com.peppermint.app.tracking;

import android.content.Context;
import android.util.Log;

/**
 * Created by Nuno Luz on 17-12-2015.
 *
 * Console implementation for the Tracker API.
 */
public class ConsoleTracker extends TrackerApi {

    private static final String TAG = ConsoleTracker.class.getSimpleName();

    public ConsoleTracker(Context applicationContext) {
        super(applicationContext);
    }

    @Override
    public void track(final int type, String name, String category) {
        if(category == null) {
            category = TAG;
        }

        switch(type) {
            case TYPE_SCREEN_VIEW:
                Log.d(category, "##-Screen-View: " + name);
                break;
            case TYPE_EVENT:
                Log.d(category, "##-Event: " + name);
                break;
            default:
                Log.w(TAG, "Invalid action type: " + type);
        }
    }

    @Override
    public void logException(Throwable t) {
        Log.e(TAG, "##-Exception: " + t.getMessage(), t);
    }

    @Override
    public void log(String log, Throwable t) {
        if(t == null) {
            Log.d(TAG, log);
        } else {
            Log.d(TAG, log, t);
        }
    }

    @Override
    public void setUserEmail(String email) { }

    @Override
    public void setScreen(String screenId) { }

    @Override
    public void set(String key, String value) { }

}
