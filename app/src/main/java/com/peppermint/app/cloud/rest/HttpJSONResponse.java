package com.peppermint.app.cloud.rest;

import android.os.Parcel;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 02-07-2015.
 *
 * <p>
 *     Represents an HTTP REST response.
 * </p>
 */
public class HttpJSONResponse<T extends Serializable> extends HttpResponse {

    private JSONParser<T> mBodyParser;
    private T mBody;

    /**
     * Copy constructor
     * @param resp the {@link HttpJSONResponse to be copied}
     */
	public HttpJSONResponse(HttpJSONResponse resp) {
		super(resp);
	}

	public HttpJSONResponse(JSONParser<T> bodyParser) {
		this.mBodyParser = bodyParser;
	}

    public HttpJSONResponse() {
		super();
    }

	/**
	 * Reads the body data from the supplied {@link BufferedReader}.<br />
     * Stops the process if the request is cancelled.
	 * @param inStream the input stream
	 * @param request the request
	 * @throws IOException
	 */
	public void readBody(InputStream inStream, HttpRequest request) throws Throwable {
		super.readBody(inStream, request);

        if(getCode() / 100 != 2) {
            return;
        }

        String bodyString = (String) super.getBody();

        mBody = null;
        if(bodyString != null) {
            JSONObject obj = new JSONObject(bodyString);
            mBody = mBodyParser.processJson(obj);
        }
	}

    public T getJsonBody() {
        return mBody;
    }

	@Override
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeSerializable(mBody);
		out.writeSerializable(mBodyParser);
	}

	public static final Creator<HttpJSONResponse> CREATOR = new Creator<HttpJSONResponse>() {
		public HttpJSONResponse createFromParcel(Parcel in) {
			return new HttpJSONResponse(in);
		}
		public HttpJSONResponse[] newArray(int size) {
			return new HttpJSONResponse[size];
		}
	};

	protected HttpJSONResponse(Parcel in) {
		super(in);
		mBody = (T) in.readSerializable();
		mBodyParser = (JSONParser<T>) in.readSerializable();
	}
}
