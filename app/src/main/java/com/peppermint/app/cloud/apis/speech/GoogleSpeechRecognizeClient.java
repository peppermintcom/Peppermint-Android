package com.peppermint.app.cloud.apis.speech;

import android.content.Context;
import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.nano.AudioRequest;
import com.google.cloud.speech.v1.nano.InitialRecognizeRequest;
import com.google.cloud.speech.v1.nano.RecognizeRequest;
import com.google.cloud.speech.v1.nano.SpeechGrpc;
import com.peppermint.app.cloud.senders.exceptions.NoInternetConnectionException;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.auth.ClientAuthInterceptor;
import io.grpc.okhttp.NegotiationType;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

/**
 * Created by Nuno Luz on 19-05-2016.
 *
 * Client code for streaming audio and retrieving Google Speech API recognitions using gRPC.
 */
public class GoogleSpeechRecognizeClient {

    public interface RecognitionListener {
        void onRecognitionStarted(GoogleSpeechRecognizeClient client);
        void onRecognitionFinished(GoogleSpeechRecognizeClient client, com.google.cloud.speech.v1.nano.RecognizeResponse lastResponse);
        void onRecognitionError(GoogleSpeechRecognizeClient client, Status status, com.google.cloud.speech.v1.nano.RecognizeResponse lastResponse);
    }

    private static final String TAG = GoogleSpeechRecognizeClient.class.getSimpleName();

    private static final List<String> OAUTH2_SCOPES =
            Arrays.asList("https://www.googleapis.com/auth/cloud-platform");

    private String mId;
    private Context mContext;

    private ManagedChannel mManagedChannel;
    private SpeechGrpc.SpeechStub mStub;
    private StreamObserver<RecognizeRequest> mStreamObserver;
    private RecognitionListener mRecognitionListener;

    private BlockingQueue<byte[]> mAudioDataQueue;
    private int mEncoding, mSampleRate;
    private Future<?> mSenderFuture;
    private ThreadPoolExecutor mThreadPoolExecutor;
    private Runnable mSenderRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                mAudioDataQueue = new LinkedBlockingQueue<>();

                GoogleCredentials creds = GoogleCredentials.fromStream(mContext.getAssets().open("peppermint-5a6159bab682.json"));
                creds = creds.createScoped(OAUTH2_SCOPES);

                mManagedChannel = OkHttpChannelBuilder.forAddress("speech.googleapis.com", 443)
                        .negotiationType(NegotiationType.TLS)
                        // forcefully disable SSLv3 (mandatory for Android < v5)
                        .sslSocketFactory(new NoSSLv3SocketFactory())
                        .intercept(new ClientAuthInterceptor(creds, Executors.newSingleThreadExecutor()))
                        .build();
                mStub = SpeechGrpc.newStub(mManagedChannel);
                mStreamObserver = mStub.recognize(new RecognizeStreamObserver());

                // build and send a RecognizeRequest containing the parameters for processing the audio
                InitialRecognizeRequest initial = new InitialRecognizeRequest();
                initial.encoding = mEncoding; // InitialRecognizeRequest.LINEAR16;
                initial.sampleRate = mSampleRate;
                initial.interimResults = true;
                initial.languageCode = Utils.toBcp47LanguageTag(Locale.getDefault());

                RecognizeRequest firstRequest = new RecognizeRequest();
                firstRequest.initialRequest = initial;
                mStreamObserver.onNext(firstRequest);

                if(mRecognitionListener != null) {
                    mRecognitionListener.onRecognitionStarted(GoogleSpeechRecognizeClient.this);
                }
            } catch (Throwable e) {
                TrackerManager.getInstance(mContext).logException(e);
                finishReceiving(null, e);
                return;
            }

            try {
                while(!mFinished) {
                    try {
                        final byte[] bytes = mAudioDataQueue.take();
                        AudioRequest audio = new AudioRequest();
                        audio.content = bytes;
                        RecognizeRequest request = new RecognizeRequest();
                        request.audioRequest = audio;
                        mStreamObserver.onNext(request);
                    } catch (InterruptedException e) {
                        /* nothing to do here */
                    }
                }
            } catch (RuntimeException e) {
                mStreamObserver.onError(e);
            }

            // mark the end of requests
            mStreamObserver.onCompleted();

            if(mAudioDataQueue != null) {
                mAudioDataQueue.clear();
                mAudioDataQueue = null;
            }
        }
    };

    private boolean mFinished = true;

    private class RecognizeStreamObserver implements StreamObserver<com.google.cloud.speech.v1.nano.RecognizeResponse> {
        private com.google.cloud.speech.v1.nano.RecognizeResponse mLastResponse;

        @Override
        public void onNext(com.google.cloud.speech.v1.nano.RecognizeResponse response) {
            mLastResponse = response;
        }

        @Override
        public void onError(Throwable error) {
            finishReceiving(mLastResponse, error);
        }

        @Override
        public void onCompleted() {
            finishReceiving(mLastResponse, null);
        }
    }

    public GoogleSpeechRecognizeClient(final Context context, String id) {
        this.mContext = context;
        this.mId = id;
        this.mThreadPoolExecutor = new ThreadPoolExecutor(1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    public void startSending(int encoding, int sampleRate) throws NoInternetConnectionException {
        if(!Utils.isInternetAvailable(mContext)) {
            throw new NoInternetConnectionException();
        }

        if(!mFinished) {
            throw new IllegalStateException("There's an ongoing speech recognition! finish() or cancel() must be invoked first.");
        }

        mFinished = false;

        mEncoding = encoding;
        mSampleRate = sampleRate;

        mSenderFuture = mThreadPoolExecutor.submit(mSenderRunnable);
    }

    public void finishSending() {
        if(mFinished) {
            throw new IllegalStateException("No ongoing speech recognition! start() must be invoked first.");
        }

        mFinished = true;
        mSenderFuture.cancel(true);
    }

    private void finishReceiving(com.google.cloud.speech.v1.nano.RecognizeResponse lastResponse, Throwable error) {
        mManagedChannel.shutdown();
        if(mRecognitionListener != null) {
            if(error == null) {
                Log.d(TAG, "Recognize Completed.");
                mRecognitionListener.onRecognitionFinished(GoogleSpeechRecognizeClient.this, lastResponse);
            } else {
                final Status status = Status.fromThrowable(error);
                Log.d(TAG, "Recognize Failed: " + status);
                mRecognitionListener.onRecognitionError(GoogleSpeechRecognizeClient.this, status, lastResponse);
            }
        }
    }

    public boolean recognize(byte[] bytes) {
        if(mFinished) {
            throw new IllegalStateException("No ongoing speech recognition! start() must be invoked first.");
        }
        return mAudioDataQueue.offer(bytes);
    }

    public void setRecognitionListener(RecognitionListener mRecognitionListener) {
        this.mRecognitionListener = mRecognitionListener;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }
}
