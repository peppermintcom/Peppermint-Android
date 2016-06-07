package com.peppermint.app.services.messenger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.peppermint.app.cloud.apis.peppermint.objects.MessageListResponse;
import com.peppermint.app.cloud.apis.peppermint.objects.MessagesResponse;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.services.messenger.handlers.SenderSupportListener;
import com.peppermint.app.services.messenger.handlers.SenderSupportTask;
import com.peppermint.app.dal.DatabaseHelper;
import com.peppermint.app.dal.GlobalManager;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.PeppermintEventBus;
import com.peppermint.app.services.authenticator.SignOutEvent;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.utils.DateContainer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Sync Peppermint messages.
 * </p>
 */
public class MessagesSyncTask extends SenderSupportTask {

    private static final String TAG = MessagesSyncTask.class.getSimpleName();

    private static final int PROGRESS_EVENT_MSG_AMOUNT = 20;
    private static final int PROGRESS_BETWEEN_MS = 1000;

    private DatabaseHelper mDatabaseHelper;
    private boolean mNeverSyncedBefore = false;

    private List<Message> _receivedMessages = new ArrayList<>();
    private List<Message> _sentMessages = new ArrayList<>();
    private String mLocalEmailAddress;

    private String mLastMessageTimestamp;
    private boolean mLocalError = false;

    public MessagesSyncTask(Context context, SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        getIdentity().setContext(context);
        getIdentity().setTrackerManager(TrackerManager.getInstance(context.getApplicationContext()));
        getIdentity().setPreferences(new SenderPreferences(context));

        mDatabaseHelper = DatabaseHelper.getInstance(context);
    }

    @Override
    protected void execute() throws Throwable {
        Log.d(TAG, "Starting Message Sync...");

        setupPeppermintAuthentication();
        String serverAccountId = getAuthenticationData().getAccountServerId();
        mLocalEmailAddress = getAuthenticationData().getEmail();

        // RECEIVED MESSAGES
        String originalSyncTimestamp = getSenderPreferences().getLastSyncTimestamp();
        if(originalSyncTimestamp == null) {
            mNeverSyncedBefore = true;
            DateContainer weekAgo = new DateContainer(DateContainer.TYPE_DATETIME);
            weekAgo.getCalendar().add(Calendar.DAY_OF_YEAR, -15);
            originalSyncTimestamp = weekAgo.toString();
        }

        String nextUrl = null;

        do {
            MessageListResponse receivedResponse = nextUrl == null ?
                    getPeppermintApi().getMessages(getId().toString(), serverAccountId, originalSyncTimestamp, true) :
                    getPeppermintApi().getMessages(getId().toString(), nextUrl);

            int receivedAmount = receivedResponse.getMessages().size();
            for (int i=0; i<receivedAmount && !isCancelled(); i++) {
                MessagesResponse messagesResponse = receivedResponse.getMessages().get(i);

                if(mLocalEmailAddress.compareToIgnoreCase(messagesResponse.getRecipientEmail().trim()) != 0) {
                    getTrackerManager().log("Received wrong message from GCM! Should have gone to email " + messagesResponse.getRecipientEmail());
                } else {

                    mDatabaseHelper.lock();
                    SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                    db.beginTransaction();

                    try {
                        Message message = GlobalManager.insertReceivedMessage(getContext(), db, messagesResponse.getRecipientEmail(), messagesResponse.getSenderName(),
                                messagesResponse.getSenderEmail(), messagesResponse.getAudioUrl(), messagesResponse.getMessageId(), messagesResponse.getTranscription(),
                                messagesResponse.getCreatedTimestamp(), messagesResponse.getDuration(), messagesResponse.getReadTimestamp());
                        if (message != null) {
                            _receivedMessages.add(message);
                            if(!mNeverSyncedBefore && message.getParameter(Message.PARAM_INSERTED) != null &&
                                    ((boolean) message.getParameter(Message.PARAM_INSERTED))) {
                                PeppermintEventBus.postReceiverEvent(mLocalEmailAddress, message);
                            }
                        }
                        db.setTransactionSuccessful();
                        if(mLastMessageTimestamp == null || mLastMessageTimestamp.compareTo(message.getRegistrationTimestamp()) < 0) {
                            mLastMessageTimestamp = message.getRegistrationTimestamp();
                        }
                    } catch (Exception e) {
                        getTrackerManager().logException(e);
                        mLocalError = true;
                    } finally {
                        db.endTransaction();
                        mDatabaseHelper.unlock();
                    }

                    if(i%PROGRESS_EVENT_MSG_AMOUNT==0) {
                        publishProgress(0f);
                        try {
                            Thread.sleep(PROGRESS_BETWEEN_MS);
                        } catch (InterruptedException e) {
                            /* nothing to do here */
                        }
                    }
                }
            }

            nextUrl = receivedResponse.getNextUrl();
        } while (nextUrl != null && !isCancelled());

        publishProgress(0f);

        // SENT MESSAGES
        do {
            MessageListResponse sentResponse = nextUrl == null ?
                    getPeppermintApi().getMessages(getId().toString(), serverAccountId, originalSyncTimestamp, false) :
                    getPeppermintApi().getMessages(getId().toString(), nextUrl);

            int sentAmount = sentResponse.getMessages().size();
            for (int i=0; i<sentAmount && !isCancelled(); i++) {
                MessagesResponse messagesResponse = sentResponse.getMessages().get(i);

                mDatabaseHelper.lock();
                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    Message message = GlobalManager.insertSentMessage(getContext(), db, null, messagesResponse.getRecipientEmail(), messagesResponse.getSenderEmail(),
                            messagesResponse.getAudioUrl(), messagesResponse.getMessageId(), messagesResponse.getTranscription(), messagesResponse.getCreatedTimestamp(),
                            messagesResponse.getDuration());
                    if (message != null) {
                        _sentMessages.add(message);
                    }
                    db.setTransactionSuccessful();
                    if(mLastMessageTimestamp == null || mLastMessageTimestamp.compareTo(message.getRegistrationTimestamp()) < 0) {
                        mLastMessageTimestamp = message.getRegistrationTimestamp();
                    }
                } catch (Exception e) {
                    getTrackerManager().logException(e);
                    mLocalError = true;
                } finally {
                    db.endTransaction();
                    mDatabaseHelper.unlock();
                }

                if(i%PROGRESS_EVENT_MSG_AMOUNT==0) {
                    publishProgress(0f);
                    try {
                        Thread.sleep(PROGRESS_BETWEEN_MS);
                    } catch (InterruptedException e) {
                            /* nothing to do here */
                    }
                }
            }

            nextUrl = sentResponse.getNextUrl();
        } while (nextUrl != null && !isCancelled());

        GlobalManager.clearCache();
    }

    public void onEventMainThread(SignOutEvent event) {
        cancel(true);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        PeppermintEventBus.register(this);
    }

    @Override
    protected void onCancelled(Void aVoid) {
        super.onCancelled(aVoid);
        PeppermintEventBus.unregister(this);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        PeppermintEventBus.unregister(this);
        if(getError() == null && !mLocalError && mLastMessageTimestamp != null) {
            Log.d(TAG, "New Messages Sync Date: " + mLastMessageTimestamp);
            getSenderPreferences().setLastSyncTimestamp(mLastMessageTimestamp);
        }
    }

    public List<Message> getReceivedMessages() {
        return _receivedMessages;
    }

    public List<Message> getSentMessages() {
        return _sentMessages;
    }

}
