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
import com.peppermint.app.tracking.TrackerManager;
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
 * Extended {@link MediaRecorder} that allows to pause/resume audio recordings ({@link MediaRecorder} only allows start/stop).<br />
 * Uses an external library in C through NDK + the isoparser/mp4parser library to merge and encode the final mp4/aac file.<br />
 * It also employs a progressive gain algorithm to increase the recorded sound volume. This increases the volume for devices that
 * have low gain values for their microphones.
 */
public class ExtendedAudioRecorder {

    public interface Listener {
        void onStart(String filePath, long durationInMillis, float sizeKbs, int amplitude);
        void onPause(String filePath, long durationInMillis, float sizeKbs, int amplitude);
        void onResume(String filePath, long durationInMillis, float sizeKbs, int amplitude);
        void onStop(String filePath, long durationInMillis, float sizeKbs, int amplitude);
        void onError(String filePath, long durationInMillis, float sizeKbs, int amplitude, Throwable t);
    }

    private static final String TAG = ExtendedAudioRecorder.class.getSimpleName();
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'_'HH-mm-ss");

    private static final int SAMPLE_SIZE = 1024;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final float MAX_GAIN = 5f;
    private static final int MAX_EMPTY_ITERATIONS = 10;

    // current recording context variables
    private static final String DEFAULT_FILE_PREFIX = "Record";
    private String mFilePrefix = DEFAULT_FILE_PREFIX;

    private String mFilePath;           // final AAC file with encoded audio
    private String mTempFilePath;       // temporary PCM file with raw audio
    private long mFullDuration = 0;
    private float mFullSize = 0;
    private int mAmplitude = 0;

    // external event listener
    private Listener mListener;

    // native AAC encoder
    private AACEncoder mEncoder = new AACEncoder();

