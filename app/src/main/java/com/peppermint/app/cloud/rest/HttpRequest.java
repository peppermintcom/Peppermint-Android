package com.peppermint.app.cloud.rest;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 02-07-2015.
 *
 * <p>
 *     Represents an HTTP REST request.<br />
 *     It builds partial instances of {@link HttpURLConnection} with the supplied parameters and configuration.
 * </p>
 */
public class HttpRequest implements Parcelable {

    private static final String TAG = HttpRequest.class.getSimpleName();

	public static final int METHOD_GET = 1;
	public static final int METHOD_POST = 2;
	public static final int METHOD_PUT = 3;
	public static final int METHOD_DELETE = 4;

	public static final String[] METHOD_MAP = {"INVALID", "GET", "POST", "PUT", "DELETE"};

	private int mConnectTimeout = -1;
	private int mReadTimeout = -1;

	private boolean mForceOnlyGetAndPost = false;
	private String mEndpoint;
	private int mRequestMethod;

	private Map<String, Object> mUrlParams = new HashMap<>();
	private Map<String, String> mHeaderParams = new HashMap<>();

	private String mBody;
	private boolean mCancelled = false;
    private UUID mUuid = UUID.randomUUID();

    private transient HttpURLConnection mConnection;          // the running request connection

    /**
     * Copy constructor
     * @param req the {@link HttpRequest to be copied}
     */
	public HttpRequest(HttpRequest req) {
		this.mForceOnlyGetAndPost = req.mForceOnlyGetAndPost;
		this.mEndpoint = req.mEndpoint;
		this.mRequestMethod = req.mRequestMethod;
		this.mUrlParams.putAll(req.mUrlParams);
		this.mHeaderParams.putAll(req.mHeaderParams);
		this.mCancelled = req.mCancelled;
        this.mUuid = req.mUuid;
		this.mBody = req.mBody;
		this.mConnectTimeout = req.mConnectTimeout;
		this.mReadTimeout = req.mReadTimeout;
	}
	
	public HttpRequest(String endpoint) {
		this.mEndpoint = endpoint;

		// defaults
		mHeaderParams.put("Accept", "application/json");
		mHeaderParams.put("Content-Type", "application/json");
		mHeaderParams.put("Cache-Control", "no-cache");

		mUrlParams.put("_ts", System.currentTimeMillis());
	}

	/**
	 * Starts the request with the endpoint and the HTTP request method.
	 * @param endpoint the endpoint (without parameters)
	 * @param requestMethod the HTTP request method (use {@link #METHOD_GET}, {@link #METHOD_POST}, {@link #METHOD_PUT} or {@link #METHOD_DELETE})
	 */
	public HttpRequest(String endpoint, int requestMethod) {
		this(endpoint);
		this.mRequestMethod = requestMethod;
	}
	
	public HttpRequest(String endpoint, int requestMethod, boolean forceOnlyGetAndPost) {
		this(endpoint, requestMethod);
		this.mForceOnlyGetAndPost = forceOnlyGetAndPost;
	}

	public void execute(HttpResponse response) {
		Log.d(TAG, toString());

		try {
			mConnection = createConnection();

			if(mConnection != null) {
				if(!isCancelled()) {
					Log.d(TAG, getEndpoint() + " Sending Request Body...");
					if(getBody() != null) {
						OutputStream outStream = mConnection.getOutputStream();
						writeBody(outStream);
						outStream.flush();
						outStream.close();
					}
				} else {
					Log.d(TAG, getEndpoint() + " Cancelled before sending Request Body...");
				}

				if(!isCancelled()) {
					Log.d(TAG, getEndpoint() + " Retrieving Response Body...");
					try {
                        response.setCode(mConnection.getResponseCode());
					} catch (IOException e) {
						if(mConnection.getResponseCode() == 401) {
                            response.setCode(401);
						} else {
							throw e;
						}
					}
                    response.setMessage(mConnection.getResponseMessage());

					InputStream inputStream = response.getCode() >= 400 ? mConnection.getErrorStream() : mConnection.getInputStream();
                    response.readBody(inputStream, this);
					inputStream.close();
				} else {
					Log.d(TAG, getEndpoint() + " Cancelled before retrieving Response Body...");
				}
			} else {
				throw new NullPointerException("HttpURLConnection is null. Skipping...");
			}
		} catch (Throwable e) {
			Log.e(TAG, "Error while performing HttpURLConnection!", e);
            response.setException(e);
		}

		if(mConnection != null) {
			try {
				Log.d(TAG, getEndpoint() + " Disconnecting...");
				mConnection.disconnect();
			} catch(Throwable e) {
				Log.e(TAG, "Error disconnecting...", e);
			}
		}
	}

