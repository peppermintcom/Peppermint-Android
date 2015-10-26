package com.peppermint.app.utils;

import java.io.IOException;

/**
 * Created by Nuno Luz on 26-10-2015.
 * Exception thrown when no audio data from the microphone is received.
 */
public class NoMicDataIOException extends IOException {
    public NoMicDataIOException() {
    }

    public NoMicDataIOException(String detailMessage) {
        super(detailMessage);
    }

    public NoMicDataIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoMicDataIOException(Throwable cause) {
        super(cause);
    }
}
