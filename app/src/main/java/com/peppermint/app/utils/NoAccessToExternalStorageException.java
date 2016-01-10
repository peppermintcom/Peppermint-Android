package com.peppermint.app.utils;

/**
 * Created by Nuno Luz on 10-01-2016.
 */
public class NoAccessToExternalStorageException extends Exception {
    public NoAccessToExternalStorageException() {
    }

    public NoAccessToExternalStorageException(String detailMessage) {
        super(detailMessage);
    }

    public NoAccessToExternalStorageException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NoAccessToExternalStorageException(Throwable throwable) {
        super(throwable);
    }
}