    protected long getContentLength() {
        return -1;
    }
	
	/**
	 * 
	 * Builds a partial {@link HttpURLConnection} object with all the request data.<br />
     * It does not establish the connection!
	 * @throws Exception (URL encoding)
	 * @return the {@link HttpURLConnection} instance
	 */
	protected HttpURLConnection createConnection() throws Exception {

        Uri.Builder uriBuilder = Uri.parse(mEndpoint).buildUpon();

		// build url params
		if (!mUrlParams.isEmpty()) {
			for (Map.Entry<String, Object> p : mUrlParams.entrySet()) {
                uriBuilder.appendQueryParameter(p.getKey(), p.getValue().toString());
			}
		}

		// connection
		URL url = new URL(uriBuilder.build().toString());
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		if(mConnectTimeout >= 0) {
			conn.setConnectTimeout(mConnectTimeout);
		}
		if(mReadTimeout >= 0) {
			conn.setReadTimeout(mReadTimeout);
		}
		conn.setUseCaches(false);
		//conn.setDoInput(true);
		if(mRequestMethod == METHOD_PUT || mRequestMethod == METHOD_POST) {
			conn.setDoOutput(true);
		}
        long contentLength = getContentLength();
        if(contentLength < 0) {
            conn.setChunkedStreamingMode(0);
        } else {
			// should be supplied for binary content
			mHeaderParams.put("Content-Length", String.valueOf(contentLength));
        }

		// set request method
		if(mForceOnlyGetAndPost && mRequestMethod >= 3) {
			conn.setRequestMethod(METHOD_MAP[METHOD_POST]);
			mHeaderParams.put("X-HTTP-Method-Override", METHOD_MAP[mRequestMethod]);
		} else {
			conn.setRequestMethod(METHOD_MAP[mRequestMethod]);
		}

		// set header params
		if(!mHeaderParams.isEmpty()) {
			for(Map.Entry<String, String> p : mHeaderParams.entrySet()) {
				conn.setRequestProperty(p.getKey(), p.getValue());
			}
		}

		return conn;
	}

	/**
	 * Some servers do not allow HTTP methods other than GET and POST
	 * Setting this flag to true uses the X-HTTP-Method-Override as a work-around
	 * @return true if forcing; false otherwise
	 */
	public boolean isForceOnlyGetAndPost() {
		return mForceOnlyGetAndPost;
	}

	/**
	 * Some servers do not allow HTTP methods other than GET and POST
	 * Setting this flag to true uses the X-HTTP-Method-Override as a work-around
	 * @param forceOnlyGetAndPost true to force GET or POST
	 * @return the {@link HttpRequest} (this)
	 */
	public HttpRequest setForceOnlyGetAndPost(boolean forceOnlyGetAndPost) {
		this.mForceOnlyGetAndPost = forceOnlyGetAndPost;
		return this;
	}

	/**
	 * The actual endpoint URL, without parameters
	 * @return the endpoint URL
	 */
	public String getEndpoint() {
		return mEndpoint;
	}

	/**
	 * Sets the endpoint URL, without parameters
	 * @param endpoint the endpoint without parameters
	 * @return the {@link HttpRequest} (this)
	 */
	public HttpRequest setEndpoint(String endpoint) {
		this.mEndpoint = endpoint;
		return this;
	}

	public int getConnectTimeout() {
		return mConnectTimeout;
	}

	/**
	 * See {@link HttpURLConnection#setConnectTimeout(int)}
	 * @param mConnectTimeout
     */
	public void setConnectTimeout(int mConnectTimeout) {
		this.mConnectTimeout = mConnectTimeout;
	}

	public int getReadTimeout() {
		return mReadTimeout;
	}

	/**
	 * See {@link HttpURLConnection#setReadTimeout(int)}
	 * @param mReadTimeout
     */
	public void setReadTimeout(int mReadTimeout) {
		this.mReadTimeout = mReadTimeout;
	}

	public String getBody() {
		return mBody;
	}

