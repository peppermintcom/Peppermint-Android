package com.peppermint.app.dal;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.dal.chat.ChatManager;
import com.peppermint.app.dal.contact.ContactManager;
import com.peppermint.app.dal.recipient.RecipientManager;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.contact.ContactData;
import com.peppermint.app.dal.contact.ContactRaw;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.ScriptFileReader;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Nuno Luz on 05/10/2015.
 *
 * Database helper class for initialization, migration and connect operations.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	public static synchronized DatabaseHelper getInstance(Context context) {
        if(INSTANCE == null) {
            INSTANCE = new DatabaseHelper(context.getApplicationContext());
        }
        return INSTANCE;
    }

	public static synchronized void clearInstance() {
		if(INSTANCE != null) {
			INSTANCE.close();
			INSTANCE = null;
		}
	}

    private static DatabaseHelper INSTANCE;

	private static final String TAG = DatabaseHelper.class.getSimpleName();
	private static final String DATABASE_NAME = "peppermint.db";        // database filename
	private static final int DATABASE_VERSION = 20;                     // database version

	private Context mContext;
    private ReentrantLock mLock = new ReentrantLock();

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.mContext = context;
	}

    public void lock() {
        mLock.lock();
    }

    public void unlock() {
		mLock.unlock();
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
			TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
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
			TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
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
		if(_oldVersion < 15) {
            Log.d(TAG, "Updating Database: v15 Reset...");

            dropAll(_db);
            onCreate(_db);

			// move recent contacts to chats
			SenderPreferences prefs = new SenderPreferences(mContext);
			List<Long> contactIds = prefs.getRecentContactUris();
			if(contactIds == null) {
				return;
			}

			DateContainer dateContainer = new DateContainer(DateContainer.TYPE_DATETIME);
			try {
				String oldestTimestamp = ChatManager.getInstance(mContext).getOldestChatTimestamp(_db);
				if(oldestTimestamp != null) {
					dateContainer.setFromString(oldestTimestamp);
				}
			} catch (ParseException e) {
				TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
			}

			Calendar calendar = dateContainer.getCalendar();
			calendar.add(Calendar.MINUTE, -1);

			for (Long contactId : contactIds) {
                ContactRaw rawContact = ContactManager.getInstance().getRawContactByDataId(mContext, contactId);
                String utcTimestamp = dateContainer.getAsString(DateContainer.UTC);

                _db.beginTransaction();

                try {
                    Recipient recipient = new Recipient(rawContact, utcTimestamp);
                    RecipientManager.getInstance(mContext).insertOrUpdate(_db, recipient);

					Chat newChat = new Chat(0, recipient.getDisplayName(), utcTimestamp, recipient);
                    ChatManager.getInstance(mContext).insert(_db, newChat);

                    _db.setTransactionSuccessful();
                } catch (SQLException e) {
                    TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
                } finally {
                    _db.endTransaction();
                }

                calendar.add(Calendar.MINUTE, -1);
			}

            return;
		}

		if(_oldVersion < 17) {
            Log.d(TAG, "Updating Database: v17 Refactoring...");

			try {
				execSQLScript(R.raw.db_17_refactor, _db);
			} catch (Exception e) {
				TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
			}

			// set is_peppermint for all recipients
			Cursor recipientCursor = RecipientManager.getInstance(mContext).getAll(_db);
			try {
				while (recipientCursor.moveToNext()) {
                    Recipient recipient = RecipientManager.getInstance(mContext).getFromCursor(_db, recipientCursor);

                    ContactRaw contactRaw = ContactManager.getInstance().getRawContactByDataId(mContext, recipient.getDroidContactDataId());
					if(contactRaw != null) {
						recipient.setDroidContactId(contactRaw.getContactId());

						ContactData peppermintData = ContactManager.getInstance().getPeppermintContactByContactIdAndVia(mContext, recipient.getDroidContactId(), recipient.getVia());
						if(peppermintData != null) {
							recipient.setPeppermint(true);
						}

						RecipientManager.getInstance(mContext).update(_db, recipient);
					}
				}
			} catch (SQLException e) {
                TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
            } finally {
                recipientCursor.close();
			}
		}

		if(_oldVersion < 18) {

			try {
				ChatManager.getInstance(mContext).getChatCount(_db, true);
			} catch(RuntimeException e) {
				TrackerManager.getInstance(mContext).log("Issue with local database. Resetting...", e);
				dropAll(_db);
				onCreate(_db);
				return;
			}

			// delete all phone contact conversations (no longer supported)
			Set<Long> idsToDelete = new HashSet<>();

			Cursor chatCursor = ChatManager.getInstance(mContext).getAll(_db, false);
			while(chatCursor.moveToNext()) {
				Chat chat = ChatManager.getInstance(mContext).getFromCursor(_db, chatCursor);
				if(chat.getRecipientList() == null || chat.getRecipientList().size() <= 0 || chat.getRecipientList().get(0).getMimeType().compareTo(ContactData.PHONE_MIMETYPE) == 0) {
					idsToDelete.add(chat.getId());
				}
			}
			chatCursor.close();

			for(long id : idsToDelete) {
				try {
					ChatManager.getInstance(mContext).delete(_db, id);
				} catch (SQLException e) {
					TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
				}
			}
		}

		if(_oldVersion < 19) {
			Log.d(TAG, "Updating Database: v19 Pending Logout...");

			try {
				execSQLScript(R.raw.db_19_pending_logout, _db);
			} catch (Exception e) {
				TrackerManager.getInstance(mContext).logException(e);
			}
		}

		if(_oldVersion < 20) {
			Log.d(TAG, "Updating Database: v20 Transcription...");

			try {
				execSQLScript(R.raw.db_20_transcription, _db);
			} catch (Exception e) {
				TrackerManager.getInstance(mContext).logException(e);
			}
		}
	}
}