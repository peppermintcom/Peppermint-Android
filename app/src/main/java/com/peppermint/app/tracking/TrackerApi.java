package com.peppermint.app.tracking;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

/**
 * Created by Nuno Luz on 17-12-2015.
 *
 * Abstract tracker API implementation. Extend this class to implement
 * concrete tracker/report/analytics features.
 */
public abstract class TrackerApi {

    private static final String TAG = TrackerApi.class.getSimpleName();

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

    public static String getThrowableString(Throwable t, String additionalMessage) {
        try {
            StringBuilder writer = new StringBuilder();
            if(additionalMessage != null) {
                writer.append(additionalMessage).append("\n");
            }
            for(boolean e = true; t != null; t = t.getCause()) {
                String message = t.getLocalizedMessage();
                message = message == null ? "" : message.replaceAll("(\r\n|\n|\f)", " ");
                String causedBy = e ? "" : "Caused by: ";
                writer.append(causedBy).append(t.getClass().getName()).append(": ").append(message).append("\n");
                e = false;
                StackTraceElement[] arr$ = t.getStackTrace();
                int len$ = arr$.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    StackTraceElement element = arr$[i$];
                    writer.append("\tat ").append(element.toString()).append("\n");
                }
            }

            return writer.toString();
        } catch (Exception ex) {
            Log.e(TAG, "Unable to get printable throwable!", ex);
            Crashlytics.logException(ex);
            return null;
        }
    }
}