	/**
	 * Sets the body of the request. The supplied object is serialized using the {@link Object#toString()}
	 * @param body the body object/instance
	 */
	public void setBody(String body) {
		this.mBody = body;
	}

    public void writeBody(OutputStream outStream) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outStream);
        writer.write(mBody);
        writer.flush();     // important! without the flush the body might be empty!
    }

	/**
	 * Gets the current URL parameters.
	 * @return the URL parameter map
	 */
	public Map<String, Object> getUrlParams() {
		return mUrlParams;
	}

	/**
	 * Sets an URL parameter.
	 * {@link Object#toString()} will be used to serialize the value object when creating the param string.
	 *
	 * @param key the url param key
	 * @param obj the url param value
	 * @return the {@link HttpRequest} (this)
	 */
	public HttpRequest setUrlParam(String key, Object obj) {
		mUrlParams.put(key, obj.toString());
		return this;
	}

	/**
	 * Removes the specified URL parameter
	 * @param key the URL parameter name/key
	 * @return the removed URL parameter value, or null if no parameter was removed (not found)
	 */
	public Object removeUrlParam(String key) {
		if (mUrlParams.containsKey(key)) {
			return mUrlParams.remove(key);
		}
		return null;
	}

	/**
	 * Gets the current header parameters.
	 * @return the header parameter map
	 */
	public Map<String, String> getHeaderParams() {
		return mHeaderParams;
	}

	/**
	 * Sets a header parameter.
	 * {@link Object#toString()} will be used to serialize the value object when creating the param string.
	 * @param key the header param key
	 * @param value the header param value
	 * @return the {@link HttpRequest} (this)
	 */
	public HttpRequest setHeaderParam(String key, String value) {
		mHeaderParams.put(key, value);
		return this;
	}

	/**
	 * Removes the specified header parameter.
	 * @param key the header parameter name/key
	 * @return the removed header parameter value, or null if no parameter was removed (not found)
	 */
	public String removeHeaderParam(String key) {
		if (mHeaderParams.containsKey(key)) {
			return mHeaderParams.remove(key);
		}
		return null;
	}

    public UUID getUUID() {
        return mUuid;
    }

    /**
	 * Checks if the request has been cancelled.
	 * @return true if cancelled; false otherwise
	 */
	public boolean isCancelled() {
		return mCancelled;
	}

	/**
	 * Cancels the request.
	 */
	public void cancel() {
		this.mCancelled = true;
        if(mConnection != null) {
            mConnection.disconnect();
        }
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mForceOnlyGetAndPost ? 1 : 0);
        out.writeString(mEndpoint);
        out.writeInt(mRequestMethod);
        out.writeMap(mUrlParams);
        out.writeMap(mHeaderParams);
        out.writeString(mBody);
        out.writeInt(mCancelled ? 1 : 0);
        out.writeSerializable(mUuid);
		out.writeInt(mConnectTimeout);
		out.writeInt(mReadTimeout);
	}

	public static final Creator<HttpRequest> CREATOR = new Creator<HttpRequest>() {
		public HttpRequest createFromParcel(Parcel in) {
			return new HttpRequest(in);
		}
		public HttpRequest[] newArray(int size) {
			return new HttpRequest[size];
		}
	};

    protected HttpRequest(Parcel in) {
        mForceOnlyGetAndPost = in.readInt() != 0;
        mEndpoint = in.readString();
        mRequestMethod = in.readInt();
        mUrlParams = in.readHashMap(null);
		mHeaderParams = in.readHashMap(null);
        mBody = in.readString();
		mCancelled = in.readInt() != 0;
        mUuid = (UUID) in.readSerializable();
		mConnectTimeout = in.readInt();
		mReadTimeout = in.readInt();
    }

    @Override
    public String toString() {
        return mEndpoint +
                " [" + mRequestMethod + "] { URLPARAMS=" + mUrlParams +
                ", HEADERPARAMS=" + mHeaderParams +
                ", BODY=" + mBody +
				", Cancelled=" + mCancelled +
				", Method=" + (mRequestMethod == METHOD_GET ? "GET" :
                    mRequestMethod == METHOD_POST ? "POST" :
                            mRequestMethod == METHOD_PUT ? "PUT" :
                                    mRequestMethod == METHOD_DELETE ? "DELETE" : mRequestMethod) + "}";
    }
}
