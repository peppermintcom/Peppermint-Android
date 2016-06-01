package com.peppermint.app.cloud.apis.speech;

import android.content.Context;
import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.nano.AudioRequest;
import com.google.cloud.speech.v1.nano.InitialRecognizeRequest;
import com.google.cloud.speech.v1.nano.RecognizeRequest;
import com.google.cloud.speech.v1.nano.RecognizeResponse;
import com.google.cloud.speech.v1.nano.SpeechGrpc;
import com.peppermint.app.cloud.rest.HttpJSONResponse;
import com.peppermint.app.cloud.senders.exceptions.NoInternetConnectionException;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public static class RecognizeResponseWrapper {
        private List<com.google.cloud.speech.v1.nano.RecognizeResponse> mRecognizeResponseList;
        private String mLanguageCode;

        public RecognizeResponseWrapper(List<com.google.cloud.speech.v1.nano.RecognizeResponse> recognizeResponseList, String languageCode) {
            this.mRecognizeResponseList = recognizeResponseList;
            this.mLanguageCode = languageCode;
        }

        public List<com.google.cloud.speech.v1.nano.RecognizeResponse> getRecognizeResponseList() {
            return mRecognizeResponseList;
        }

        public void setRecognizeResponseList(List<com.google.cloud.speech.v1.nano.RecognizeResponse> mRecognizeResponse) {
            this.mRecognizeResponseList = mRecognizeResponse;
        }

        public String getLanguageCode() {
            return mLanguageCode;
        }

        public void setLanguageCode(String mLanguageCode) {
            this.mLanguageCode = mLanguageCode;
        }
    }

    public interface RecognitionListener {
        void onRecognitionStarted(GoogleSpeechRecognizeClient client);
        void onRecognitionFinished(GoogleSpeechRecognizeClient client, RecognizeResponseWrapper lastResponse);
        void onRecognitionError(GoogleSpeechRecognizeClient client, Status status, RecognizeResponseWrapper lastResponse);
    }

    private static final String TAG = GoogleSpeechRecognizeClient.class.getSimpleName();

    private static final String SPEECH_CONFIG_FILE = "peppermint-5a6159bab682.json";
    private static final String SPEECH_HOST = "speech.googleapis.com";
    private static final int SPEECH_PORT = 443;

    private static final int BUFFER_SIZE = 2048;
    private static final float BITS_PER_SAMPLE = 16f;

    private static final List<String> OAUTH2_SCOPES =
            Arrays.asList("https://www.googleapis.com/auth/cloud-platform");

    private final SpeechApiHttpResponseDataParser mHttpResponseDataParser = new SpeechApiHttpResponseDataParser();

    private String mId;
    private Context mContext;

    private ManagedChannel mManagedChannel;
    private SpeechGrpc.SpeechStub mStub;
    private StreamObserver<RecognizeRequest> mStreamObserver;
    private RecognitionListener mRecognitionListener;

    private BlockingQueue<byte[]> mAudioDataQueue = new LinkedBlockingQueue<>();
    private int mEncoding, mSampleRate;
    private String mLanguageCode;
    private Future<?> mSenderFuture;
    private ThreadPoolExecutor mThreadPoolExecutor;
    private Runnable mSenderRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                GoogleCredentials creds = GoogleCredentials.fromStream(mContext.getAssets().open(SPEECH_CONFIG_FILE));
                creds = creds.createScoped(OAUTH2_SCOPES);

                mManagedChannel = OkHttpChannelBuilder.forAddress(SPEECH_HOST, SPEECH_PORT)
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
                initial.continuous = true;
                initial.enableEndpointerEvents = true;
                initial.maxAlternatives = 3;
                initial.languageCode = mLanguageCode;

                RecognizeRequest firstRequest = new RecognizeRequest();
                firstRequest.initialRequest = initial;
                mStreamObserver.onNext(firstRequest);

                if(mRecognitionListener != null) {
                    mRecognitionListener.onRecognitionStarted(GoogleSpeechRecognizeClient.this);
                }
            } catch (Throwable e) {
                TrackerManager.getInstance(mContext).logException(e);
                finishReceiving(null, e);
                finishSending();
                return;
            }

            try {
                // send empty byte data at first since the speech service seems to skip
                // the first part of the audio
                final int waitingMs = (int) ((float) BUFFER_SIZE / (((float) mSampleRate * BITS_PER_SAMPLE) / 8f) * 1000f);
                final byte[] emptyData = new byte[BUFFER_SIZE];
                final AudioRequest emptyAudio = new AudioRequest();
                emptyAudio.content = emptyData;
                final RecognizeRequest emptyRequest = new RecognizeRequest();
                emptyRequest.audioRequest = emptyAudio;
                try {
                    Thread.sleep(waitingMs);
                    mStreamObserver.onNext(emptyRequest);
                    Thread.sleep(waitingMs);
                    mStreamObserver.onNext(emptyRequest);
                    Thread.sleep(waitingMs);
                } catch (InterruptedException e) {
                        /* nothing to do here */
                }

                while(!mFinished) {
                    try {
                        long ms = System.currentTimeMillis();
                        final byte[] bytes = mAudioDataQueue.take();
                        AudioRequest audio = new AudioRequest();
                        audio.content = bytes;
                        RecognizeRequest request = new RecognizeRequest();
                        request.audioRequest = audio;
                        mStreamObserver.onNext(request);
                        long realWait = waitingMs - (System.currentTimeMillis() - ms);
                        if(realWait > 0) {
                            Thread.sleep(realWait);
                        }
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
            }
        }
    };

    private boolean mFinished = true;
    private Thread mThreadToNotify = null;

    private class RecognizeStreamObserver implements StreamObserver<com.google.cloud.speech.v1.nano.RecognizeResponse> {
        private List<com.google.cloud.speech.v1.nano.RecognizeResponse> mResponseList = new ArrayList<>();
        private RecognizeResponse mLastResponse;

        @Override
        public void onNext(com.google.cloud.speech.v1.nano.RecognizeResponse response) {
            mLastResponse = response;
            if(response != null && response.results != null && response.results.length > 0 && response.error == null) {
                if(response.results[0].isFinal) {
                    mResponseList.add(response);
                    mLastResponse = null;
                }
            }
            Log.d(TAG, String.valueOf(response));
        }

        @Override
        public void onError(Throwable error) {
            if(mLastResponse != null && mLastResponse.error == null) {
                mResponseList.add(mLastResponse);
            }
            finishReceiving(mResponseList, error);
        }

        @Override
        public void onCompleted() {
            if(mLastResponse != null && mLastResponse.error == null) {
                mResponseList.add(mLastResponse);
            }
            finishReceiving(mResponseList, null);
        }
    }

    public GoogleSpeechRecognizeClient(final Context context, String id) {
        this.mContext = context;
        this.mId = id;
        this.mThreadPoolExecutor = new ThreadPoolExecutor(1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    public void startSending(int encoding, int sampleRate, String languageCode) throws NoInternetConnectionException {
        if(!Utils.isInternetAvailable(mContext)) {
            throw new NoInternetConnectionException();
        }

        if(!mFinished) {
            throw new IllegalStateException("There's an ongoing speech recognition! finish() or cancel() must be invoked first.");
        }

        mFinished = false;

        mEncoding = encoding;
        mSampleRate = sampleRate;
        mLanguageCode = getAppropriateLanguage(languageCode);

        mSenderFuture = mThreadPoolExecutor.submit(mSenderRunnable);
    }

    public void finishSending() {
        if(mFinished) {
            throw new IllegalStateException("No ongoing speech recognition! start() must be invoked first.");
        }

        mFinished = true;
        mSenderFuture.cancel(true);
    }

    private void finishReceiving(List<com.google.cloud.speech.v1.nano.RecognizeResponse> responseList, Throwable error) {
        mManagedChannel.shutdown();
        if(mThreadToNotify != null) {
            mThreadToNotify.interrupt();
        }
        if(mRecognitionListener != null) {
            if(error == null) {
                Log.d(TAG, "Recognize Completed.");
                mRecognitionListener.onRecognitionFinished(GoogleSpeechRecognizeClient.this, new RecognizeResponseWrapper(responseList, mLanguageCode));
            } else {
                final Status status = Status.fromThrowable(error);
                Log.d(TAG, "Recognize Failed: " + status);
                mRecognitionListener.onRecognitionError(GoogleSpeechRecognizeClient.this, status, new RecognizeResponseWrapper(responseList, mLanguageCode));
            }
        }
    }

    public boolean recognize(byte[] bytes) {
        if(mFinished) {
            throw new IllegalStateException("No ongoing speech recognition! start() must be invoked first.");
        }
        return mAudioDataQueue.offer(bytes);
    }

    public synchronized Object[] getTranscriptionSync(String mintFilePath) throws IOException, NoInternetConnectionException {
        SpeechApiHttpRequest request = new SpeechApiHttpRequest(new File(mintFilePath));
        HttpJSONResponse<SpeechApiHttpResponseData> response = new HttpJSONResponse<>(mHttpResponseDataParser);
        request.execute(response);

        final SpeechApiHttpResponseData responseData = response.getJsonBody();

        if(response.getCode() / 100 != 2) {
            TrackerManager.getInstance(mContext).log("Error Getting Transcription! " + response.getBody());
            TrackerManager.getInstance(mContext).logException(new RuntimeException("Unable to get Transcription!"));
        }

        String transcription = null;
        float transcriptionConfidence = -1;
        String transcriptionLanguage = null;

        if(responseData != null && responseData.getErrorCode() == 0 && responseData.getResponses().size() > 0) {
            final com.peppermint.app.cloud.apis.speech.RecognizeResponse recognizeResponse = responseData.getResponses().get(0);
            if (recognizeResponse != null && recognizeResponse.getErrorCode() == 0 && recognizeResponse.getResults().size() > 0) {
                transcriptionLanguage = request.getDetectedTranscriptionLanguage();
                final SpeechRecognitionResult speechRecognitionResult =
                        recognizeResponse.getResults().get(0);
                if (!speechRecognitionResult.isFinal()) {
                    transcriptionConfidence = speechRecognitionResult.getStability();
                    transcription = speechRecognitionResult.getAlternatives().get(0).getTranscript();
                } else {
                    transcription = speechRecognitionResult.getAlternatives().get(0).getTranscript();
                    transcriptionConfidence = speechRecognitionResult.getAlternatives().get(0).getConfidence();
                    final int alternativeCount = speechRecognitionResult.getAlternatives().size();
                    for (int i = 1; i < alternativeCount; i++) {
                        if (transcriptionConfidence < speechRecognitionResult.getAlternatives().get(i).getConfidence()) {
                            transcription = speechRecognitionResult.getAlternatives().get(i).getTranscript();
                            transcriptionConfidence = speechRecognitionResult.getAlternatives().get(i).getConfidence();
                        }
                    }
                }
            }
        }

        return new Object[] {transcription, transcriptionConfidence, transcriptionLanguage};
    }

    public static Object[] getBestTranscriptionResults(RecognizeResponseWrapper recognizeResponseWrapper) {
        StringBuilder transcriptionBuilder = new StringBuilder();
        float transcriptionConfidence = -1;
        int amount = 0;
        String transcriptionLanguage = null;
        if(recognizeResponseWrapper != null && recognizeResponseWrapper.getRecognizeResponseList() != null && recognizeResponseWrapper.getRecognizeResponseList().size() > 0) {
            transcriptionLanguage = recognizeResponseWrapper.getLanguageCode();
            final List<RecognizeResponse> recognizeResponseList = recognizeResponseWrapper.getRecognizeResponseList();
            for(RecognizeResponse recognizeResponse : recognizeResponseList) {
                if(recognizeResponse.results.length > 0) {
                    final com.google.cloud.speech.v1.nano.SpeechRecognitionResult speechRecognitionResult =
                            recognizeResponse.results[0];
                    if (!speechRecognitionResult.isFinal) {
                        transcriptionConfidence += (transcriptionConfidence < 0 ? 1 : 0) + speechRecognitionResult.stability;
                        final String transcription = speechRecognitionResult.alternatives[0].transcript;
                        if(transcription.length() > 0) {
                            transcriptionBuilder.append(transcription);
                            transcriptionBuilder.append(".");
                        }
                        amount++;
                    } else {
                        String transcription = speechRecognitionResult.alternatives[0].transcript;
                        float localTranscriptionConfidence = speechRecognitionResult.alternatives[0].confidence;
                        for (int i = 1; i < speechRecognitionResult.alternatives.length; i++) {
                            if (localTranscriptionConfidence < speechRecognitionResult.alternatives[i].confidence) {
                                transcription = speechRecognitionResult.alternatives[i].transcript;
                                localTranscriptionConfidence = speechRecognitionResult.alternatives[i].confidence;
                            }
                        }
                        if(transcription.length() > 0) {
                            transcriptionBuilder.append(transcription);
                            transcriptionBuilder.append(".");
                        }
                        transcriptionConfidence += (transcriptionConfidence < 0 ? 1 : 0) + localTranscriptionConfidence;
                        amount++;
                    }
                }
            }
        }

        String finalTranscription = transcriptionBuilder.toString();
        if(finalTranscription.trim().length() <= 0) {
            finalTranscription = null;
            amount = 0;
            transcriptionConfidence = -1;
        }

        return new Object[] {finalTranscription, amount > 0 ? transcriptionConfidence / (float) amount : transcriptionConfidence, transcriptionLanguage};
    }

    public void setRecognitionListener(RecognitionListener mRecognitionListener) {
        this.mRecognitionListener = mRecognitionListener;
    }

    public boolean isReceiving() {
        return mManagedChannel != null && !mManagedChannel.isShutdown();
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getLanguageCode() {
        return mLanguageCode;
    }

    public String getAppropriateLanguage(String languageCode) {
        int separatorIndex = languageCode.indexOf("-");
        if(languageCode == null || separatorIndex < 0) {
            return DEFAULT_LANGUAGE_CODE;
        }

        if(supportsLanguageCode(languageCode)) {
            return languageCode;
        }

        String pureLanguageCode = languageCode.substring(0, separatorIndex);
        for(int i=0; i<SUPPORTED_LANGUAGE_CODES.length; i++) {
            if(SUPPORTED_LANGUAGE_CODES[i].startsWith(pureLanguageCode)) {
                return SUPPORTED_LANGUAGE_CODES[i];
            }
        }

        return DEFAULT_LANGUAGE_CODE;
    }

    public boolean supportsLanguageCode(String languageCode) {
        for(int i=0; i<SUPPORTED_LANGUAGE_CODES.length; i++) {
            if(SUPPORTED_LANGUAGE_CODES[i].compareTo(languageCode) == 0) {
                return true;
            }
        }
        return false;
    }

    private static final String DEFAULT_LANGUAGE_CODE = "en-US";
    public static final String[] SUPPORTED_LANGUAGE_CODES = {
            "af-ZA",
            "id-ID",
            "ms-MY",
            "ca-ES",
            "cs-CZ",
            "da-DK",
            "de-DE",
            "en-US", "en-GB", "en-AU", "en-CA", "en-IN", "en-IE", "en-NZ", "en-PH", "en-ZA",
            "es-AR", "es-BO", "es-CL", "es-CO", "es-CR", "es-EC", "es-SV", "es-ES", "es-US",
            "es-GT", "es-HN", "es-MX", "es-NI", "es-PA", "es-PY", "es-PE", "es-PR", "es-DO",
            "es-UY", "es-VE", "eu-ES",
            "fil-PH",
            "fr-FR",
            "gl-ES",
            "hr-HR",
            "zu-ZA",
            "is-IS",
            "it-IT",
            "lt-LT",
            "hu-HU",
            "nl-NL",
            "nb-NO",
            "pl-PL",
            "pt-BR", "pt-PT",
            "ro-RO",
            "sk-SK",
            "sl-SI",
            "fi-FI",
            "sv-SE",
            "vi-VN",
            "tr-TR",
            "el-GR",
            "bg-BG",
            "ru-RU",
            "sr-RS",
            "uk-UA",
            "he-IL",
            "ar-AE", "ar-IL", "ar-JO", "ar-BH", "ar-DZ", "ar-SA", "ar-IQ", "ar-KW", "ar-MA",
            "ar-TN", "ar-OM", "ar-PS", "ar-QA", "ar-LB", "ar-EG",
            "fa-IR",
            "hi-IN",
            "th-TH",
            "ko-KR",
            "cmn-Hant-TW",
            "yue-Hant-HK",
            "ja-JP",
            "cmn-Hans-HK",
            "cmn-Hans-CN"
    };
}
