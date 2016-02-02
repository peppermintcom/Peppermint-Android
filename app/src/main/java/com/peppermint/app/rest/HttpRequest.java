package com.peppermint.app.rest;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

	private boolean mForceOnlyGetAndPost = false;
	private String mEndpoint;
	private int mRequestMethod;

	private Map<String, Object> urlParams = new HashMap<>();
	private Map<String, String> headerParams = new HashMap<>();

	private String body;
	private boolean cancelled = false;
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
		this.urlParams.putAll(req.urlParams);
		this.headerParams.putAll(req.headerParams);
		this.cancelled = req.cancelled;
        this.mUuid = req.mUuid;
	}
	
	public HttpRequest(String endpoint) {
		this.mEndpoint = endpoint;

		// defaults
		headerParams.put("Accept", "application/json");
		headerParams.put("Content-Type", "application/json");
		headerParams.put("Cache-Control", "no-cache");

		urlParams.put("_ts", System.currentTimeMillis());
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
		try {
			mConnection = createConnection();

			if(mConnection != null) {
				if(!isCancelled()) {
					if(getBody() != null) {
						OutputStream outStream = mConnection.getOutputStream();
						writeBody(outStream);
						outStream.flush();
						outStream.close();
					}
				}

				if(!isCancelled()) {
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

		// build url params
		String combinedParams = "";
		if (!urlParams.isEmpty()) {
			combinedParams += "?";
			for (Map.Entry<String, Object> p : urlParams.entrySet()) {
				String paramString = p.getKey() + "="
						+ URLEncoder.encode(p.getValue().toString(), "UTF-8");

				if (combinedParams.length() > 1)
					combinedParams += "&";

				combinedParams += paramString;
			}
		}

		// connection
		URL url = new URL(mEndpoint + combinedParams);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setUseCaches(false);
		//conn.setDoInput(true);
		if(mRequestMethod != METHOD_GET) {
			conn.setDoOutput(true);
		}
        long contentLength = getContentLength();
        if(contentLength < 0) {
            conn.setChunkedStreamingMode(0);
        } else {
			// should be supplied for binary content
            headerParams.put("Content-Length", String.valueOf(contentLength));
        }

		// set request method
		if(mForceOnlyGetAndPost && mRequestMethod >= 3) {
			conn.setRequestMethod(METHOD_MAP[METHOD_POST]);
			headerParams.put("X-HTTP-Method-Override", METHOD_MAP[mRequestMethod]);
		} else {
			conn.setRequestMethod(METHOD_MAP[mRequestMethod]);
		}

		// set header params
		if(!headerParams.isEmpty()) {
			for(Map.Entry<String, String> p : headerParams.entrySet()) {
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

	public String getBody() {
		return body;
	}

	/**
	 * Sets the body of the request. The supplied object is serialized using the {@link Object#toString()}
	 * @param body the body object/instance
	 */
	public void setBody(String body) {
		this.body = body;
	}

    public void writeBody(OutputStream outStream) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outStream);
        writer.write(body);
        writer.flush();     // important! without the flush the body might be empty!
    }

	/**
	 * Gets the current URL parameters.
	 * @return the URL parameter map
	 */
	public Map<String, Object> getUrlParams() {
		return urlParams;
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
		urlParams.put(key, obj.toString());
		return this;
	}

	/**
	 * Removes the specified URL parameter
	 * @param key the URL parameter name/key
	 * @return the removed URL parameter value, or null if no parameter was removed (not found)
	 */
	public Object removeUrlParam(String key) {
		if (urlParams.containsKey(key)) {
			return urlParams.remove(key);
		}
		return null;
	}

	/**
	 * Gets the current header parameters.
	 * @return the header parameter map
	 */
	public Map<String, String> getHeaderParams() {
		return headerParams;
	}

	/**
	 * Sets a header parameter.
	 * {@link Object#toString()} will be used to serialize the value object when creating the param string.
	 * @param key the header param key
	 * @param value the header param value
	 * @return the {@link HttpRequest} (this)
	 */
	public HttpRequest setHeaderParam(String key, String value) {
		headerParams.put(key, value);
		return this;
	}

	/**
	 * Removes the specified header parameter.
	 * @param key the header parameter name/key
	 * @return the removed header parameter value, or null if no parameter was removed (not found)
	 */
	public String removeHeaderParam(String key) {
		if (headerParams.containsKey(key)) {
			return headerParams.remove(key);
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
		return cancelled;
	}

	/**
	 * Cancels the request.
	 */
	public void cancel() {
		this.cancelled = true;
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
        out.writeMap(urlParams);
        out.writeMap(headerParams);
        out.writeString(body);
        out.writeInt(cancelled ? 1 : 0);
        out.writeSerializable(mUuid);
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
        urlParams = in.readHashMap(null);
        headerParams = in.readHashMap(null);
        body = in.readString();
        cancelled = in.readInt() != 0;
        mUuid = (UUID) in.readSerializable();
    }

    @Override
    public String toString() {
        return mEndpoint +
                " [" + mRequestMethod + "] { URLPARAMS=" + urlParams +
                ", HEADERPARAMS=" + headerParams +
                ", BODY=" + body + "}";
    }
}
