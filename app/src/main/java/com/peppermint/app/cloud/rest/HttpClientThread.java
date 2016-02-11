package com.peppermint.app.cloud.rest;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 02-07-2015.
 *
 * <p>
 *     The runnable that executes a {@link HttpRequest} in a background_gradient thread (pool).
 * </p>
 */
public class HttpClientThread extends Thread {

	private static final String TAG = HttpClientThread.class.getSimpleName();

	private HttpClientThreadListener mListener;
	private HttpRequest mRequest;                   // the running request
    private HttpResponse mResponse;                 // the running request response

	public HttpClientThread(HttpClientThreadListener listener, HttpRequest request, HttpResponse response) {
		this.mListener = listener;
		this.mRequest = request;
        this.mResponse = response;
	}

    /**
     * Executes a {@link HttpRequest}.<br />
     * Its listener methods are invoked as follows:
     * <ol>
     *     <li>{@link HttpClientThreadListener#onConnect(HttpClientThread, HttpRequest)} - through the background_gradient thread handler</li>
     *     <li>{@link HttpClientThreadListener#onDisconnect(HttpClientThread, HttpRequest, HttpResponse)} - through the background_gradient thread handler</li>
     * </ol>
     */
	@Override
	public void run() {
        if(mListener != null) {
            mListener.onConnect(this, mRequest);
        }

        mRequest.execute(mResponse);

        if(mListener != null) {
            mListener.onDisconnect(this, mRequest, mResponse);
        }
	}

    /**
     * Cancel the runnable and its request.<br />
     * This method tries to interrupt the thread and disconnect the connection.
     */
	public void cancel() {
        // TODO neither disconnect or interrupt works for HttpUrlConnection!
        // This has been fixed by sending a virtual cancel event back to the listener;
        // The thread/runnable still hangs until the request times out and further events are ignored.
		mRequest.cancel();
        interrupt();
	}
}
