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
    public static Recording getRecordingFromCursor(Cursor cursor) {
        Recording recording = new Recording();
        recording.setId(cursor.getLong(cursor.getColumnIndex("recording_id")));
        recording.setFilePath(cursor.getString(cursor.getColumnIndex("file_path")));
        recording.setContentType(cursor.getString(cursor.getColumnIndex("content_type")));
        recording.setDurationMillis(cursor.getLong(cursor.getColumnIndex("duration_millis")));
        recording.setSizeKb(cursor.getFloat(cursor.getColumnIndex("size_kb")));
        recording.setHasVideo(cursor.getInt(cursor.getColumnIndex("has_video")) > 0);
        recording.setRecordedTimestamp(cursor.getString(cursor.getColumnIndex("recorded_ts")));
        recording.setTranscription(cursor.getString(cursor.getColumnIndex("transcription")));
        recording.setTranscriptionLanguage(cursor.getString(cursor.getColumnIndex("transcription_lang")));
        recording.setTranscriptionUrl(cursor.getString(cursor.getColumnIndex("transcription_url")));
        final Float confidence = cursor.getFloat(cursor.getColumnIndex("transcription_confidence"));
        recording.setTranscriptionConfidence(confidence == null ? -1 : confidence);
        return recording;
    }

    public static Recording getRecordingById(SQLiteDatabase db, long recordingId) {
        Recording recording = null;
        Cursor cursor = db.rawQuery("SELECT * FROM tbl_recording WHERE recording_id = " + recordingId, null);
        if(cursor.moveToFirst()) {
            recording = getRecordingFromCursor(cursor);
        }
        cursor.close();
        return recording;
    }

    // OPERATIONS

    public static Recording insert(SQLiteDatabase db, Recording recording) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("file_path", recording.getFilePath());
        cv.put("duration_millis", recording.getDurationMillis());
        cv.put("size_kb", recording.getSizeKb());
        cv.put("has_video", recording.hasVideo() ? 1 : 0);
        cv.put("recorded_ts", recording.getRecordedTimestamp());
        cv.put("content_type", recording.getContentType());
        cv.put("transcription", recording.getTranscription());
        cv.put("transcription_lang", recording.getTranscriptionLanguage());
        cv.put("transcription_url", recording.getTranscriptionUrl());
        if(recording.getTranscriptionConfidence() >= 0) {
            cv.put("transcription_confidence", recording.getTranscriptionConfidence());
        } else {
            cv.putNull("transcription_confidence");
        }

        long id = db.insert("tbl_recording", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert recording!");
        }

        recording.setId(id);
        return recording;
    }

    public static Recording update(SQLiteDatabase db, Recording recording) throws SQLException {
        if(recording.getId() <= 0) {
            throw new IllegalArgumentException("Recording Id must be supplied!");
        }

        ContentValues cv = new ContentValues();
        cv.put("file_path", recording.getFilePath());
        cv.put("duration_millis", recording.getDurationMillis());
        cv.put("size_kb", recording.getSizeKb());
        cv.put("has_video", recording.hasVideo() ? 1 : 0);
        cv.put("recorded_ts", recording.getRecordedTimestamp());
        cv.put("content_type", recording.getContentType());
        if(recording.getTranscriptionConfidence() >= 0) {
            cv.put("transcription_confidence", recording.getTranscriptionConfidence());
        } else {
            cv.putNull("transcription_confidence");
        }
        if(recording.getTranscription() != null) {
            cv.put("transcription", recording.getTranscription());
        }
        if(recording.getTranscriptionLanguage() != null) {
            cv.put("transcription_lang", recording.getTranscriptionLanguage());
        }
        if(recording.getTranscriptionUrl() != null) {
            cv.put("transcription_url", recording.getTranscriptionUrl());
        }

        long id = db.update("tbl_recording", cv, "recording_id = " + recording.getId(), null);
        if(id < 0) {
            throw new SQLException("Unable to update recording!");
        }

        return recording;
    }

    public static Recording insertOrUpdate(SQLiteDatabase db, Recording recording) throws  SQLException {
        if(recording.getId() <= 0) {
            return insert(db, recording);
        }
        return update(db, recording);
    }

    public static void delete(SQLiteDatabase db, long recordingId) throws SQLException {
        long id = db.delete("tbl_recording", "recording_id = " + recordingId, null);
        if(id < 0) {
            throw new SQLException("Unable to delete the recording!");
        }
    }

}
