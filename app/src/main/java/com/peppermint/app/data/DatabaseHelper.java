package com.peppermint.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.utils.ScriptFileReader;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = DatabaseHelper.class.getSimpleName();
	
	private static final String DATABASE_NAME = "peppermint.db";
	public static final int DATABASE_VERSION = 8;
	private Context mContext;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.mContext = context;
	}

	protected void execSQLScript(int fileRes, SQLiteDatabase _db) throws Exception {
		ScriptFileReader sfr = new ScriptFileReader(mContext, fileRes);
		sfr.open();

		String str = sfr.nextLine();
		while (str != null) {
			if (str.trim().length() > 0 && !str.startsWith("--"))
				_db.execSQL(str);

			str = sfr.nextLine();
		}

		sfr.close();
	}

	// Called when no database exists and the helper class needs to create a new one.
	@Override
	public void onCreate(SQLiteDatabase _db) {
		try {
			execSQLScript(R.raw.db_create, _db);
		} catch (Exception e) {
			Log.e(TAG, "Unable to create database", e);
		}
	}

	protected void dropAll(SQLiteDatabase _db) {
		try {
			execSQLScript(R.raw.db_drop, _db);
		} catch (Exception e) {
			Log.e(TAG, "Unable to remove database", e);
		}
	}

	// Called when there is a database version mismatch meaning that the version of the database 
	// on disk needs to be upgraded to the current version.
	@Override
	public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion) {
		dropAll(_db);
		onCreate(_db);
	}
}