package com.peppermint.app.cloud;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.peppermint.app.cloud.apis.data.MessageListResponse;
import com.peppermint.app.cloud.apis.data.MessagesResponse;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.cloud.senders.SenderSupportListener;
import com.peppermint.app.cloud.senders.SenderSupportTask;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.GlobalManager;
import com.peppermint.app.data.Message;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.SignOutEvent;
import com.peppermint.app.tracking.TrackerManager;
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

    private DatabaseHelper mDatabaseHelper;
    private boolean mNeverSyncedBefore = false;

    private List<Message> _receivedMessages = new ArrayList<>();
    private List<Message> _sentMessages = new ArrayList<>();
    private String mLocalEmailAddress;

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
            weekAgo.getCalendar().add(Calendar.DAY_OF_YEAR, -7);
            originalSyncTimestamp = weekAgo.toString();
        }

        String syncTimestamp = originalSyncTimestamp;
        String nextUrl;

        do {
            MessageListResponse receivedResponse = getPeppermintApi().getMessages(getId().toString(), serverAccountId, syncTimestamp, true);

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
                    } catch (Exception e) {
                        getTrackerManager().logException(e);
                    } finally {
                        db.endTransaction();
                        mDatabaseHelper.unlock();
                    }
                }
            }

            nextUrl = receivedResponse.getNextUrl();
            if (nextUrl != null) {
                Uri uri = Uri.parse(nextUrl);
                syncTimestamp = uri.getQueryParameter("since");
            }
        } while (nextUrl != null && !isCancelled());

        // SENT MESSAGES
        syncTimestamp = originalSyncTimestamp;

        do {
            MessageListResponse sentResponse = getPeppermintApi().getMessages(getId().toString(), serverAccountId, syncTimestamp, false);
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
                } catch (Exception e) {
                    getTrackerManager().logException(e);
                } finally {
                    db.endTransaction();
                    mDatabaseHelper.unlock();
                }
            }

            nextUrl = sentResponse.getNextUrl();
            if (nextUrl != null) {
                Uri uri = Uri.parse(nextUrl);
                syncTimestamp = uri.getQueryParameter("since");
            }

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
        if(getError() == null) {
            getSenderPreferences().setLastSyncTimestamp(DateContainer.getCurrentUTCTimestamp());
        }
    }

    public List<Message> getReceivedMessages() {
        return _receivedMessages;
    }

    public List<Message> getSentMessages() {
        return _sentMessages;
    }

}
