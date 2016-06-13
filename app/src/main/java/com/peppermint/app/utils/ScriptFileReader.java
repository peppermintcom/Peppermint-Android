package com.peppermint.app.utils;

import android.content.Context;

import com.peppermint.app.trackers.TrackerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utility class to read SQL instructions from a raw .SQL file.
 */
public class ScriptFileReader {

	private Context mContext;
	private BufferedReader mBufferedReader = null;
	private int mResourceId;

	public ScriptFileReader(Context mContext, int mResourceId) {
		this.mContext = mContext;
		this.mResourceId = mResourceId;
	}

	public void open() {
		mBufferedReader = new BufferedReader(new InputStreamReader(mContext.getResources().openRawResource(mResourceId)));
	}

	public void close() {
		if (mBufferedReader != null) {
			try {
				mBufferedReader.close();
			} catch (IOException e) {
				TrackerManager.getInstance(mContext).logException(e);
			}
			mBufferedReader = null;
		}
	}

	public String nextLine() {
		if (mBufferedReader != null) {
			try {
				return mBufferedReader.readLine();
			} catch (IOException e) {
				TrackerManager.getInstance(mContext).logException(e);
			}
		}
		return null;
	}
}