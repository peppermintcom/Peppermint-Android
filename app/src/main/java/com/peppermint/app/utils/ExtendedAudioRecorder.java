package com.peppermint.app.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.todoroo.aacenc.AACEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Nuno Luz on 14-09-2015.
 *
 * Extended MediaRecorder that allows to pause/resume audio recordings.
 * Uses an external library in C through NDK + isoparser/mp4parser to merge encode the final mp4/aac file.
 * It also employs a progressive gain algorithm to increase the recorded sound volume.
 */
public class ExtendedAudioRecorder {

    public interface Listener {
        void onStart(String filePath, long durationInMillis, int amplitude);
        void onPause(String filePath, long durationInMillis, int amplitude);
        void onResume(String filePath, long durationInMillis, int amplitude);
        void onStop(String filePath, long durationInMillis, int amplitude);
        void onError(String filePath, long durationInMillis, int amplitude, Throwable t);
    }

    private static final String TAG = ExtendedAudioRecorder.class.getSimpleName();
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'_'HH-mm-ss");

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final float MAX_GAIN = 1.3f;

    // current recording context variables
    private static final String DEFAULT_FILE_PREFIX = "Record";
    private String mFilePrefix = DEFAULT_FILE_PREFIX;

    private String mFilePath;           // final AAC file with encoded audio
    private String mTempFilePath;       // temporary PCM file with raw audio
    private long mFullDuration = 0;
    private int mAmplitude = 0;

    // external event listener
    private Listener mListener;

    // native AAC encoder
    private AACEncoder mEncoder = new AACEncoder();

    // state related
    private boolean mRecording = false;
    private boolean mPaused = false;
    private boolean mDiscard = false;

    private Context mContext;

    // recorder thread
    private Thread mRecorderThread;
    private Runnable mRecorderPauseToStopRunnable = new Runnable() {
        @Override
        public void run() {
            Throwable error = null;
            try {
                encodeFile();
            } catch (Throwable t) {
                error = t;
                Log.e(TAG, t.getMessage(), t);
            }

            // thread is finishing...
            mRecorderThread = null;

            if(mListener != null) {
                if (error != null) {
                    mListener.onError(mFilePath, mFullDuration, mAmplitude, error);
                } else {
                    mListener.onStop(mFilePath, mFullDuration, mAmplitude);
                }
            }
        }
    };
    private Runnable mRecorderRunnable = new Runnable() {
        public void run() {
            AudioRecord recorder = null;
            Throwable error = null;

            try {
                int sampleRate = getBestValidSampleRate();
                // try to release the obtained audiorecord, just in case...
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRate, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING, bufferElements2Rec * bytesPerElement);
                recorder.release();

                // start recording
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRate, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING, bufferElements2Rec * bytesPerElement);
                recorder.startRecording();

                writeAudioDataToFile(recorder, sampleRate);
            } catch (Throwable t) {
                // keep the error to send it to the external listener
                error = t;
                Log.e(TAG, t.getMessage(), t);
            } finally {
                if(recorder != null) {
                    try {
                        recorder.stop();
                    } catch(Throwable t) { Log.w(TAG, "Error stopping recorder after exception occurred!", t); }
                    recorder.release();
                }
            }

            if(mDiscard || error != null) {
                if(!discardAll()) {
                    Log.w(TAG, "Unable to discard all created audio files!");
                }
            } else if(!mPaused) {
                // if the recording was stopped/finished, encode the PCM file to AAC
                try {
                    encodeFile();
                } catch (Throwable t) {
                    error = t;
                    Log.e(TAG, t.getMessage(), t);
                }
            }

            // thread is finishing...
            mRecorderThread = null;

