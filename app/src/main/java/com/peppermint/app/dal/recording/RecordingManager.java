package com.peppermint.app.dal.recording;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.dal.DataObjectManager;

import java.sql.SQLException;

/**
 * Created by Nuno Luz on 18-02-2016.
 *
 * Database operations for {@link Recording}
 */
public class RecordingManager extends DataObjectManager<Long, Recording> {

    private static RecordingManager INSTANCE;

    public static RecordingManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new RecordingManager();
        }
        return INSTANCE;
    }

    protected RecordingManager() {
        super();
    }

    @Override
    protected Recording doInsert(SQLiteDatabase db, Recording recording) throws SQLException {
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

    @Override
    protected void doUpdate(SQLiteDatabase db, Recording recording) throws SQLException {
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
    }

    @Override
    protected void doDelete(SQLiteDatabase db, Long recordingId) throws SQLException {
        long id = db.delete("tbl_recording", "recording_id = " + recordingId, null);
        if(id < 0) {
            throw new SQLException("Unable to delete the recording!");
        }
    }

    @Override
    protected Recording newDataObjectInstance(Long id) {
        final Recording recording = new Recording();
        recording.setId(id);
        return recording;
    }

    @Override
    protected Recording doGetFromCursor(SQLiteDatabase db, Cursor cursor) {
        final Recording recording = obtainCacheDataObject(cursor.getLong(cursor.getColumnIndex("recording_id")));
        recording.setFilePath(cursor.getString(cursor.getColumnIndex("file_path")));
        recording.setContentType(cursor.getString(cursor.getColumnIndex("content_type")));
        recording.setDurationMillis(cursor.getLong(cursor.getColumnIndex("duration_millis")));
        recording.setSizeKb(cursor.getFloat(cursor.getColumnIndex("size_kb")));
        recording.setHasVideo(cursor.getInt(cursor.getColumnIndex("has_video")) > 0);
        recording.setRecordedTimestamp(cursor.getString(cursor.getColumnIndex("recorded_ts")));
        recording.setTranscription(cursor.getString(cursor.getColumnIndex("transcription")));
        recording.setTranscriptionLanguage(cursor.getString(cursor.getColumnIndex("transcription_lang")));
        recording.setTranscriptionUrl(cursor.getString(cursor.getColumnIndex("transcription_url")));
        recording.setTranscriptionConfidence(cursor.isNull(cursor.getColumnIndex("transcription_confidence")) ?
                -1f : cursor.getFloat(cursor.getColumnIndex("transcription_confidence")));
        return recording;
    }

    @Override
    public boolean exists(SQLiteDatabase db, Recording recording) throws SQLException {
        return recording.getId() > 0;
    }

    public Recording getRecordingById(SQLiteDatabase db, long recordingId) {
        Recording recording = null;
        Cursor cursor = db.rawQuery("SELECT * FROM tbl_recording WHERE recording_id = " + recordingId, null);
        if(cursor.moveToFirst()) {
            recording = getFromCursor(db, cursor);
        }
        cursor.close();
        return recording;
    }

}
