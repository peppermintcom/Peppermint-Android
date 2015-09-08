package com.peppermint.app.rest;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 02-07-2015.
 *
 * <p>
 *     Listener of events related to {@link HttpRequest}, executed by a {@link HttpClientRunnable}.<br />
 *     These methods run in the {@link HttpClientRunnable}'s background thread.
 * </p>
 */
public interface HttpClientRunnableListener {

	/**
     * Invoked before a connection is established.
     * @param runnable the runnable handling the connection
	 * @param request the request
	 */
	void onConnect(HttpClientRunnable runnable, HttpRequest request);

	/**
     * Invoked after the request is finished and a response has been received.
     * @param runnable the runnable handling the connection
	 * @param request the request
     * @param response the response
	 */
	void onDisconnect(HttpClientRunnable runnable, HttpRequest request, HttpResponse response);
		
}
