package com.peppermint.app.rest;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

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
	private HttpURLConnection mConnection;          // the running request connection

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

        try {
            mConnection = mRequest.createConnection();

            if(mConnection != null) {
                if(!mRequest.isCancelled()) {
                    if(mRequest.getBody() != null) {
                        OutputStream outStream = mConnection.getOutputStream();
                        mRequest.writeBody(outStream);
                        outStream.flush();
                        outStream.close();
                    }
                }

                if(!mRequest.isCancelled()) {
                    try {
                        mResponse.setCode(mConnection.getResponseCode());
                    } catch (IOException e) {
                        if(mConnection.getResponseCode() == 401) {
                            mResponse.setCode(401);
                        } else {
                            throw e;
                        }
                    }
                    mResponse.setMessage(mConnection.getResponseMessage());

                    InputStream inputStream = mResponse.getCode() >= 400 ? mConnection.getErrorStream() : mConnection.getInputStream();
                     mResponse.readBody(inputStream, mRequest);
                    inputStream.close();
                }
            } else {
                throw new NullPointerException("HttpURLConnection is null. Skipping...");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while performing HttpURLConnection!", e);
            mResponse.setException(e);
        }

        if(mConnection != null) {
            try {
                mConnection.disconnect();
            } catch(Throwable e) {
                Log.e(TAG, "Error disconnecting...", e);
            }
        }

        if(mListener != null) {
            mListener.onDisconnect(this, mRequest, mResponse);
        }
	}

    /**
     * Cancel the runnable and its request.<br />
     * This method tries to interrupt the thread and disconnect the connection.
     */
	public void cancel() {
		mRequest.cancel();
		if(mConnection != null) {
			// TODO neither disconnect or interrupt works for HttpUrlConnection!
            // This has been fixed by sending a virtual cancel event back to the listener;
            // The thread/runnable still hangs until the request times out and further events are ignored.
			mConnection.disconnect();
			interrupt();
		}
	}
}