    // state related
    private boolean mRecording = false;
    private boolean mPaused = false;
    private boolean mDiscard = false;
    private long mMaxDuration = -1;

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
                    mListener.onError(mFilePath, mFullDuration, mFullSize, mAmplitude, error);
                } else {
                    mListener.onStop(mFilePath, mFullDuration, mFullSize, mAmplitude);
                }
            }
        }
    };

    public Object[] findAudioRecord() {
        // 44100 increases the record size significantly; to remove or not to remove?
        int[] rates = new int[] {16000, 44100, 22050, 11025, 8000};
        for (int rate : rates) {  // add the rates you wish to check against
            try {
                int minBufferSize = AudioRecord.getMinBufferSize(rate, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
                if (minBufferSize > 0) {
                    // check if we can instantiate and have a success
                    // careful with the buffer size! it seems to mess up the duration of the recording (gets slowed down in some cases)
                    AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, RECORDER_CHANNELS,
                            RECORDER_AUDIO_ENCODING, minBufferSize * 5);

                    if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                        Log.d(TAG, "Picked AudioRecord Rate = " + rate + "; MinBufferSize = " + minBufferSize + "; Channels = " + recorder.getChannelCount() + "; Encoding = " + recorder.getAudioFormat());
                        return new Object[]{recorder, rate};
                    } else {
                        recorder.release();
                    }
                }
            } catch(Throwable t) {
                TrackerManager.getInstance(mContext.getApplicationContext()).logException(t);
                Log.w(TAG, "Error obtaining AudioRecord instance!", t);
            }
        }

        return null;
    }

    private Runnable mRecorderRunnable = new Runnable() {
        public void run() {
            AudioRecord recorder = null;
            Throwable error = null;

            synchronized (ExtendedAudioRecorder.this) {
                try {
                    Object[] data = findAudioRecord();
                    recorder = (AudioRecord) data[0];
                    recorder.startRecording();

                    writeAudioDataToFile(recorder, (int) data[1]);
                } catch (Throwable t) {
                    // keep the error to send it to the external listener
                    error = t;
                    Log.e(TAG, t.getMessage(), t);
                } finally {
                    mRecording = false;
                    if (recorder != null) {
                        try {
                            recorder.stop();
                        } catch (Throwable t) {
                            Log.w(TAG, "Error stopping recorder after exception occurred!", t);
                        }
                        recorder.release();
                    }
                }

                if (mDiscard || error != null) {
                    if (!discardAll()) {
                        Log.w(TAG, "Unable to discard all created audio files!");
                    }
                } else if (!mPaused) {
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

                if (mListener != null) {
                    if (error != null) {
                        if(!(error instanceof NoMicDataIOException)) {
                            TrackerManager.getInstance(mContext.getApplicationContext()).logException(error);
                        }
                        mListener.onError(mFilePath, mFullDuration, mFullSize, mAmplitude, error);
                    } else {
                        if (mPaused) {
                            mListener.onPause(mFilePath, mFullDuration, mFullSize, mAmplitude);
                        } else {
                            mListener.onStop(mFilePath, mFullDuration, mFullSize, mAmplitude);
                        }
                    }
                }
            }
        }
    };

    public ExtendedAudioRecorder(Context context) {
        this.mContext = context;
    }

    public ExtendedAudioRecorder(Context context, String filePrefix) {
        this(context);
        this.mFilePrefix = filePrefix;
    }

    public boolean isPaused() {
        return mPaused;
    }

    public void start(long maxDurationMillis) throws NoAccessToExternalStorageException {
        if(mRecording || mPaused) {
            throw new RuntimeException("Already recording or paused. Use pause, resume or stop.");
        }

        if(mContext.getExternalFilesDir(null) == null) {
            throw new NoAccessToExternalStorageException("No access to external storage directory!");
        }

        synchronized (this) {
            Calendar now = Calendar.getInstance();
            mFilePath = mContext.getExternalFilesDir(null).getAbsolutePath() + "/" + mFilePrefix + "_" +
                    DATETIME_FORMAT.format(now.getTime()) + ".m4a";
            mTempFilePath = mContext.getExternalFilesDir(null).getAbsolutePath() + "/" + mFilePrefix + "_" +
                    DATETIME_FORMAT.format(now.getTime()) + ".aac";
            mPaused = false;
            mFullDuration = 0;
            mFullSize = 0;
            mAmplitude = 0;
            mDiscard = false;
            mMaxDuration = maxDurationMillis;

            startRecording(false);
        }
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

    private double[] getBufferData(short[] data, int indexFrom, int length) {
        double[] result = new double[3];    // 0 mean amplitude; 1 max amplitude abs
        if(length <= 0) {
            return result;
        }

        double sum = 0, maxAbs = 0;
        for(int i=indexFrom; i<indexFrom + length; i++) {
            sum += data[i] * data[i];
            double abs = Math.abs(data[i]);
            if(abs > maxAbs) {
                maxAbs = abs;
            }
        }

        result[0] = (int) Math.sqrt(sum/(double)length);
        result[1] = maxAbs;
        result[2] = ((float) Short.MAX_VALUE / 2f) / (float) maxAbs;

        if (result[2] < 1) {
            result[2] = 1;
        }
        if (result[2] > MAX_GAIN) {
            result[2] = MAX_GAIN;
        }

        return result;
    }

    private void writeAudioDataToFile(AudioRecord recorder, int sampleRate) throws IOException {
        // check if the output file exists
        File file = new File(mTempFilePath);
        if(!file.exists()) {
            file.createNewFile();
        }

        // initialize native AAC encoder
        float bitrate = 16 * sampleRate;
        short dataOld = 0;
        mEncoder.init((int) bitrate, 1, sampleRate, 16, mTempFilePath);

        short sData[] = new short[SAMPLE_SIZE];
        byte bData[] = new byte[SAMPLE_SIZE * 2];

        // bo code to ignore the first 350ms to avoid noises and button sounds
        final int ignoreTimeMs = 350;
        final int ignoreShortAmount = (int) ((float) sampleRate * ((float) ignoreTimeMs / 1000f));
        int ignoredShortsLeft = ignoreShortAmount;
        int emptyIts = 0;
        while(ignoredShortsLeft > 0 && emptyIts < MAX_EMPTY_ITERATIONS) {
            ignoredShortsLeft = ignoredShortsLeft - recorder.read(sData, 0, ignoredShortsLeft > SAMPLE_SIZE ? SAMPLE_SIZE : ignoredShortsLeft);
            emptyIts++;
        }
        // eo code to ignore the first 350ms to avoid noises and button sounds

        long now = android.os.SystemClock.uptimeMillis();
        emptyIts = 0;
        int totalRead = 0;
        double totalMax = 0;

        while (mRecording && !(totalRead <= 0 && emptyIts >= MAX_EMPTY_ITERATIONS) && !(totalMax <= 0 && emptyIts >= MAX_EMPTY_ITERATIONS)) {
            // gets the voice output from microphone to short format
            int numRead = recorder.read(sData, 0, SAMPLE_SIZE);
            double[] bufferData = getBufferData(sData, 0, SAMPLE_SIZE);
            totalMax += bufferData[1];
            totalRead += numRead;

            mAmplitude = (int) bufferData[0];
            float gain = (float) bufferData[2];

            //Log.d(TAG, "Amplitude = " + mAmplitude + "; Max = " + maxAbs + "; Gain = " + gainNew + "; NumRead = " + numRead);
            for (int i = 0; i < numRead; i++) {
                sData[i] = (short) Math.min((int) (sData[i] * gain), (int) Short.MAX_VALUE);

                // filter discontinuity if new gain is lower than the old one
                float prevData = i > 0 ? sData[i - 1] : (dataOld != 1 ? dataOld : sData[i]);
                sData[i] = (short) (prevData + (.25f * (sData[i] - prevData)));
            }

            if(numRead > 0 && bufferData[1] > 0) {
                dataOld = sData[numRead - 1];
            } else {
                emptyIts++;
            }

            // encode in AAC using the native encoder
            Utils.short2Byte(sData, bData);
            mEncoder.encode(bData);

            // calculate duration
            long cicleNow = android.os.SystemClock.uptimeMillis();
            mFullDuration += cicleNow - now;
            mFullSize = bitrate / 8f * (mFullDuration / 1000f);
            now = cicleNow;

            // exit if max duration has been exceeded
            if(mMaxDuration > 0 && mFullDuration > mMaxDuration) {
                mDiscard = false;
                stopRecording();
            }
        }

        // close the output file stream present in the native encoder
        mEncoder.uninit();

        // either no data is received, or the data received contains no sound (not even noise)
        if((emptyIts >= MAX_EMPTY_ITERATIONS && totalRead <= 0) || (totalMax <= 0 && emptyIts >= MAX_EMPTY_ITERATIONS)) {
            throw new NoMicDataIOException();
        }
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
                mListener.onResume(mFilePath, mFullDuration, mFullSize, mAmplitude);
            } else {
                mListener.onStart(mFilePath, mFullDuration, mFullSize, mAmplitude);
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

    public float getFullSize() {
        return mFullSize;
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
