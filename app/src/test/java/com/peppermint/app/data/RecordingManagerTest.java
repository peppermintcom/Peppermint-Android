package com.peppermint.app.data;

import android.database.sqlite.SQLiteDatabase;

import com.peppermint.app.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.sql.SQLException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Created by Nuno Luz on 24-02-2016.
 *
 * Tests for {@link RecordingManager}
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class RecordingManagerTest {

    private SQLiteDatabase mDatabase;

    @Before
    public void init() {
        DatabaseHelper helper = DatabaseHelper.getInstance(RuntimeEnvironment.application);
        mDatabase = helper.getWritableDatabase();
    }

    @After
    public void deinit() {
        mDatabase.close();
    }

    @Test
    public void testGlobal() throws SQLException {
        testInsert();
        testGetInsert();
        testUpdate();
        testGetUpdate();
        testInsertOrUpdate();
    }

    public void testInsert() throws SQLException {
        Recording recording = new Recording("file://this_is_a_file.ext", 2000, 123f, false, Recording.CONTENT_TYPE_AUDIO);
        recording.setRecordedTimestamp("2016-02-24 12:12:12");
        RecordingManager.insert(mDatabase, recording);

        assertEquals(recording.getId(), 1);
        assertEquals(recording.getFilePath(), "file://this_is_a_file.ext");
        assertEquals(recording.getDurationMillis(), 2000);
        assertEquals(recording.getSizeKb(), 123f);
        assertEquals(recording.hasVideo(), false);
        assertEquals(recording.getRecordedTimestamp(), "2016-02-24 12:12:12");
        assertEquals(recording.getContentType(), Recording.CONTENT_TYPE_AUDIO);

        recording = new Recording("file://this_is_a_file2.ext", 2000, 123f, false, Recording.CONTENT_TYPE_AUDIO);
        recording.setRecordedTimestamp("2016-02-24 12:12:12");
        RecordingManager.insert(mDatabase, recording);

        assertEquals(recording.getId(), 2);
        assertEquals(recording.getFilePath(), "file://this_is_a_file2.ext");
    }

    public void testGetInsert() throws SQLException {
        Recording recording = RecordingManager.getRecordingById(mDatabase, 1);
        assertNotNull(recording);

        assertEquals(recording.getId(), 1);
        assertEquals(recording.getFilePath(), "file://this_is_a_file.ext");
        assertEquals(recording.getDurationMillis(), 2000);
        assertEquals(recording.getSizeKb(), 123f);
        assertEquals(recording.hasVideo(), false);
        assertEquals(recording.getRecordedTimestamp(), "2016-02-24 12:12:12");
        assertEquals(recording.getContentType(), Recording.CONTENT_TYPE_AUDIO);

        recording = RecordingManager.getRecordingById(mDatabase, 2);
        assertNotNull(recording);

        assertEquals(recording.getId(), 2);
        assertEquals(recording.getFilePath(), "file://this_is_a_file2.ext");
    }

    public void testUpdate() throws SQLException {
        Recording recording = new Recording("file://this_is_a_file_update.ext", 1900, 120f, false, Recording.CONTENT_TYPE_AUDIO);
        recording.setRecordedTimestamp("2016-02-25 12:12:12");
        recording.setId(1);
        RecordingManager.update(mDatabase, recording);

        assertEquals(recording.getId(), 1);
        assertEquals(recording.getFilePath(), "file://this_is_a_file_update.ext");
        assertEquals(recording.getDurationMillis(), 1900);
        assertEquals(recording.getSizeKb(), 120f);
        assertEquals(recording.hasVideo(), false);
        assertEquals(recording.getRecordedTimestamp(), "2016-02-25 12:12:12");
        assertEquals(recording.getContentType(), Recording.CONTENT_TYPE_AUDIO);
    }

    public void testGetUpdate() throws SQLException {
        Recording recording = RecordingManager.getRecordingById(mDatabase, 1);
        assertNotNull(recording);

        assertEquals(recording.getId(), 1);
        assertEquals(recording.getFilePath(), "file://this_is_a_file_update.ext");
        assertEquals(recording.getDurationMillis(), 1900);
        assertEquals(recording.getSizeKb(), 120f);
        assertEquals(recording.hasVideo(), false);
        assertEquals(recording.getRecordedTimestamp(), "2016-02-25 12:12:12");
        assertEquals(recording.getContentType(), Recording.CONTENT_TYPE_AUDIO);
    }

    public void testInsertOrUpdate() throws SQLException {
        Recording recording = new Recording("file://this_is_a_file.ext", 1234, 12f, false, Recording.CONTENT_TYPE_AUDIO);
        recording.setRecordedTimestamp("2016-02-24 14:12:12");
        RecordingManager.insertOrUpdate(mDatabase, recording);

        assertEquals(recording.getId(), 3);
        assertEquals(recording.getFilePath(), "file://this_is_a_file.ext");
        assertEquals(recording.getDurationMillis(), 1234);
        assertEquals(recording.getSizeKb(), 12f);
        assertEquals(recording.hasVideo(), false);
        assertEquals(recording.getRecordedTimestamp(), "2016-02-24 14:12:12");
        assertEquals(recording.getContentType(), Recording.CONTENT_TYPE_AUDIO);

        recording = new Recording("file://this_is_a_file1b.ext", 2000, 123f, false, Recording.CONTENT_TYPE_AUDIO);
        recording.setRecordedTimestamp("2016-02-24 12:12:12");
        RecordingManager.insertOrUpdate(mDatabase, recording);

        assertEquals(recording.getId(), 1);
        assertEquals(recording.getFilePath(), "file://this_is_a_file1b.ext");
    }

}
