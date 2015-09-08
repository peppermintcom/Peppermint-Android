package com.peppermint.app.rest;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSONParser<T> {
	T processJson(JSONObject obj) throws JSONException;
    JSONObject toJson(T inst) throws JSONException;
}