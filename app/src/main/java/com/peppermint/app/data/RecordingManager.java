package com.peppermint.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Database operations for {@link Recording}
 */
public class RecordingManager {

    /**
     * Gets the recording data inside the Cursor's current position and puts it in an instance
     * of the Recording structure.
     *
     * @param cursor the cursor
     * @return the Recording instance
     */
    public static Recording getFromCursor(Cursor cursor) {
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
     * Obtains the recording with the supplied ID from the database.
     *
     * @param db the local database connection
     * @param recordingId the recording id
     * @return the recording instance with all data
     */
    public static Recording getRecordingById(SQLiteDatabase db, long recordingId) {
        Recording recording = null;
        Cursor cursor = db.rawQuery("SELECT * FROM tbl_recording WHERE recording_id = " + recordingId, null);
        if(cursor.moveToFirst()) {
            recording = getFromCursor(cursor);
        }
        cursor.close();
        return recording;
    }

    /**
     * Inserts the supplied recording into the supplied local database.
     *
     * @param db the local database connection
     * @param filePath the recording's file path
     * @param durationMillis the recording's duration in milliseconds
     * @param sizeKb the recording's size in kilobytes
     * @param hasVideo if the recording includes video
     * @param recordedTimestamp the recording's timestamp
     * @param contentType the recording's content type
     * @throws SQLException
     */
    public static Recording insert(SQLiteDatabase db, String filePath, long durationMillis, float sizeKb, boolean hasVideo, String recordedTimestamp, String contentType) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("file_path", filePath);
        cv.put("duration_millis", durationMillis);
        cv.put("size_kb", sizeKb);
        cv.put("has_video", hasVideo ? 1 : 0);
        cv.put("recorded_ts", recordedTimestamp);
        cv.put("content_type", contentType);

        long id = db.insert("tbl_recording", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert recording!");
        }

        Recording recording = new Recording(filePath, durationMillis, sizeKb, hasVideo, contentType);
        recording.setRecordedTimestamp(recordedTimestamp);
        recording.setId(id);
        return recording;
    }

    /**
     * Updates the supplied recording data (ID must be supplied).
     * An SQLException is thrown if the recording ID does not exist in the database.
     *
     * @param db the local database connection
     * @param recordingId the recording id
     * @param filePath the recording's file path
     * @param durationMillis the recording's duration in milliseconds
     * @param sizeKb the recording's size in kilobytes
     * @param hasVideo if the recording includes video
     * @param recordedTimestamp the recording's timestamp
     * @param contentType the recording's content type
     * @throws SQLException
     */
    public static Recording update(SQLiteDatabase db, long recordingId, String filePath, long durationMillis, float sizeKb, boolean hasVideo, String recordedTimestamp, String contentType) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("file_path", filePath);
        cv.put("duration_millis", durationMillis);
        cv.put("size_kb", sizeKb);
        cv.put("has_video", hasVideo ? 1 : 0);
        cv.put("recorded_ts", recordedTimestamp);
        cv.put("content_type", contentType);

        long id = db.update("tbl_recording", cv, "recording_id = " + recordingId, null);
        if(id < 0) {
            throw new SQLException("Unable to update recording!");
        }

        Recording recording = new Recording(filePath, durationMillis, sizeKb, hasVideo, contentType);
        recording.setRecordedTimestamp(recordedTimestamp);
        recording.setId(recordingId);
        return recording;
    }

    /**
     * If a recording ID is supplied it performs an update, otherwise, it inserts the recording.
     *
     * @param db the local database connection
     * @param recordingId the recording id
     * @param filePath the recording's file path
     * @param durationMillis the recording's duration in milliseconds
     * @param sizeKb the recording's size in kilobytes
     * @param hasVideo if the recording includes video
     * @param recordedTimestamp the recording's timestamp
     * @param contentType the recording's content type
     * @throws SQLException
     */
    public static Recording insertOrUpdate(SQLiteDatabase db, long recordingId, String filePath, long durationMillis, float sizeKb, boolean hasVideo, String recordedTimestamp, String contentType) throws  SQLException {
        if(recordingId <= 0) {
            return insert(db, filePath, durationMillis, sizeKb, hasVideo, recordedTimestamp, contentType);
        }
        return update(db, recordingId, filePath, durationMillis, sizeKb, hasVideo, recordedTimestamp, contentType);
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

}
