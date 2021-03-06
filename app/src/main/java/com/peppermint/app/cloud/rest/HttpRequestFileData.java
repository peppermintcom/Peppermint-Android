package com.peppermint.app.cloud.rest;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 02-07-2015.
 *
 * <p>
 *     Represents an HttpRequest that sends a file in the request body.
 * </p>
 */
public class HttpRequestFileData extends HttpRequest implements Parcelable {

    private static final String TAG = HttpRequestFileData.class.getSimpleName();

    protected ResultReceiver mListener;
    protected File mFile;

    /**
     * Copy constructor
     * @param req the {@link HttpRequestFileData to be copied}
     */
    public HttpRequestFileData(HttpRequestFileData req) {
        super(req);
        this.mFile = req.mFile;
    }

    public HttpRequestFileData(String endpoint, int requestMethod, boolean forceOnlyGetAndPost, File file) {
        super(endpoint, requestMethod, forceOnlyGetAndPost);
        this.mFile = file;
    }

    @Override
    protected long getContentLength() {
        return mFile.length();
    }

    @Override
    public String getBody() {
        // must return something != null in order to output the body
        return mFile.toString();
    }

    @Override
    public void writeBody(OutputStream outStream) throws IOException {
        FileInputStream reader = new FileInputStream(mFile);
        writeInputStream(outStream, reader);
        reader.close();
    }

    protected void writeInputStream(OutputStream outputStream, InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1023];

        int count = 0;
        long sum = 0;
        final long length = mFile.length();

        while((count = inputStream.read(buffer)) > 0 && !isCancelled()) {
            outputStream.write(buffer, 0, count);
            outputStream.flush();

            if (mListener != null) {
                sum += count;
                int percent = (int) ((sum * 100) / length);
                mListener.send(percent, null);
            }
        }
    }

    public void setUploadResultReceiver(ResultReceiver listener) {
        this.mListener = listener;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mFile.getAbsolutePath());
        if(mListener != null) {
            out.writeByte((byte) 1);
            mListener.writeToParcel(out, flags);
        } else {
            out.writeByte((byte) 0);
        }
    }

    public static final Creator<HttpRequestFileData> CREATOR = new Creator<HttpRequestFileData>() {
        public HttpRequestFileData createFromParcel(Parcel in) { return new HttpRequestFileData(in); }
        public HttpRequestFileData[] newArray(int size) {
            return new HttpRequestFileData[size];
        }
    };

    protected HttpRequestFileData(Parcel in) {
        super(in);
        mFile = new File(in.readString());
        if(in.readByte() > 0) {
            mListener = ResultReceiver.CREATOR.createFromParcel(in);
        }
    }
}

