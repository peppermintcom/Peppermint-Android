package com.peppermint.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;

/**
 * Created by Nuno Luz on 18-02-2016.
 */
public class RecordingManager {

    /**
     * Gets the recording data inside the Cursor's current position and puts it in an instance
     * of the Recording structure.
     *
     * @param cursor the cursor
     * @return the Recording instance
     */
    private static Recording getFromCursor(Cursor cursor) {
        Recording recording = new Recording();
        recording.setId(cursor.getLong(cursor.getColumnIndex("recording_id")));
        recording.setFilePath(cursor.getString(cursor.getColumnIndex("file_path")));
        recording.setContentType(cursor.getString(cursor.getColumnIndex("content_type")));
        recording.setDurationMillis(cursor.getLong(cursor.getColumnIndex("duration_millis")));
        recording.setSizeKb(cursor.getFloat(cursor.getColumnIndex("size_kb")));
        recording.setHasVideo(cursor.getInt(cursor.getColumnIndex("has_video")) > 0);
        recording.setRecordedTimestamp(cursor.getString(cursor.getColumnIndex("recorded_ts")));
        return recording;
    }

    /**
     * Inserts the supplied recording into the supplied local database.
     *
     * @param db the local database connection
     * @param recording the recording
     * @throws SQLException
     */
    public static long insert(SQLiteDatabase db, Recording recording) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("file_path", recording.getFilePath());
        cv.put("duration_millis", recording.getDurationMillis());
        cv.put("size_kb", recording.getSizeKb());
        cv.put("has_video", recording.hasVideo() ? 1 : 0);
        cv.put("recorded_ts", recording.getRecordedTimestamp());
        cv.put("content_type", recording.getContentType());

        long id = db.insert("tbl_recording", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert recording!");
        }

        recording.setId(id);
        return id;
    }

    /**
     * Updates the supplied recording data (ID must be supplied).
     * An SQLException is thrown if the recording ID does not exist in the database.
     *
     * @param db the local database connection
     * @param recording the recording
     * @throws SQLException
     */
    public static void update(SQLiteDatabase db, Recording recording) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("file_path", recording.getFilePath());
        cv.put("duration_millis", recording.getDurationMillis());
        cv.put("size_kb", recording.getSizeKb());
        cv.put("has_video", recording.hasVideo() ? 1 : 0);
        cv.put("recorded_ts", recording.getRecordedTimestamp());
        cv.put("content_type", recording.getContentType());

        long id = db.update("tbl_recording", cv, "recording_id = " + recording.getId(), null);
        if(id < 0) {
            throw new SQLException("Unable to update recording!");
        }
    }

    /**
     * If a recording ID is supplied it performs an update, otherwise, it inserts the recording.
     *
     * @param db the local database connection
     * @param recording the recording
     * @throws SQLException
     */
    public static void insertOrUpdate(SQLiteDatabase db, Recording recording) throws  SQLException {
        if(recording.getId() <= 0) {
            insert(db, recording);
            return;
        }
        update(db, recording);
    }

    /**
     * Deletes the supplied recording data (ID must be supplied).
     * An SQLException is thrown if the recording ID does not exist in the database.
     *
     * @param db the local database connection
     * @param recordingId the recording id
     * @throws SQLException
     */
    public static void delete(SQLiteDatabase db, long recordingId) throws SQLException {
        long id = db.delete("tbl_recording", "recording_id = " + recordingId, null);
        if(id < 0) {
            throw new SQLException("Unable to delete the recording!");
        }
    }

    /**
     * Obtains the recording with the supplied ID from the database.
     *
     * @param db the local database connection
     * @param id the recording ID
     * @return the recording instance with all data
     */
    public static Recording get(SQLiteDatabase db, long id) {
        Recording recording = null;
        Cursor cursor = db.rawQuery("SELECT * FROM tbl_recording WHERE recording_id = " + id, null);
        if(cursor.moveToFirst()) {
            recording = getFromCursor(cursor);
        }
        cursor.close();
        return recording;
    }

}
