package com.peppermint.app.tracking;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Nuno Luz on 17-12-2015.
 *
 * Manages multiple tracking/reporting/logging/analytics APIs.
 * Get an instance through {@link #getInstance(Context)} and always use the global application context.
 */
public class TrackerManager extends TrackerApi {

    private static TrackerManager SINGLETON;

    /**
     * Get the singleton instance of the TrackerManager. <br />
     * <strong>Always use the global application context!</strong>
     *
     * @param applicationContext the global application context
     * @return the singleton instance
     */
    public synchronized static TrackerManager getInstance(Context applicationContext) {
        if(SINGLETON == null) {
            SINGLETON = new TrackerManager(applicationContext);
        }
        return SINGLETON;
    }

    private Set<TrackerApi> mTrackers;

    public TrackerManager(Context applicationContext) {
        super(applicationContext);

        mTrackers = new HashSet<>();
        mTrackers.add(new GoogleAnalyticsTracker(applicationContext));
        mTrackers.add(new FabricTracker(applicationContext));
        mTrackers.add(new FlurryTracker(applicationContext));
    }

    public void startSession() {
        for(TrackerApi tracker : mTrackers) {
            tracker.startSession();
        }
    }

    public void endSession() {
        for(TrackerApi tracker : mTrackers) {
            tracker.endSession();
        }
    }

    public void startTrack(int type, String log, String category) {
        for(TrackerApi tracker : mTrackers) {
            tracker.startTrack(type, log, category);
        }
    }

    public void endTrack(String name) {
        for(TrackerApi tracker : mTrackers) {
            tracker.endTrack(name);
        }
    }

    @Override
    public void track(int type, String log, String category) {
        for(TrackerApi tracker : mTrackers) {
            tracker.track(type, log, category);
        }
    }

    @Override
    public void logException(Throwable t) {
        for(TrackerApi tracker : mTrackers) {
            tracker.logException(t);
        }
    }

    @Override
    public void log(String log) {
        for(TrackerApi tracker : mTrackers) {
            tracker.log(log);
        }
    }

    @Override
    public void setUserEmail(String email) {
        for(TrackerApi tracker : mTrackers) {
            tracker.setUserEmail(email);
        }
    }

    @Override
    public void setScreen(String screenId) {
        for(TrackerApi tracker : mTrackers) {
            tracker.setScreen(screenId);
        }
    }

    @Override
    public void set(String key, String value) {
        for(TrackerApi tracker : mTrackers) {
            tracker.set(key, value);
        }
    }

}
