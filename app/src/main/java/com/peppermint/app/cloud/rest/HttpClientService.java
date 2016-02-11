package com.peppermint.app.cloud.rest;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

public class HttpClientService extends Service {

    private static final String TAG = HttpClientService.class.getSimpleName();

    protected IBinder mBinder = new HttpClientServiceBinder();
    
    /**
     * The service binder used by external components to interact with the service.
     */
    public class HttpClientServiceBinder extends Binder {
        /**
         * Register an event listener to receive HTTP request events.
         * @param listener the event listener
         */
        void register(Object listener) {
            mEventBus.register(listener);
        }

        /**
         * Unregister the specified event listener to stop receiving HTTP request events.
         * @param listener the event listener
         */
        void unregister(Object listener) {
            mEventBus.unregister(listener);
        }

        /**
         * Retrieves and pops the finish (error or success) HTTP request event associated with the
         * specified request UUID. <br />
         * This is useful for activities that are in the background_gradient when a particular HTTP request finishes.<br />
         * Through this method, the finish event can be checked and retrieved later during {@link Activity#onStart()}.
         * @param requestUuid the request UUID
         * @return the finish HTTP request event
         */
        HttpClientServiceEvent popPendingFinishEvent(UUID requestUuid) {
            if(mFinishedRequests.containsKey(requestUuid)) {
                HttpClientServiceEvent ev = mFinishedRequests.get(requestUuid);
                mFinishedRequests.remove(requestUuid);
                return ev;
            }
            return null;
        }

        /**
         * Cancel a HTTP request.
         * @param request the request
         */
        void cancelRequest(final HttpRequest request) {
            mThreadPool.execute(new CancelRunnable(request));
        }

        /**
         * Try to shutdown the service.
         */
        void tryShutdown() {
            mTryShutdown = true;
        }
    }

    /**
     * Runnable that cancels a particular request in a secondary/background_gradient thread.<br />
     * This must happen in a background_gradient thread since the {@link HttpClientThread#cancel()} invokes the {@link HttpURLConnection#disconnect()} method.
     */
    public class CancelRunnable implements Runnable {
        private HttpRequest mRequest;
        public CancelRunnable(HttpRequest request) {
            this.mRequest = request;
        }
        public void run() {
            UUID uuid = mRequest.getUUID();
            if(mRunningMap.containsKey(uuid)) {
                Log.d(TAG, "Cancelling request " + mRequest.getEndpoint() + " (" + mRequest.getUUID() + ")");
                mRequest.cancel();
                mRunningMap.get(uuid).cancel();
            }
        }
    }

    /**
     * Class that represents HTTP request events.
     */
    public class HttpClientServiceEvent {
        private HttpRequest request;
        private HttpResponse response;
        private int type;

        public HttpClientServiceEvent(int type, HttpRequest request, HttpResponse response) {
            this.type = type;
            this.request = request;
            this.response = response;
        }

        public HttpRequest getRequest() {
            return request;
        }
        public HttpResponse getResponse() {
            return response;
        }
        public int getType() {
            return type;
        }
    }

    // Event types sent to the HttpClientManager
    public static final int MSG_ONERROR = 10;
    public static final int MSG_ONSUCCESS = 11;

    // Intent data keys
    public static final String INTENT_DATA_REQUEST = "RESTfulClientService_Request";
    public static final String INTENT_DATA_RESPONSE = "RESTfulClientService_Response";

    private EventBus mEventBus;
    private boolean mTryShutdown = false;

    /*
     * Gets the number of available cores
     * (not always the same as the maximum number of cores)
     */
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private BlockingQueue<Runnable> mWorkQueue = new LinkedBlockingQueue<>();

    protected Executor mThreadPool = new ThreadPoolExecutor(
            NUMBER_OF_CORES,                                // initial pool size
            NUMBER_OF_CORES > 4 ? NUMBER_OF_CORES : 4,      // max pool size
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            mWorkQueue);

    protected Map<UUID, HttpClientThread> mRunningMap = new HashMap<>();
    protected Map<UUID, HttpClientServiceEvent> mFinishedRequests = new HashMap<>();

    /**
     * Listens for events triggered by the {@link HttpClientThread} in a background_gradient thread.
     */
    protected HttpClientThreadListener mListener = new HttpClientThreadListener() {
        @Override
        public void onConnect(HttpClientThread runnable, HttpRequest request) {
            // do nothing for now
            Log.d(TAG, "Connecting for request " + request.getEndpoint() + " (" + request.getUUID() + ")");
            Log.d(TAG, "Request (" + request.getUUID() + "): " + request.getBody());
        }

        @Override
        public void onDisconnect(HttpClientThread runnable, HttpRequest request, HttpResponse response) {
            // send the success/error event to the HttpClientManager through the EventBus
            HttpClientServiceEvent ev;
            if(response.getException() != null || (response.getCode()/100) != 2) {
                ev = new HttpClientServiceEvent(MSG_ONERROR, request, response);
                mEventBus.post(ev);
            } else {
                ev = new HttpClientServiceEvent(MSG_ONSUCCESS, request, response);
                mEventBus.post(ev);
            }

            mRunningMap.remove(request.getUUID());
            mFinishedRequests.put(request.getUUID(), ev);

            if(mRunningMap.size() <= 0 && mTryShutdown) {
                mTryShutdown = false;
                stopSelf();
                Log.d(TAG, "Shutdown received. Stopping self since no pending requests exist...");
            }

            Log.d(TAG, "Response (" + request.getUUID() + "): " + response.getBody());
            Log.d(TAG, "Disconnecting (finished) request " + request.getEndpoint() + " (" + request.getUUID() + ")");
        }
    };

    public HttpClientService() {
        mEventBus = new EventBus();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);

        mTryShutdown = false;

        if(intent != null && intent.hasExtra(INTENT_DATA_REQUEST) && intent.hasExtra(INTENT_DATA_RESPONSE)) {
            HttpRequest request = intent.getParcelableExtra(INTENT_DATA_REQUEST);
            HttpResponse response = intent.getParcelableExtra(INTENT_DATA_RESPONSE);
            HttpClientThread runnable = new HttpClientThread(mListener, request, response);
            mThreadPool.execute(runnable);
            mRunningMap.put(request.getUUID(), runnable);
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: " + intent);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: " + intent);
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind: " + intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mFinishedRequests.clear();
        super.onDestroy();
    }
}
