package com.peppermint.app.cloud.apis;

import com.peppermint.app.cloud.rest.HttpRequest;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Nuno Luz on 18-04-2016.
 *
 * Base API class for tracking all requests and proper cancelling.
 */
public class BaseApi implements Serializable {

    protected Map<String, Set<HttpRequest>> mPendingHttpRequests = new ConcurrentHashMap<>();

    public BaseApi() {
    }

    protected void addPendingRequest(String requesterId, HttpRequest request) {
        if(requesterId == null) {
            return;
        }

        Set<HttpRequest> set = mPendingHttpRequests.get(requesterId);
        if(set == null) {
            set = new HashSet<>();
            mPendingHttpRequests.put(requesterId, set);
        }
        set.add(request);
    }

    protected boolean removePendingRequest(String requesterId, HttpRequest request) {
        if(requesterId == null) {
            return false;
        }

        Set<HttpRequest> set = mPendingHttpRequests.get(requesterId);
        return set != null && set.remove(request);
    }

    public void cancelPendingRequests(String requesterId) {
        Set<HttpRequest> set = mPendingHttpRequests.get(requesterId);
        if(set != null) {
            for(HttpRequest request : set) {
                request.cancel();
            }
        }
    }
}
