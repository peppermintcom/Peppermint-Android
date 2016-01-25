package com.peppermint.app.rest;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 02-07-2015.
 *
 * <p>
 *     Represents an HTTP REST response.
 * </p>
 */
public class HttpResponse implements Parcelable {

    private Map<String, String> mHeaderParams = new HashMap<>();
	private Object mBody;
	private int mCode;
	private String mMessage;
	private Throwable mException;

    /**
     * Copy constructor
     * @param resp the {@link HttpResponse to be copied}
     */
	public HttpResponse(HttpResponse resp) {
		this.mBody = resp.mBody;
		this.mCode = resp.mCode;
		this.mMessage = resp.mMessage;
		this.mException = resp.mException;
		this.mHeaderParams.putAll(resp.mHeaderParams);
	}

	public HttpResponse(Object body) {
		this.mBody = body;
	}

    public HttpResponse() {
    }

	public Object getBody() {
		return mBody;
	}

	protected void setBody(Object body) {
		this.mBody = body;
	}

	/**
	 * Reads the body data from the supplied {@link BufferedReader}.<br />
     * Stops the process if the request is cancelled.
	 * @param inStream the input stream
	 * @param request the request
	 * @throws IOException
	 */
	public void readBody(InputStream inStream, HttpRequest request) throws Throwable {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
		StringBuilder builder = new StringBuilder();

		String buffer;
		while ((buffer = reader.readLine()) != null && !request.isCancelled()) {
			builder.append(buffer);
		}

		mBody = builder.toString();
	}

    public int getCode() {
        return mCode;
    }

    public void setCode(int mCode) {
        this.mCode = mCode;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String mMessage) {
        this.mMessage = mMessage;
    }

    public Throwable getException() {
        return mException;
    }

    public void setException(Throwable mException) {
        this.mException = mException;
    }

    /**
	 * Gets the response header parameters.
	 * @return the header parameter map
	 */
	public Map<String, String> getHeaderParams() {
		return mHeaderParams;
	}

	/**
	 * Sets a header parameter.
	 * @param key the header param key
	 * @param value the header param value
	 * @return the {@link HttpResponse} (this)
	 */
	public HttpResponse setHeaderParam(String key, String value) {
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

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mCode);
        out.writeMap(mHeaderParams);
        out.writeString(mMessage);
        out.writeSerializable(mException);
		if(mBody != null && mBody instanceof Serializable) {
			out.writeSerializable((Serializable) mBody);
		}
	}

	public static final Creator<HttpResponse> CREATOR = new Creator<HttpResponse>() {
		public HttpResponse createFromParcel(Parcel in) {
			return new HttpResponse(in);
		}
		public HttpResponse[] newArray(int size) {
			return new HttpResponse[size];
		}
	};

    protected HttpResponse(Parcel in) {
        mCode = in.readInt();
        mHeaderParams = in.readHashMap(null);
        mMessage = in.readString();
        mException = (Throwable) in.readSerializable();
        mBody = in.readSerializable();
    }
}
