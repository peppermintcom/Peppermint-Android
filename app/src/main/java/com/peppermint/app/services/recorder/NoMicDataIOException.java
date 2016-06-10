package com.peppermint.app.services.recorder;

import java.io.IOException;

/**
 * Created by Nuno Luz on 26-10-2015.
 * Exception thrown when no audio data from the microphone is received.
 */
public class NoMicDataIOException extends IOException {
    public NoMicDataIOException() {
    }
}
