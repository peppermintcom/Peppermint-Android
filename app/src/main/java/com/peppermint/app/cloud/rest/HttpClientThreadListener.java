package com.peppermint.app.cloud.rest;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 02-07-2015.
 *
 * <p>
 *     RecordServiceListener of events related to {@link HttpRequest}, executed by a {@link HttpClientThread}.<br />
 *     These methods run in the {@link HttpClientThread}'s background_gradient thread.
 * </p>
 */
public interface HttpClientThreadListener {

	/**
     * Invoked before a connection is established.
     * @param runnable the runnable handling the connection
	 * @param request the request
	 */
	void onConnect(HttpClientThread runnable, HttpRequest request);

	/**
     * Invoked after the request is finished and a response has been received.
     * @param runnable the runnable handling the connection
	 * @param request the request
     * @param response the response
	 */
	void onDisconnect(HttpClientThread runnable, HttpRequest request, HttpResponse response);
		
}
