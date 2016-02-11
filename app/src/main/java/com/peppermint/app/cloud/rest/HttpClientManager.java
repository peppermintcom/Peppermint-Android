package com.peppermint.app.cloud.rest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 10-07-2015.
 * <p>
 *     Manages the execution of HTTP requests through an Android Service.<br/>
 *     It is prepared to deal with the peculiarities of the Activity life-cycle,
 *     even when the Activity is hidden or its configuration is changed (e.g. orientation).
 * </p>
 */
public class HttpClientManager {

    private static final String TAG = HttpClientManager.class.getSimpleName();

    private static final String UUID_LIST_KEY = "HttpClientManager_UUID_List";  // key to save to bundle

    // context and service instances
    protected Context mContext;
    protected HttpClientService.HttpClientServiceBinder mService;

    protected boolean mIsBound = false;                                         // if the manager is bound to the service
    protected List<HttpRequestListener> mListenerList = new ArrayList<>();      // the external (activity) listener
    protected Map<UUID, HttpRequest> mPendingRequestMap = new HashMap<>();      // pending HTTP requests

    /**
     * Event callback triggered by the {@link HttpClientService} through an {@link de.greenrobot.event.EventBus}.<br />
     * This is invoked after the HTTP connection is closed, whether the request succeeds of fails.
     * @param event the event
     */
    public void onEventMainThread(HttpClientService.HttpClientServiceEvent event) {
        HttpRequest request = event.getRequest();
        HttpResponse response = event.getResponse();

        switch (event.getType()) {
            case HttpClientService.MSG_ONSUCCESS:             // request succeeded
                if(mPendingRequestMap.containsKey(request.getUUID())) {
                    mPendingRequestMap.remove(request.getUUID());
                    for(HttpRequestListener listener : mListenerList) {
                        listener.onRequestSuccess(request, response);
                    }
                }
                break;
            case HttpClientService.MSG_ONERROR:               // request failed
                if(mPendingRequestMap.containsKey(request.getUUID())) {
                    mPendingRequestMap.remove(request.getUUID());
                    for(HttpRequestListener listener : mListenerList) {
                        listener.onRequestError(request, response);
                    }
                }
                break;
            default:
        }
    }

    /**
     * Event listener associated with the service bind/unbind.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (HttpClientService.HttpClientServiceBinder) binder;
            mService.register(HttpClientManager.this);

            // Some of the pending request might have finished while an activity was in the background_gradient.
            // Thus, re-trigger all events once a client binds to the service.
            // This also removes the event so that it will not be triggered again in future binds.
            for(UUID uuid : mPendingRequestMap.keySet()) {
                HttpClientService.HttpClientServiceEvent ev = mService.popPendingFinishEvent(uuid);
                if(ev != null) {
                    mPendingRequestMap.put(uuid, ev.getRequest());
                    onEventMainThread(ev);
                }
            }

            Log.d(TAG, "onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName className) {
            // this is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    public HttpClientManager(Context context) {
        this.mContext = context;
    }

    /**
     * Always invoke this during {@link Activity#onCreate} to load the saved instance state!
     * @param bundle the saved bundle
     */
    public void load(Bundle bundle) {
        String[] uuidList = bundle.getStringArray(UUID_LIST_KEY);
        if(uuidList == null) {
            return;
        }
        for(String uuidStr : uuidList) {
            mPendingRequestMap.put(UUID.fromString(uuidStr), null);
        }
    }

    /**
     * Always invoke this during {@link Activity#onSaveInstanceState(Bundle)} to save the instance state!
     * @param bundle the bundle of data to save
     */
    public void save(Bundle bundle) {
        String[] uuidList = new String[mPendingRequestMap.size()];
        int i=0;
        for(UUID uuid : mPendingRequestMap.keySet()) {
            uuidList[i] = uuid.toString();
            i++;
        }
        bundle.putStringArray(UUID_LIST_KEY, uuidList);
    }

    /**
     * Starts the service that will handle all HTTP requests.
     * Also binds this manager to the service.
     */
    public void start() {
        Intent intent = new Intent(mContext, HttpClientService.class);
        mContext.startService(intent);
        bind();
    }

    /**
     * Tries to stop the service that handles all HTTP requests.
     * Also unbinds this manager from the service.
     * The service will only stop after cancelling and cleaning up all pending HTTP requests.
     */
    public void shouldStop() {
        cancelRequests();
        mService.tryShutdown();
        unbind();
        mListenerList.clear();
    }

    public UUID performRequest(HttpRequest request) {
        return performRequest(request, new HttpResponse());
    }

	public UUID performRequest(HttpRequest request, HttpResponse responseObject) {
		// send command to the service
        Intent intent = new Intent(mContext, HttpClientService.class);
        intent.putExtra(HttpClientService.INTENT_DATA_REQUEST, request);
        intent.putExtra(HttpClientService.INTENT_DATA_RESPONSE, responseObject);
        mContext.startService(intent);

        mPendingRequestMap.put(request.getUUID(), request);
        return request.getUUID();
	}

    /**
     * Creates and submits a request to the service so that it is executed in a background_gradient thread.<br />
     * @param endpoint the request URL
     * @param requestMethod the request HTTP method
     * @param body the request body data
     * @param headerContentType the content type HTTP header
     * @param headerAuth the auth HTTP header
     * @return the UUID of the request
     */
    public UUID performRequest(String endpoint, int requestMethod, String body, String headerContentType, String headerAuth) {
        HttpRequest request = new HttpRequest(endpoint, requestMethod);
        if(headerContentType != null) {
            request.setHeaderParam("Content-Type", headerContentType);
        }
        if(headerAuth != null) {
            request.setHeaderParam("Authorization", headerAuth);
        }
        if(body != null) {
            request.setBody(body);
        }
		return performRequest(request);
    }

    /**
     * Trigger the cancelling of the request with the specified UUID.<br />
     * The cancel event is triggered immediately although the actual cancelling may still be ocurring in the background_gradient.
     * @param uuid the UUID of the request
     */
    public void cancelRequest(UUID uuid) {
        if(!mPendingRequestMap.containsKey(uuid)) {
            return;
        }

        mService.cancelRequest(mPendingRequestMap.get(uuid));

        HttpRequest req = mPendingRequestMap.get(uuid);
        mPendingRequestMap.remove(uuid);
        for(HttpRequestListener listener : mListenerList) {
            listener.onRequestCancel(req);
        }
    }

    /**
     * Triggers the cancelling of all pending requests. <br />
     * See {@link #cancelRequest(UUID)} for more information.
     */
    public void cancelRequests() {
        Set<HttpRequest> reqSet = new HashSet<>(mPendingRequestMap.values());
        for(HttpRequest request : reqSet) {
            cancelRequest(request.getUUID());
        }
    }

    public boolean isPending(UUID uuid) {
        return mPendingRequestMap.containsKey(uuid);
    }

    /**
     * Binds this manager to the service.
     */
    public void bind() {
        mContext.bindService(new Intent(mContext, HttpClientService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * Unbinds this manager from the service.
     */
    public void unbind() {
        if (mIsBound) {
            // if we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                mService.unregister(HttpClientManager.this);
            }
            // detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public void addHttpRequestListener(HttpRequestListener listener) {
        mListenerList.add(listener);
    }

    public boolean removeHttpRequestListener(HttpRequestListener listener) {
        return mListenerList.remove(listener);
    }

    public boolean isBound() {
        return mIsBound;
    }
}
