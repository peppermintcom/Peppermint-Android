package com.peppermint.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.Serializable;
import java.sql.SQLException;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an audio/video recording present in a local file.
 */
public class Recording implements Serializable {

    private static Recording getFromCursor(Cursor cursor) {
        Recording recording = new Recording();
        recording.setId(cursor.getLong(cursor.getColumnIndex("recording_id")));
        recording.setFilePath(cursor.getString(cursor.getColumnIndex("file_path")));
        recording.setDurationMillis(cursor.getLong(cursor.getColumnIndex("duration_millis")));
        recording.setSizeKb(cursor.getFloat(cursor.getColumnIndex("size_kb")));
        recording.setHasVideo(cursor.getInt(cursor.getColumnIndex("has_video")) > 0);
        recording.setRecordedTimestamp(cursor.getString(cursor.getColumnIndex("recorded_ts")));
        return recording;
    }

    public static void insert(SQLiteDatabase db, Recording recording) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("file_path", recording.getFilePath());
        cv.put("duration_millis", recording.getDurationMillis());
        cv.put("size_kb", recording.getSizeKb());
        cv.put("has_video", recording.hasVideo() ? 1 : 0);
        cv.put("recorded_ts", recording.getRecordedTimestamp());

        long id = db.insert("tbl_sending_request_recording", null, cv);
        if(id < 0) {
            throw new SQLException("Unable to insert recording!");
        }

        recording.setId(id);
    }

    public static void update(SQLiteDatabase db, Recording recording) throws SQLException {
        ContentValues cv = new ContentValues();
        cv.put("file_path", recording.getFilePath());
        cv.put("duration_millis", recording.getDurationMillis());
        cv.put("size_kb", recording.getSizeKb());
        cv.put("has_video", recording.hasVideo() ? 1 : 0);
        cv.put("recorded_ts", recording.getRecordedTimestamp());

        long id = db.update("tbl_sending_request_recording", cv, "recording_id = " + recording.getId(), null);
        if(id < 0) {
            throw new SQLException("Unable to update recording!");
        }
    }

    public static void insertOrUpdate(SQLiteDatabase db, Recording recording) throws  SQLException {
        if(recording.getId() <= 0) {
            insert(db, recording);
            return;
        }
        update(db, recording);
    }

    public static Recording get(SQLiteDatabase db, long id) {
        Recording recording = null;
        Cursor cursor = db.rawQuery("SELECT * FROM tbl_sending_request_recording WHERE recording_id = " + id, null);
        if(cursor != null && cursor.moveToFirst()) {
            recording = getFromCursor(cursor);
        }
        return recording;
    }

    private long mId;
    private String mFilePath;
    private long mDurationMillis;
    private float mSizeKb;
    private String mRecordedTimestamp;
    private boolean mHasVideo = false;

    public Recording() {
    }

    public Recording(String filePath) {
        this.mFilePath = filePath;
    }

    public Recording(String filePath, long durationMillis, long sizeKb) {
        this.mFilePath = filePath;
        this.mDurationMillis = durationMillis;
        this.mSizeKb = sizeKb;
    }

    public Recording(String filePath, long durationMillis, float sizeKb, boolean hasVideo) {
        this.mFilePath = filePath;
        this.mDurationMillis = durationMillis;
        this.mSizeKb = sizeKb;
        this.mHasVideo = hasVideo;
    }

    public File getFile() {
        return new File(mFilePath);
    }

    public File getValidatedFile() {
        File file = new File(mFilePath);
        if(file.exists() && file.canRead()) {
            return file;
        }
        return null;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String mFilePath) {
        this.mFilePath = mFilePath;
    }

    public long getDurationMillis() {
        return mDurationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.mDurationMillis = durationMillis;
    }

    public float getSizeKb() {
        return mSizeKb;
    }

    public void setSizeKb(float mSizeKb) {
        this.mSizeKb = mSizeKb;
    }

    public boolean hasVideo() {
        return mHasVideo;
    }

    public void setHasVideo(boolean mHasVideo) {
        this.mHasVideo = mHasVideo;
    }

    public String getRecordedTimestamp() {
        return mRecordedTimestamp;
    }

    public void setRecordedTimestamp(String mRecordedTimestamp) {
        this.mRecordedTimestamp = mRecordedTimestamp;
    }
}
