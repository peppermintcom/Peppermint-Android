package com.peppermint.app.services.messenger.handlers;

import android.content.Context;

import com.peppermint.app.trackers.TrackerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 25-01-2016.
 *
 * Generic root object of the Sender API.<br />
 * Contains common data that must be used throughout the whole API.<br />
 * It resembles an extensible context.<br />
 */
public class SenderObject implements Cloneable {

    private UUID mId = UUID.randomUUID();

    protected transient Context mContext;
    protected transient TrackerManager mTrackerManager;

    protected Map<String, Object> mParameters;
    protected transient SenderPreferences mPreferences;

    public SenderObject(final SenderObject inst) {
        this(inst.mContext, inst.mTrackerManager, inst.mParameters, inst.mPreferences);
    }

    public SenderObject(final Context context, final TrackerManager trackerManager, final Map<String, Object> parameters, final SenderPreferences senderPreferences) {
        mContext = context;
        mTrackerManager = trackerManager;
        mPreferences = senderPreferences;
        mParameters = new HashMap<>();
        if(parameters != null) {
            mParameters.putAll(parameters);
        }
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(final Context mContext) {
        this.mContext = mContext;
    }

    public TrackerManager getTrackerManager() {
        return mTrackerManager;
    }

    public void setTrackerManager(final TrackerManager mTrackerManager) {
        this.mTrackerManager = mTrackerManager;
    }

    public Map<String, Object> getParameters() {
        return mParameters;
    }

    public void setParameters(final Map<String, Object> mParameters) {
        this.mParameters = mParameters;
    }

    public Object getParameter(String key) {
        if(!mParameters.containsKey(key)) {
            return null;
        }
        return mParameters.get(key);
    }

    public void setParameter(final String key, final Object value) {
        mParameters.put(key, value);
    }

    public SenderPreferences getPreferences() {
        return mPreferences;
    }

    public void setPreferences(final SenderPreferences mPreferences) {
        this.mPreferences = mPreferences;
    }

    public UUID getId() {
        return mId;
    }

    public void setId(final UUID mId) {
        this.mId = mId;
    }
}
