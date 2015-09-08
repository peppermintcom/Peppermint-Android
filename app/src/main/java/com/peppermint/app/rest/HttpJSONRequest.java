package com.peppermint.app.rest;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class HttpJSONRequest<T> extends HttpRequest {

    protected JSONParser<T> mParser;
    protected List<T> mBody;

    public HttpJSONRequest(HttpJSONRequest<T> req) {
        super(req);
        this.mParser = req.mParser;
    }

    public HttpJSONRequest(String endpoint, JSONParser<T> parser) {
        super(endpoint);
        this.mParser = parser;
    }

    public HttpJSONRequest(String endpoint, int requestMethod, JSONParser<T> parser) {
        super(endpoint, requestMethod);
        this.mParser = parser;
    }

    public HttpJSONRequest(String endpoint, int requestMethod, boolean forceOnlyGetAndPost, JSONParser<T> parser) {
        super(endpoint, requestMethod, forceOnlyGetAndPost);
        this.mParser = parser;
    }

    @Override
    public void writeBody(OutputStream outStream) throws IOException {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(outStream);
            if (mBody != null) {
                if (mBody.size() == 1) {
                    if (mParser != null) {
                        writer.write(mParser.toJson(mBody.get(0)).toString());
                    } else {
                        writer.write(mBody.get(0).toString());
                    }
                } else {
                    JSONArray arr = new JSONArray();
                    for(T t : mBody) {
                        if (mParser != null) {
                            arr.put(mParser.toJson(t));
                        } else {
                            arr.put(t);
                        }
                    }
                    writer.write(arr.toString());
                }
            }
        } catch(JSONException e) {
            throw new RuntimeException("Error parsing JSON!", e);
        }
    }

    public List<T> getJsonBody() {
        return mBody;
    }

    public void setJsonBody(T body) {
        mBody = new ArrayList<>();
        mBody.add(body);
    }

    public void setJsonBody(List<T> body) {
        mBody = body;
    }
}
