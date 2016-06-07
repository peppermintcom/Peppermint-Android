package com.peppermint.app.cloud.apis.speech;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Base64OutputStream;

import com.peppermint.app.cloud.rest.HttpRequestFileData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 02-07-2015.
 *
 * <p>
 *     Represents an HttpRequest that sends a file in the request body.
 * </p>
 */
public class SpeechApiHttpRequest extends HttpRequestFileData implements Parcelable {

    private static final String SPEECH_HTTPS_URL = "https://speech.googleapis.com/v1/speech:recognize?key=AIzaSyDHIurMNCMvOsuhWtVngBx3CdxasALDSjc";

    private String mDetectedTranscriptionLanguage;

    public SpeechApiHttpRequest(File file) {
        super(SPEECH_HTTPS_URL, METHOD_POST, false, file);
        removeUrlParam("_ts");
    }

    @Override
    protected long getContentLength() {
        return -1;
    }

    public void writeBody(OutputStream outStream) throws IOException {
        FileInputStream mintInStream = new FileInputStream(mFile);

        byte[] buffer = new byte[4];
        if(mintInStream.read(buffer) != 4) {
            mintInStream.close();
            throw new IOException("Invalid Peppermint File!");
        }
        int sampleRate = ByteBuffer.wrap(buffer).getInt();

        if(mintInStream.read(buffer) != 4) {
            mintInStream.close();
            throw new IOException("Invalid Peppermint File!");
        }
        int transcriptionLanguageLen = ByteBuffer.wrap(buffer).getInt();

        buffer = new byte[transcriptionLanguageLen];
        if(mintInStream.read(buffer) != transcriptionLanguageLen) {
            mintInStream.close();
            throw new IOException("Invalid Peppermint File!");
        }
        mDetectedTranscriptionLanguage = new String(buffer, "UTF-8");

        String firstPart = "{ \"initialRequest\": {\"continuous\": false, \"encoding\":\"LINEAR16\", \"sampleRate\":" + sampleRate + ", \"languageCode\": \"" + mDetectedTranscriptionLanguage + "\" }, \"audioRequest\": {\"content\":\"";
        outStream.write(firstPart.getBytes("UTF-8"));
        outStream.flush();

        super.writeInputStream(new Base64OutputStream(outStream, Base64.NO_WRAP|Base64.NO_PADDING), mintInStream);

        String lastPart = "\"} }";
        outStream.write(lastPart.getBytes("UTF-8"));
        outStream.flush();

        mintInStream.close();
    }

    public String getDetectedTranscriptionLanguage() {
        return mDetectedTranscriptionLanguage;
    }

    public static final Creator<SpeechApiHttpRequest> CREATOR = new Creator<SpeechApiHttpRequest>() {
        public SpeechApiHttpRequest createFromParcel(Parcel in) { return new SpeechApiHttpRequest(in); }
        public SpeechApiHttpRequest[] newArray(int size) {
            return new SpeechApiHttpRequest[size];
        }
    };

    protected SpeechApiHttpRequest(Parcel in) {
        super(in);
    }
}

