package com.peppermint.app.services.recorder;

import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.recording.Recording;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 16-03-2016.
 *
 * Event associated with the recording process of the {@link RecordService}.
 */
public class RecorderEvent implements Serializable {

    public static final int EVENT_START = 1;
    public static final int EVENT_RESUME = 2;
    public static final int EVENT_PAUSE = 3;
    public static final int EVENT_STOP = 4;
    public static final int EVENT_LOUDNESS = 5;
    public static final int EVENT_ERROR = 6;

    public static final int EVENT_TRANSCRIPTION = 7;

    // intermediate process data
    private float mLoudness;

    // final relevant data
    private Recording mRecording;
    private Chat mChat;

    // event data
    private int mType;
    private Throwable mError;

    public RecorderEvent(int type, Recording recording, Chat chat, float loudness, Throwable error) {
        this.mLoudness = loudness;
        this.mType = type;
        this.mChat = chat;
        this.mRecording = recording;
        this.mError = error;
    }

    public float getLoudness() {
        return mLoudness;
    }

    public Recording getRecording() {
        return mRecording;
    }

    public int getType() {
        return mType;
    }

    public Chat getChat() {
        return mChat;
    }

    public Throwable getError() {
        return mError;
    }

    @Override
    public String toString() {
        return "RecorderEvent{" +
                "mType=" + mType +
                ", mLoudness=" + mLoudness +
                ", mRecording=" + mRecording +
                ", mChat=" + mChat +
                ", mError=" + mError +
                '}';
    }
}
