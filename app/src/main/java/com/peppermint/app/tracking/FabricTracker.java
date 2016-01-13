package com.peppermint.app.tracking;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.AnswersEvent;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.ndk.CrashlyticsNdk;

import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Nuno Luz on 17-12-2015.
 *
 * Fabric implementation for the Tracker API.
 */
public class FabricTracker extends TrackerApi {

    private static final String TAG = FabricTracker.class.getSimpleName();

    private static final String SCREEN_KEY = "ScreenId";

    private Map<String, String> mValueMap;

    public FabricTracker(Context applicationContext) {
        super(applicationContext);
        mValueMap = new HashMap<>();
        Fabric.with(applicationContext, new Crashlytics(), new CrashlyticsNdk());
    }

    protected <T extends AnswersEvent<?>> T initEvent(T event) {
        for(Map.Entry<String, String> entry : mValueMap.entrySet()) {
            event.putCustomAttribute(entry.getKey(), entry.getValue());
        }
        return event;
    }

    @Override
    public void track(final int type, String name, String category) {
        switch(type) {
            case TYPE_SCREEN_VIEW:
                Answers.getInstance().logContentView(initEvent(new ContentViewEvent().putContentName(name)));
                break;
            case TYPE_EVENT:
                CustomEvent event = initEvent(new CustomEvent(name));
                if(category != null) {
                    event.putCustomAttribute("Category", category);
                }
                Answers.getInstance().logCustom(event);
                break;
            default:
                Log.w(TAG, "Invalid action type: " + type);
        }
    }

    @Override
    public void logException(Throwable t) {
        Crashlytics.logException(t);
    }

    @Override
    public void log(String log) {
        Crashlytics.log(log);
    }

    @Override
    public void setUserEmail(String email) {
        Crashlytics.setUserEmail(email);
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
