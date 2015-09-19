package com.peppermint.app.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Nuno Luz on 14-09-2015.
 *
 * Extended MediaRecorder that allows to pause/resume recordings.
 * Uses an external library isoparser/mp4parser to merge all recorded parts.
 */
public class ExtendedMediaRecorder {

    private static final String TAG = ExtendedMediaRecorder.class.getSimpleName();
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'_'HH-mm-ss");

    private static final String DEFAULT_FILE_PREFIX = "Record";
    private String mFilePrefix = DEFAULT_FILE_PREFIX;

    private String mFilePath;
    private ArrayList<String> mIntermediateFilePaths;
    private long mFullDuration = 0;

    private long mCurrentStartTime = 0;
    private String mCurrentFilePath;

    private Context mContext;
    private MediaRecorder mRecorder;
    private boolean mOngoing = false;

    public ExtendedMediaRecorder(Context context) {
        this.mContext = context;
        this.mIntermediateFilePaths = new ArrayList<>();
    }

    public ExtendedMediaRecorder(Context context, String filePrefix) {
        this(context);
        this.mFilePrefix = filePrefix;
    }

    public boolean isPaused() {
        return mOngoing && mRecorder == null;
    }

    /**
     * Start a recording.
     */
    public void start() {
        if(mOngoing) {
            throw new RuntimeException("Already recording. Available actions are pause, resume and stop.");
        }
        mOngoing = true;
        startRecording();
    }

    public long pause() {
        if(!mOngoing || mRecorder == null) {
            throw new RuntimeException("Cannot pause. Nothing is currently being recorded. Use start.");
        }
        return stopRecording();
    }

    public void resume() {
        if(!mOngoing) {
            throw new RuntimeException("Cannot resume. Nothing is currently being recorded. Use start.");
        }
        if(mRecorder != null) {
            throw new RuntimeException("Cannot resume. Must be paused in order to do so.");
        }
        startRecording();
    }

    /**
     * Stop the recording with the specified UUID.
     */
    public long stop() throws Exception {
        long currentDuration = stopRecording();
        mergeFiles();
        mOngoing = false;
        return currentDuration;
    }

    public boolean discardIntermediate() {
        if(mOngoing) {
            stopRecording();
            mOngoing = false;
        }

        boolean allDeleted = true;
        for(String filePath : mIntermediateFilePaths) {
            File f = new File(filePath);
            if(f.exists()) {
                allDeleted = f.delete() && allDeleted;
            }
        }
        return allDeleted;
    }

    public boolean discard() {
        boolean discarded = discardIntermediate();
        if(mFilePath != null) {
            File file = new File(mFilePath);
            return discarded && file.delete();
        }
        return discarded;
    }

    protected void startRecording() {
        if(mContext.getExternalCacheDir() == null) {
            throw new NullPointerException("No access to external cache directory!");
        }

        Calendar now = Calendar.getInstance();
        mCurrentFilePath = mContext.getExternalCacheDir().getAbsolutePath() + "/" + mFilePrefix + "_" +
                DATETIME_FORMAT.format(now.getTime()) + "_" + mIntermediateFilePaths.size() + ".mp3";

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mCurrentFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mCurrentStartTime = System.currentTimeMillis();
        mRecorder.start();
    }

    protected long stopRecording() {
        if(mRecorder != null) {
            mRecorder.stop();

            long currentDuration = System.currentTimeMillis() - mCurrentStartTime;
            mFullDuration += currentDuration;
            mIntermediateFilePaths.add(mCurrentFilePath);

            mRecorder.release();
            mRecorder = null;
            mCurrentStartTime = 0;
            mCurrentFilePath = null;

            return currentDuration;
        }
        return 0;
    }

    // https://stackoverflow.com/questions/23129561/how-to-concat-mp4-files/23144266#23144266
    // https://github.com/sannies/mp4parser
    private void mergeFiles() throws Exception {
        if(mIntermediateFilePaths == null || mIntermediateFilePaths.size() <= 0) {
            return;
        }

        if(mContext.getExternalCacheDir() == null) {
            throw new NullPointerException("No access to external cache directory!");
        }

        //List<Track> videoTracks = new LinkedList<Track>();
        List<Track> audioTracks = new LinkedList<>();

        for(String filePath : mIntermediateFilePaths) {
            Movie m = MovieCreator.build(filePath);
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
                /*if (t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                }*/
            }
        }

        Movie result = new Movie();
        /*if (videoTracks.size() > 0) {
            result.addTrack(new AppendTrack(videoTracks
                    .toArray(new Track[videoTracks.size()])));
        }*/
        if (audioTracks.size() > 0) {
            result.addTrack(new AppendTrack(audioTracks
                    .toArray(new Track[audioTracks.size()])));
        }

        Calendar now = Calendar.getInstance();
        String newFilePath = mContext.getExternalCacheDir().getAbsolutePath() + "/" + mFilePrefix + "_" +
                DATETIME_FORMAT.format(now.getTime()) + ".mp3";

        Container out = new DefaultMp4Builder().build(result);
        RandomAccessFile ram = new RandomAccessFile(newFilePath, "rw");
        FileChannel fc = ram.getChannel();
        out.writeContainer(fc);
        ram.close();
        fc.close();

        discardIntermediate();
        mFilePath = newFilePath;
    }

    public boolean isOngoing() {
        return mOngoing;
    }

    public boolean isActuallyRecording() {
        return mRecorder != null;
    }

    public String getFilePrefix() {
        return mFilePrefix;
    }

    public void setFilePrefix(String mFilePrefix) {
        this.mFilePrefix = mFilePrefix;
    }

    public ArrayList<String> getIntermediateFilePaths() {
        return mIntermediateFilePaths;
    }

    public long getFullDuration() {
        return mFullDuration;
    }

    public long getCurrentStartTime() {
        return mCurrentStartTime;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public String getCurrentFilePath() {
        return mCurrentFilePath;
    }

    public int getMaxAmplitude() {
        return mRecorder == null ? 0 : mRecorder.getMaxAmplitude();
    }
}
