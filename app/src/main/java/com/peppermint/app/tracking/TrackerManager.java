package com.peppermint.app.tracking;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Nuno Luz on 17-12-2015.
 */
public class TrackerManager extends TrackerApi {

    private static TrackerManager SINGLETON;

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
