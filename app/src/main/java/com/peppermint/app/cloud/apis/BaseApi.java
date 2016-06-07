package com.peppermint.app.cloud.apis;

import android.content.Context;

import com.peppermint.app.cloud.rest.HttpRequest;
import com.peppermint.app.cloud.rest.HttpResponse;
import com.peppermint.app.cloud.rest.HttpResponseException;
import com.peppermint.app.trackers.TrackerApi;
import com.peppermint.app.trackers.TrackerManager;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Nuno Luz on 18-04-2016.
 * <p/>
 * Base API class for tracking all requests and proper cancelling.
 */
public class BaseApi implements Serializable {

    protected Map<String, Set<HttpRequest>> mPendingHttpRequests = new ConcurrentHashMap<>();

    protected Context mContext;
    protected String mApiKey = null;
    protected String mAuthToken = null;

    public BaseApi(final Context mContext) {
        this.mContext = mContext;
    }

    // PENDING REQUESTS

    protected void addPendingRequest(final String requesterId, final HttpRequest request) {
        if (requesterId == null) {
            return;
        }

        Set<HttpRequest> set = this.mPendingHttpRequests.get(requesterId);
        if (set == null) {
            set = new HashSet<>();
            this.mPendingHttpRequests.put(requesterId, set);
        }
        set.add(request);
    }

    protected boolean removePendingRequest(final String requesterId, final HttpRequest request) {
        if (requesterId == null) {
            return false;
        }

        Set<HttpRequest> set = this.mPendingHttpRequests.get(requesterId);
        return set != null && set.remove(request);
    }

    public void cancelPendingRequests(final String requesterId) {
        Set<HttpRequest> set = this.mPendingHttpRequests.get(requesterId);
        if (set != null) {
            for (HttpRequest request : set) {
                request.cancel();
            }
        }
    }

    // REQUEST EXECUTION

    protected <T extends HttpResponse> T executeRequest(final String requesterId, final HttpRequest request, final T response, final boolean requiresAuthenticationToken) throws Exception {
        if(requiresAuthenticationToken) {
            String authenticationToken = peekAuthenticationToken();
            if(authenticationToken == null) {
                authenticationToken = renewAuthenticationToken();
            }
            request.setHeaderParam("Authorization", "Bearer " + authenticationToken);
        }

        if(mApiKey != null) {
            request.setHeaderParam("X-Api-Key", mApiKey);
        }

        addPendingRequest(requesterId, request);
        try {
            request.execute(response);
        } finally {
            removePendingRequest(requesterId, request);
        }

        // retry with new authentication token, if necessary
        if(requiresAuthenticationToken && isAuthenticationTokenRenewalRequired(response)) {
            request.setHeaderParam("Authorization", "Bearer " + renewAuthenticationToken());
            addPendingRequest(requesterId, request);
            try {
                request.execute(response);
            } finally {
                removePendingRequest(requesterId, request);
            }
        }

        if(response.getException() != null) {
            TrackerManager.getInstance(mContext).track(TrackerApi.TYPE_EVENT, request.toString(), response.getException(), getClass().getSimpleName());
            throw new HttpResponseException(response.getException());
        }

        if(response.getCode() / 100 != 2) {
            TrackerManager.getInstance(mContext).track(TrackerApi.TYPE_EVENT, "Code: " + response.getCode() + "\n" + request.toString(), getClass().getSimpleName());
        }

        return response;
    }

    public String renewAuthenticationToken() throws Exception {
        /* implement on sub-class */
        return this.mAuthToken;
    }

    public String peekAuthenticationToken() throws Exception {
        return this.mAuthToken;
    }

    protected boolean isAuthenticationTokenRenewalRequired(final HttpResponse response) {
        /* implement on sub-class */
        return false;
    }

    protected void processGenericExceptions(final HttpRequest request, final HttpResponse response) throws Exception {
        /* implement on sub-class */
    }

    public void setAuthenticationToken(final String authenticationToken) {
        this.mAuthToken = authenticationToken;
    }
}
