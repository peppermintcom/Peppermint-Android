package com.peppermint.app.dal.pendinglogout;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.dal.DataObjectManager;

import java.sql.SQLException;

/**
 * Created by Nuno Luz on 11-05-2016.
 *
 * Database operations for {@link PendingLogout}
 */
public class PendingLogoutManager extends DataObjectManager<Long, PendingLogout> {

    private static PendingLogoutManager INSTANCE;

    public static PendingLogoutManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new PendingLogoutManager();
        }
        return INSTANCE;
    }

    protected PendingLogoutManager() {
        super();
    }

    @Override
    protected PendingLogout doInsert(SQLiteDatabase db, PendingLogout pendingLogout) throws SQLException {

        ContentValues cv = new ContentValues();
        cv.put("account_server_id", pendingLogout.getAccountServerId());
        cv.put("device_server_id", pendingLogout.getDeviceServerId());
        cv.put("auth_token", pendingLogout.getAuthenticationToken());

        long id = db.insert("tbl_pending_logout", null, cv);
        if (id < 0) {
            throw new SQLException("Unable to insert pending logout!");
        }
        pendingLogout.setId(id);

        return pendingLogout;
    }

    @Override
    protected void doUpdate(SQLiteDatabase db, PendingLogout dataObject) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDelete(SQLiteDatabase db, Long pendingLogoutId) throws SQLException {
        long resId = db.delete("tbl_pending_logout", "pending_logout_id = " + pendingLogoutId, null);
        if (resId < 0) {
            throw new SQLException("Unable to delete pending logout!");
        }
    }

    @Override
    protected PendingLogout newDataObjectInstance(Long id) {
        final PendingLogout pendingLogout = new PendingLogout();
        pendingLogout.setId(id);
        return pendingLogout;
    }

    @Override
    protected PendingLogout doGetFromCursor(SQLiteDatabase db, Cursor cursor) {
        PendingLogout pendingLogout = obtainCacheDataObject(cursor.getLong(cursor.getColumnIndex("pending_logout_id")));
        pendingLogout.setAccountServerId(cursor.getString(cursor.getColumnIndex("account_server_id")));
        pendingLogout.setDeviceServerId(cursor.getString(cursor.getColumnIndex("device_server_id")));
        pendingLogout.setAuthenticationToken(cursor.getString(cursor.getColumnIndex("auth_token")));
        return pendingLogout;
    }

    @Override
    public boolean exists(SQLiteDatabase db, PendingLogout dataObject) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Cursor getAll(SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM tbl_pending_logout;", null);
    }

}
