package com.peppermint.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.utils.ScriptFileReader;

/**
 * Created by Nuno Luz on 05/10/2015.
 *
 * Database helper class for initialization, migration and connect operations.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = DatabaseHelper.class.getSimpleName();
	
	private static final String DATABASE_NAME = "peppermint.db";    // database filename
	public static final int DATABASE_VERSION = 9;                   // database version
	private Context mContext;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.mContext = context;
	}

    /**
     * Reads and executes single line SQL instructions from a resource text file.
     * Ignores empty lines and lines starting with "--" (comments);
     *
     * @param fileRes the resource id of the text file
     * @param _db the database connection
     * @throws Exception
     */
	protected void execSQLScript(int fileRes, SQLiteDatabase _db) throws Exception {
		ScriptFileReader sfr = new ScriptFileReader(mContext, fileRes);
		sfr.open();

		String str = sfr.nextLine();
		while (str != null) {
			if (str.trim().length() > 0 && !str.startsWith("--")) {
                _db.execSQL(str);
            }
			str = sfr.nextLine();
		}
		sfr.close();
	}

    /**
     * Called when no database exists and the helper class needs to create a new one.
     * Executes the SQL instructions inside the raw/db_create.sql file.
     * @param _db the database connection
     */
	@Override
	public void onCreate(SQLiteDatabase _db) {
		try {
			execSQLScript(R.raw.db_create, _db);
		} catch (Exception e) {
			Log.e(TAG, "Unable to create database", e);
		}
	}

    /**
     * Drops all tables in the database.
     * Executes the SQL instructions inside the raw/db_drop.sql file.
     * @param _db the database connection
     */
	protected void dropAll(SQLiteDatabase _db) {
		try {
			execSQLScript(R.raw.db_drop, _db);
		} catch (Exception e) {
			Log.e(TAG, "Unable to remove database", e);
		}
	}

    /**
     * Called when there is a database version mismatch meaning that the database needs to be
     * upgraded to the new version.
     *
     * @param _db the database connection
     * @param _oldVersion the old version
     * @param _newVersion the new version
     */
	@Override
	public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion) {
		dropAll(_db);
		onCreate(_db);
	}
}