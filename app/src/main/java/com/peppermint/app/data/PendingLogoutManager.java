package com.peppermint.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;

/**
 * Created by Nuno Luz on 11-05-2016.
 *
 * Database operations for {@link PendingLogout}
 */
public class PendingLogoutManager {

    private static final String TAG = PendingLogoutManager.class.getSimpleName();

    public static PendingLogout getPendingLogoutFromCursor(Cursor cursor) {
        PendingLogout pendingLogout = new PendingLogout();
        pendingLogout.setId(cursor.getLong(cursor.getColumnIndex("pending_logout_id")));
        pendingLogout.setAccountServerId(cursor.getString(cursor.getColumnIndex("account_server_id")));
        pendingLogout.setDeviceServerId(cursor.getString(cursor.getColumnIndex("device_server_id")));
        pendingLogout.setAuthenticationToken(cursor.getString(cursor.getColumnIndex("auth_token")));
        return pendingLogout;
    }

    public static int getPendingLogoutCount(SQLiteDatabase db) {
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM tbl_pending_logout;", null);
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public static Cursor getAll(SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM tbl_pending_logout;", null);
    }

    public static PendingLogout insert(SQLiteDatabase db, PendingLogout pendingLogout) throws SQLException {
        db.beginTransaction();

        long id = 0;

        try {
            ContentValues cv = new ContentValues();
            cv.put("account_server_id", pendingLogout.getAccountServerId());
            cv.put("device_server_id", pendingLogout.getDeviceServerId());
            cv.put("auth_token", pendingLogout.getAuthenticationToken());

            id = db.insert("tbl_pending_logout", null, cv);
            if (id < 0) {
                throw new SQLException("Unable to insert pending logout!");
            }

            pendingLogout.setId(id);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return pendingLogout;
    }

    public static void delete(SQLiteDatabase db, long pendingLogoutId) throws SQLException {
        long resId = db.delete("tbl_pending_logout", "pending_logout_id = " + pendingLogoutId, null);
        if (resId < 0) {
            throw new SQLException("Unable to delete pending logout!");
        }
    }

}
