package com.peppermint.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.ScriptFileReader;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
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
		INSTANCE = null;
	}

    private static DatabaseHelper INSTANCE;

	private static final String TAG = DatabaseHelper.class.getSimpleName();
	
	private static final String DATABASE_NAME = "peppermint.db";        // database filename
	private static final int DATABASE_VERSION = 15;                     // database version

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

    public boolean tryLock() {
        return mLock.tryLock();
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
		if(_oldVersion < 15) {
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
				String oldestTimestamp = ChatManager.getOldestChatTimestamp(_db);
				if(oldestTimestamp != null) {
					dateContainer.setFromString(oldestTimestamp);
				}
			} catch (ParseException e) {
				TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
			}

			Calendar calendar = dateContainer.getCalendar();
			calendar.add(Calendar.MINUTE, -1);

			for (Long contactId : contactIds) {
                ChatRecipient recipient = new ChatRecipient();
                recipient.setContactId(contactId);
				Chat chat = ChatManager.getChatByRecipients(_db, recipient);
				if(chat == null) {
					ContactRaw rawContact = ContactManager.getRawContactByContactId(mContext, contactId);
                    recipient.setFromRawContact(rawContact);
					try {
						ChatManager.insert(mContext, _db, recipient.getDisplayName(), dateContainer.getAsString(DateContainer.UTC), recipient);
					} catch (SQLException e) {
						TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
					}
					calendar.add(Calendar.MINUTE, -1);
				}
			}
		}

		/*if(_oldVersion < 15) {
            // remove recipient table and use only contacts provider
            // also store recent contacts in database instead of prefs.
            try {
                execSQLScript(R.raw.db_drop_recipient, _db);
            } catch (Exception e) {
                Log.e(TAG, "Unable to remove database", e);
            }

            // move recent contacts to chats
			SenderPreferences prefs = new SenderPreferences(mContext);
			List<Long> contactIds = prefs.getRecentContactUris();
			if(contactIds == null) {
				return;
			}

			DateContainer dateContainer = new DateContainer(DateContainer.TYPE_DATETIME);
			try {
				String oldestTimestamp = ChatManager.getOldestChatTimestamp(_db);
				if(oldestTimestamp != null) {
					dateContainer.setFromString(oldestTimestamp);
				}
			} catch (ParseException e) {
				TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
			}

			Calendar calendar = dateContainer.getCalendar();
			calendar.add(Calendar.MINUTE, -1);

			for (Long contactId : contactIds) {
				Chat chat = ChatManager.getChatByMainRecipient(mContext, _db, contactId, null);
				if(chat == null) {
                    ContactRaw recipient = ContactManager.getRecipientByContactId(mContext, contactId);
					try {
						ChatManager.insert(_db, contactId, recipient.getDisplayName(), recipient.getContactVia(), recipient.getContactMimetype(), dateContainer.getAsString(DateContainer.UTC));
					} catch (SQLException e) {
						TrackerManager.getInstance(mContext.getApplicationContext()).logException(e);
					}
					calendar.add(Calendar.MINUTE, -1);
				}
			}
		}

		if(_oldVersion < 16) {
			// add extra data fields to tbl_chat and tbl_message
			// to include min. required recipient data in case the
			// recipient gets deleted from contacts
			try {
				execSQLScript(R.raw.db_16_add_fields, _db);

                // make sure that all chat data is valid
                // delete if not; if this happens, it's only for a really low amount of cases
                Cursor cursor = ChatManager.getAll(_db);
                while(cursor.moveToNext()) {
                    Chat chat = ChatManager.getFromCursor(mContext, cursor);
                    ContactRaw recipient = chat.getMainRecipientParameter();
                    if(recipient != null && recipient.getDisplayName() != null && recipient.getContactVia() != null) {
                        ChatManager.update(_db, chat.getId(), chat.getMainRecipientId(),
                                recipient.getDisplayName(), recipient.getContactVia(),
                                recipient.getContactMimetype(), chat.getLastMessageTimestamp());

                        Cursor msgCursor = MessageManager.getByChatId(_db, chat.getId());
                        while(msgCursor.moveToNext()) {
                            Message message = MessageManager.getFromCursor(mContext, null, msgCursor);
                            MessageManager.update(_db, message.getId(), message.getChatId(), message.getRecipientContactId(), message.getRecordingId(),
                                    message.getServerId(), message.getServerShortUrl(), message.getServerCanonicalUrl(), message.getServerTranscriptionUrl(),
                                    message.getEmailSubject(), message.getEmailBody(), message.getRegistrationTimestamp(), message.isSent(), message.isReceived(),
                                    message.isPlayed(), recipient.getDisplayName(), recipient.getContactVia(), recipient.getContactMimetype());
                        }
                        msgCursor.close();

                    } else {
						ChatManager.delete(_db, chat.getId());
					}
                }
                cursor.close();
			} catch (Exception e) {
				Log.e(TAG, "Unable to add chat table fields", e);
			}
		}*/
	}
}