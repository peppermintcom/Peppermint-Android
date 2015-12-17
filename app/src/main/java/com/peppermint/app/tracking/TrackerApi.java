package com.peppermint.app.tracking;

import android.content.Context;

/**
 * Created by Nuno Luz on 17-12-2015.
 */
public abstract class TrackerApi {

    public static final int TYPE_SCREEN_VIEW = 1;
    public static final int TYPE_EVENT = 2;

    private Context mApplicationContext;

    public TrackerApi(Context applicationContext) {
        this.mApplicationContext = applicationContext;
    }

    public abstract void track(int type, String name, String category);
    public abstract void logException(Throwable t);
    public abstract void log(String log);

    public abstract void setUserEmail(String email);
    public abstract void setScreen(String screenId);
    public abstract void set(String key, String value);

    public void trackScreenView(String screenId) {
        track(TYPE_SCREEN_VIEW, screenId, null);
    }

    public Context getApplicationContext() {
        return mApplicationContext;
    }
}
