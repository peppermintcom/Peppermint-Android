package com.peppermint.app.rest;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 02-07-2015.
 *
 * <p>
 *     Listener of events related to {@link HttpRequest}, executed by a {@link HttpClientRunnable}.<br >
 *     These events are triggered by the {@link HttpClientService} in the main/UI thread.
 * </p>
 */
public interface HttpRequestListener {
	void onRequestSuccess(HttpRequest request, HttpResponse response);
	void onRequestError(HttpRequest request, HttpResponse response);
	void onRequestCancel(HttpRequest request);
}