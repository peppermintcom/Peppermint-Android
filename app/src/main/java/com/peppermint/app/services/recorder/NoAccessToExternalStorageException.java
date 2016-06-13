package com.peppermint.app.services.recorder;

/**
 * Created by Nuno Luz on 10-01-2016.
 */
public class NoAccessToExternalStorageException extends Exception {
    public NoAccessToExternalStorageException(String detailMessage) {
        super(detailMessage);
    }
}
