package com.peppermint.app.rest;

import java.util.List;

public class HttpWrappedResponseBody<T> {

	private int code;
	private String message;
	private String date;
	private List<T> data;
	
	public HttpWrappedResponseBody() {
		this.code = 0; // OK
		//FIXME this is giving an exception so it's commented for now...
		//this.date = new DateContainer(DateContainerType.DATETIME).getValue();
	}
	
	public HttpWrappedResponseBody(int code, String message, String date, List<T> data) {
		super();
		this.code = code;
		this.message = message;
		this.date = date;
		this.data = data;
	}

    public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "HttpWrappedResponseBody [code=" + code + ", message=" + message
				+ ", date=" + date + ", data=" + data + "]";
	}
}