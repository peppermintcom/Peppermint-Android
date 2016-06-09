package com.peppermint.app.dal;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.peppermint.app.PeppermintApp;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Created by Nuno Luz on 03-06-2016.
 *
 * Base implementation of a data object manager.<br />
 * Allows performing insert/update/delete operations and triggering respective events.<br />
 * External listeners can register for these events.<br />
 * It also keeps a weak reference cache of data object instances that allows re-usability of in-memory instances.
 */
public abstract class DataObjectManager<E, T extends DataObject> {

    private final EventBus mEventBus = new EventBus();
    private final Map<E, WeakReference<T>> mDataObjectCache = new HashMap<>();

    protected Context mContext;

    protected DataObjectManager() {
        if(PeppermintApp.DEBUG) {
            registerDataListener(new Object() {
                public void onEventBackgroundThread(DataObjectEvent<T> dataObjectEvent) {
                    Log.d(DataObjectManager.this.getClass().getSimpleName(), dataObjectEvent.toString());
                }
            }, Integer.MAX_VALUE);
        }
    }

    protected DataObjectManager(Context mContext) {
        this();
        this.mContext = mContext;
    }

    /**
     * Gets the data inside the {@link Cursor}'s current position and puts it in a {@link DataObject} instance.<br />
     *
     * @param db the local database connection (optional; might be used to load additional inner {@link DataObject}s)
     * @param cursor the cursor
     * @return the {@link DataObject} instance
     */
    public T getFromCursor(SQLiteDatabase db, Cursor cursor) {
        T instance = doGetFromCursor(db, cursor);
        instance.clearUpdateHistory();      // reset history since it was just created
        return instance;
    }

    public T insert(SQLiteDatabase db, T dataObject, boolean avoidEventBus) throws SQLException {
        dataObject = doInsert(db, dataObject);
        if(!avoidEventBus) {
            postDataEvent(DataObjectEvent.TYPE_CREATE, dataObject);
        }
        dataObject.clearUpdateHistory();
        return dataObject;
    }

    public T insert(SQLiteDatabase db, T dataObject) throws SQLException {
        return insert(db, dataObject, false);
    }

    public void update(SQLiteDatabase db, T dataObject, boolean avoidEventBus) throws SQLException {
        if(dataObject.getUpdateHistory().size() > 0) {
            doUpdate(db, dataObject);
            if(!avoidEventBus) {
                postDataEvent(DataObjectEvent.TYPE_UPDATE, dataObject);
            }
            dataObject.clearUpdateHistory();
        }
    }

    public void update(SQLiteDatabase db, T dataObject) throws SQLException {
        update(db, dataObject, false);
    }

    public void delete(SQLiteDatabase db, E dataObjectId, boolean avoidEventBus) throws SQLException {
        doDelete(db, dataObjectId);
        if(!avoidEventBus) {
            postDataEvent(DataObjectEvent.TYPE_DELETE, obtainCacheDataObject(dataObjectId));
        }
    }

    public void delete(SQLiteDatabase db, E dataObjectId) throws SQLException {
        delete(db, dataObjectId, false);
    }

    public T insertOrUpdate(SQLiteDatabase db, T dataObject, boolean avoidEventBus) throws SQLException {
        if(!exists(db, dataObject)) {
            return insert(db, dataObject, avoidEventBus);
        }
        update(db, dataObject, avoidEventBus);
        return dataObject;
    }

    public T insertOrUpdate(SQLiteDatabase db, T dataObject) throws SQLException {
        return insertOrUpdate(db, dataObject, false);
    }

    protected abstract T doGetFromCursor(SQLiteDatabase db, Cursor cursor);
    protected abstract T doInsert(SQLiteDatabase db, T dataObject) throws SQLException;
    protected abstract void doUpdate(SQLiteDatabase db, T dataObject) throws SQLException;
    protected abstract void doDelete(SQLiteDatabase db, E dataObjectId) throws SQLException;

    public abstract boolean exists(SQLiteDatabase db, T dataObject) throws SQLException;

    public void registerDataListener(Object listener) {
        mEventBus.register(listener);
        cleanupRecycledCache();
    }

    public void registerDataListener(Object listener, int priority) {
        mEventBus.register(listener, priority);
        cleanupRecycledCache();
    }

    public void unregisterDataListener(Object listener) {
        mEventBus.unregister(listener);
        cleanupRecycledCache();
    }

    protected void postDataEvent(int eventType, T dataObject) {
        if(mEventBus.hasSubscriberForEvent(DataObjectEvent.class)) {
            mEventBus.post(new DataObjectEvent<>(eventType, dataObject, dataObject.getUpdateHistory().clone()));
        }
    }

    public T obtainCacheDataObject(E id) {
        T dataObject = null;
        WeakReference<T> weakReference = mDataObjectCache.get(id);
        if(weakReference != null) {
            dataObject = weakReference.get();
        }
        if(dataObject == null) {
            dataObject = newDataObjectInstance(id);
            mDataObjectCache.put(id, new WeakReference<T>(dataObject));
        }
        return dataObject;
    }

    protected abstract T newDataObjectInstance(E id);

    public void cleanupRecycledCache() {
        final Iterator<Map.Entry<E, WeakReference<T>>> it = mDataObjectCache.entrySet().iterator();
        while(it.hasNext()) {
            final Map.Entry<E, WeakReference<T>> entry = it.next();
            if(entry.getValue() != null && entry.getValue().get() == null) {
                it.remove();
            }
        }
    }
}