            if(mListener != null) {
                if (error != null) {
                    mListener.onError(mFilePath, mFullDuration, mAmplitude, error);
                } else {
                    if(mPaused) {
                        mListener.onPause(mFilePath, mFullDuration, mAmplitude);
                    } else {
                        mListener.onStop(mFilePath, mFullDuration, mAmplitude);
                    }
                }
            }
        }
    };

    private final int bufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private final int bytesPerElement = 2; // 2 bytes in 16bit format

    public ExtendedAudioRecorder(Context context) {
        this.mContext = context;
    }

    public ExtendedAudioRecorder(Context context, String filePrefix) {
        this(context);
        this.mFilePrefix = filePrefix;
    }

    private int getBestValidSampleRate() {
        // 44100 increases the record size significantly
        for (int rate : new int[] {/*44100, */22050, 16000, 11025, 8000}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
            if (bufferSize > 0) {
                return rate;
            }
        }
        return RECORDER_SAMPLERATE;
    }

    public boolean isPaused() {
        return mPaused;
    }

    public void start() {
        if(mRecording || mPaused) {
            throw new RuntimeException("Already recording or paused. Use pause, resume or stop.");
        }

        if(mContext.getExternalCacheDir() == null) {
            throw new NullPointerException("No access to external cache directory!");
        }

        Calendar now = Calendar.getInstance();
        mFilePath = mContext.getExternalCacheDir().getAbsolutePath() + "/" + mFilePrefix + "_" +
                DATETIME_FORMAT.format(now.getTime()) + ".mp3";
        mTempFilePath = mContext.getExternalCacheDir().getAbsolutePath() + "/" + mFilePrefix + "_" +
                DATETIME_FORMAT.format(now.getTime()) + ".pcm";
        mPaused = false;
        mFullDuration = 0;
        mAmplitude = 0;
        mDiscard = false;

        startRecording(false);
    }

    public void pause() {
        if(!mRecording) {
            throw new RuntimeException("Cannot pause. Nothing is currently being recorded. Use start or resume.");
        }
        mPaused = true;
        stopRecording();
    }

    public void resume() {
        if(!mPaused) {
            throw new RuntimeException("Cannot resume. Must be paused in order to do so.");
        }
        mPaused = false;
        startRecording(true);
    }

    public void stop(boolean discard) {
        boolean wasPaused = mPaused;
        mPaused = false;
        mDiscard = discard;

        stopRecording();

        // if it was paused, the thread is not running
        // this triggers the onStop method on listener and the final file encode routine
        if(wasPaused) {
            mRecorderThread = new Thread(mRecorderPauseToStopRunnable);
            mRecorderThread.start();
        }
    }

    private boolean discardTemp() {
        if(mTempFilePath != null) {
            File file = new File(mTempFilePath);
            if(file.exists()) {
                return file.delete();
            }
        }
        return false;
    }

    private boolean discardAll() {
        boolean tmp = discardTemp();
        if(mFilePath != null) {
            File file = new File(mFilePath);
            if(file.exists()) {
                tmp = file.delete() && tmp;
            }
        }
        return tmp;
    }

    private void writeAudioDataToFile(AudioRecord recorder, int sampleRate) throws IOException {
        short sData[] = new short[bufferElements2Rec];

        // check if the output file exists
        File file = new File(mTempFilePath);
        if(!file.exists()) {
            file.createNewFile();
        }

        // initialize native AAC encoder
        float gainOld = 1.0f, bitrate = 16 * sampleRate;
        //Log.d(TAG, "Sample Rate = " + sampleRate + " Bitrate = " + bitrate);
        mEncoder.init((int) bitrate, 1, sampleRate, 16, mTempFilePath);

        long now = android.os.SystemClock.uptimeMillis();
        while (mRecording) {
            // gets the voice output from microphone to short format
            int numRead = recorder.read(sData, 0, bufferElements2Rec);
            double sum = 0;
            for(int i=0; i<numRead; i++) {
                sum += sData[i] * sData[i];
            }
            if(numRead > 0) {
                mAmplitude = (int) Math.sqrt(sum/(double)numRead);
            } else {
                mAmplitude = 0;
            }

            // apply gain to read buffer data
            float gainNew = MAX_GAIN - ((float) Math.abs(mAmplitude) / (float) Short.MAX_VALUE);
            if(gainNew < 1) {
                gainNew = 1;
            }
            if(gainNew > MAX_GAIN) {
                gainNew = MAX_GAIN;
            }
            for(int i=0; i<numRead; i++) {
                float gain = gainOld + ((gainNew - gainOld) * 0.5f * (1f - (float) Math.cos(Math.PI * (float) i / (float) numRead)));
                sData[i] = (short)Math.min((int)(sData[i] * gain), (int)Short.MAX_VALUE);
            }
            gainOld = gainNew;

            // encode in AAC using the native encoder
            byte bData[] = Utils.short2Byte(sData);
            mEncoder.encode(bData);

            // calculate duration
            long cicleNow = android.os.SystemClock.uptimeMillis();
            mFullDuration += cicleNow - now;
            now = cicleNow;
        }

        // close the output file stream present in the native encoder
        mEncoder.uninit();
    }

    protected boolean startRecording(boolean isResume) {
        if(mRecording) {
            return false;
        }

        mRecording = true;
        mRecorderThread = new Thread(mRecorderRunnable);
        mRecorderThread.start();

        if(mListener != null) {
            if(isResume) {
                mListener.onResume(mFilePath, mFullDuration, mAmplitude);
            } else {
                mListener.onStart(mFilePath, mFullDuration, mAmplitude);
            }
        }

        return true;
    }

    protected void stopRecording() {
        // triggers the recording activity to stop
        mRecording = false;
    }

    // http://stackoverflow.com/questions/11291942/audiorecord-with-gain-adjustment-not-working-on-samsung-device
    // https://github.com/timsu/android-aac-enc
    // https://stackoverflow.com/questions/23129561/how-to-concat-mp4-files/23144266#23144266
    // https://github.com/sannies/mp4parser
    private void encodeFile() throws Exception {
        Movie movie = new Movie();

        AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(mTempFilePath));
        movie.addTrack(aacTrack);

        Container mp4file = new DefaultMp4Builder().build(movie);
        FileChannel fc = new FileOutputStream(new File(mFilePath)).getChannel();
        mp4file.writeContainer(fc);
        fc.close();

        discardTemp();
    }

    public boolean isRecording() {
        return mRecording;
    }

    public String getFilePrefix() {
        return mFilePrefix;
    }

    public void setFilePrefix(String mFilePrefix) {
        this.mFilePrefix = mFilePrefix;
    }

    public long getFullDuration() {
        return mFullDuration;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public int getAmplitude() {
        return mAmplitude;
    }

    public Listener getListener() {
        return mListener;
    }

    public void setListener(Listener mListener) {
        this.mListener = mListener;
    }
}
