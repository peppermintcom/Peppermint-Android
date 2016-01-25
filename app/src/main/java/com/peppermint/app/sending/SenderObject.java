package com.peppermint.app.sending;

import android.content.Context;

import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.tracking.TrackerManager;

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

    protected Context mContext;
    protected TrackerManager mTrackerManager;

    protected Map<String, Object> mParameters;
    protected SenderPreferences mPreferences;
    protected DatabaseHelper mDatabaseHelper;

    public SenderObject(SenderObject inst) {
        this(inst.mContext, inst.mTrackerManager, inst.mParameters, inst.mPreferences, inst.mDatabaseHelper);
    }

    public SenderObject(Context context, TrackerManager trackerManager, Map<String, Object> parameters, SenderPreferences senderPreferences, DatabaseHelper databaseHelper) {
        mContext = context;
        mTrackerManager = trackerManager;
        mPreferences = senderPreferences;
        mDatabaseHelper = databaseHelper;
        mParameters = new HashMap<>();
        if(parameters != null) {
            mParameters.putAll(parameters);
        }
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public TrackerManager getTrackerManager() {
        return mTrackerManager;
    }

    public void setTrackerManager(TrackerManager mTrackerManager) {
        this.mTrackerManager = mTrackerManager;
    }

    public Map<String, Object> getParameters() {
        return mParameters;
    }

    public void setParameters(Map<String, Object> mParameters) {
        this.mParameters = mParameters;
    }

    public Object getParameter(String key) {
        if(!mParameters.containsKey(key)) {
            return null;
        }
        return mParameters.get(key);
    }

    public void setParameter(String key, Object value) {
        mParameters.put(key, value);
    }

    public SenderPreferences getPreferences() {
        return mPreferences;
    }

    public void setPreferences(SenderPreferences mPreferences) {
        this.mPreferences = mPreferences;
    }

    public DatabaseHelper getDatabaseHelper() {
        return mDatabaseHelper;
    }

    public void setDatabaseHelper(DatabaseHelper mDatabaseHelper) {
        this.mDatabaseHelper = mDatabaseHelper;
    }

    public UUID getId() {
        return mId;
    }

    public void setId(UUID mId) {
        this.mId = mId;
    }
}
