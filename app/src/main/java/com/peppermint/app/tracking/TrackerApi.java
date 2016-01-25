package com.peppermint.app.tracking;

import android.content.Context;

/**
 * Created by Nuno Luz on 17-12-2015.
 *
 * Abstract tracker API implementation. Extend this class to implement
 * concrete tracker/report/analytics features.
 */
public abstract class TrackerApi {

    /**
     * Represents a screen view event
     */
    public static final int TYPE_SCREEN_VIEW = 1;

    /**
     * Represents any event
     */
    public static final int TYPE_EVENT = 2;

    private Context mApplicationContext;

    public TrackerApi(Context applicationContext) {
        this.mApplicationContext = applicationContext;
    }

    // SESSIONS

    /**
     * Start an UX session.<br />
     * Not all registered APIs need to support this.
     */
    public void startSession() { }

    /**
     * End an UX session.<br />
     * Not all registered APIs need to support this.
     */
    public void endSession() { }

    // TRACKING EVENTS

    /**
     * Register a screen view event.
     * @param screenId the screen name/ID
     */
    public void trackScreenView(String screenId) {
        track(TYPE_SCREEN_VIEW, screenId, null);
    }

    /**
     * Start tracking a timed event.<br />
     * Not all registered APIs need to support this.
     */
    public void startTrack(int type, String name, String category) { }

    /**
     * Finish tracking a timed event.<br />
     * Not all registered APIs need to support this.
     */
    public void endTrack(String name) { }

    /**
     * Register an event.<br />
     * Not all registered APIs need to support this.
     */
    public abstract void track(int type, String name, String category);

    // LOGGING

    /**
     * Log an exception.<br />
     * Not all registered APIs need to support this.
     */
    public abstract void logException(Throwable t);

    /**
     * Log entry.<br />
     * Not all registered APIs need to support this.
     */
    public void log(String log) {
        log(log, null);
    }

    public abstract void log(String log, Throwable t);

    // PARAMETERS

    /**
     * Set user email parameter.<br />
     * Not all registered APIs need to support this.
     */
    public abstract void setUserEmail(String email);

    /**
     * Set screen name/ID parameter.<br />
     * Not all registered APIs need to support this.
     */
    public abstract void setScreen(String screenId);

    /**
     * Set a custom parameter.<br />
     * Not all registered APIs need to support this.
     */
    public abstract void set(String key, String value);

    public Context getApplicationContext() {
        return mApplicationContext;
    }
}
