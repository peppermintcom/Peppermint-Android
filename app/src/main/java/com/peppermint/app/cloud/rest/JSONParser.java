package com.peppermint.app.cloud.rest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Represents a JSON parser for Java objects
 * @param <T> the Java object class
 */
public interface JSONParser<T> extends Serializable {
	T processJson(JSONObject obj) throws JSONException;
    JSONObject toJson(T inst) throws JSONException;
}