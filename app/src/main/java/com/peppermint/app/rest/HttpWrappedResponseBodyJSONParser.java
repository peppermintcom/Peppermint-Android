package com.peppermint.app.rest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class HttpWrappedResponseBodyJSONParser<T> implements JSONParser<HttpWrappedResponseBody<T>> {

    protected JSONParser<T> mDataParser;

    public HttpWrappedResponseBodyJSONParser(JSONParser<T> dataParser) {
        this.mDataParser = dataParser;
    }

    @Override
    public HttpWrappedResponseBody<T> processJson(JSONObject obj) throws JSONException {
        HttpWrappedResponseBody<T> body = new HttpWrappedResponseBody<>();
        body.setCode(obj.getInt("code"));
        body.setMessage(obj.getString("msg"));

        if(obj.optString("data") != null) {

            List<T> list = new ArrayList<>();

            try {
                if(mDataParser != null) {
                    list.add(mDataParser.processJson(new JSONObject(obj.getString("data"))));
                } else {
                    list.add((T) obj.get("data"));
                }
            } catch(JSONException e) {
                JSONArray arr = new JSONArray(obj.getString("data"));
                for(int i=0; i<arr.length(); i++) {
                    if(mDataParser != null) {
                        list.add(mDataParser.processJson(arr.getJSONObject(i)));
                    } else {
                        list.add((T) arr.get(i));
                    }
                }
            }

            body.setData(list);
        }

        body.setDate(obj.getString("date"));
        return body;
    }

    @Override
    public JSONObject toJson(HttpWrappedResponseBody<T> inst) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("code", inst.getCode());
        obj.put("msg", inst.getMessage());

        if(inst.getData() != null && inst.getData().size() > 0) {
            List<T> list = inst.getData();
            if(list.size() <= 1) {
                if(mDataParser != null) {
                    obj.put("data", mDataParser.toJson(list.get(0)).toString());
                } else {
                    obj.put("data", list.get(0));
                }
            } else {
                JSONArray arr = new JSONArray();
                for(T t : list) {
                    if(mDataParser != null) {
                        arr.put(mDataParser.toJson(t));
                    } else {
                        arr.put(t);
                    }
                }
                obj.put("data", arr.toString());
            }
        }

        obj.put("date", inst.getDate());
        return obj;
    }
}
