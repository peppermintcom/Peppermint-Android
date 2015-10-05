package com.peppermint.app.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;

public class ScriptFileReader {

	private Context c;
	private BufferedReader br = null;
	private int r_id;

	public ScriptFileReader(Context c, int r_id) {
		this.c = c;
		this.r_id = r_id;
	}

	public void open() {
		br = new BufferedReader(new InputStreamReader(c.getResources().openRawResource(r_id)));
	}

	public void close() {
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {
			}
			br = null;
		}
	}

	public String nextLine() {
		if (br != null) {
			try {
				return br.readLine();
			} catch (IOException e) {
			}
		}
		return null;
	}
}