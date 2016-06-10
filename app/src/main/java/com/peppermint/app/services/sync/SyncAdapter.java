package com.peppermint.app.services.sync;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.peppermint.app.cloud.apis.peppermint.PeppermintApi;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.cloud.apis.peppermint.objects.MessageListResponse;
import com.peppermint.app.cloud.apis.peppermint.objects.MessagesResponse;
import com.peppermint.app.dal.DatabaseHelper;
import com.peppermint.app.dal.GlobalManager;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.pendinglogout.PendingLogout;
import com.peppermint.app.dal.pendinglogout.PendingLogoutManager;
import com.peppermint.app.services.authenticator.AuthenticatorConstants;
import com.peppermint.app.trackers.TrackerApi;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.PermissionsPolicyEnforcer;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.io.InterruptedIOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Nuno Luz on 07-06-2016.
 *
 * Peppermint's synchronization adapter, according to the native Android framework.
 *
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();

    private static final String LAST_SYNC_TIMESTAMP_KEY = TAG + "_lastSyncTimestamp";

    private static final int PROGRESS_BETWEEN_MS = 5000;

    private AccountManager mAccountManager;
    private SharedPreferences mSharedPreferences;

    private Set<Long> mReceivedMessageIds;
    private Set<Long> mSentMessageIds;
    private Set<Long> mAffectedChatIds;
    private boolean mLocalError = false;
    private String mLastMessageTimestamp;
    private long mLastProgressMs;

    private PermissionsPolicyEnforcer mPermissionsManager = new PermissionsPolicyEnforcer(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE);

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        init(context);
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        init(context);
    }

    private void init(Context context) {
        mAccountManager = AccountManager.get(context);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "Starting Sync...");

        final Context context = getContext();

        if(!Utils.isInternetAvailable(context) || !Utils.isInternetActive()) {
            Log.w(TAG, "Can't Sync: No Internet connection!");
            return;
        }

        doPendingLogouts();

        if(mPermissionsManager.getPermissionsToAsk(context).size() > 0) {
            Log.w(TAG, "Can't Sync: Permissions are required...");
            return;
        }

        final PeppermintApi peppermintApi = new PeppermintApi(context);
        try {
            peppermintApi.setAuthenticationToken(mAccountManager.blockingGetAuthToken(account, AuthenticatorConstants.FULL_TOKEN_TYPE, true));
        } catch (Throwable e) {
            Log.w(TAG, "Can't Sync: Error getting authentication token!", e);
            TrackerManager.getInstance(context).logException(e);
            return;
        }

        SyncService.postSyncEvent(SyncEvent.EVENT_STARTED, null, null, null, null);
        mLastProgressMs = System.currentTimeMillis();

        mReceivedMessageIds = new HashSet<>();
        mSentMessageIds = new HashSet<>();
        mAffectedChatIds = new HashSet<>();
        mLocalError = false;
        mLastMessageTimestamp = null;

        String serverAccountId = mAccountManager.getUserData(account, AuthenticatorConstants.ACCOUNT_PARAM_ACCOUNT_SERVER_ID);
        String localEmailAddress = mAccountManager.getUserData(account, AuthenticatorConstants.ACCOUNT_PARAM_EMAIL);

        String originalSyncTimestamp = getLastSyncTimestamp();
        if(originalSyncTimestamp == null) {
            DateContainer weekAgo = new DateContainer(DateContainer.TYPE_DATETIME);
            weekAgo.getCalendar().add(Calendar.DAY_OF_YEAR, -15);
            originalSyncTimestamp = weekAgo.toString();
        }

        try {
            String nextUrl = null;

            // RECEIVED MESSAGES
            do {
                MessageListResponse receivedResponse = nextUrl == null ?
                        peppermintApi.getMessages(null, serverAccountId, originalSyncTimestamp, true, true) :
                        peppermintApi.getMessages(null, nextUrl);
                processMessages(receivedResponse, mReceivedMessageIds, false, localEmailAddress);
                nextUrl = receivedResponse.getNextUrl();

                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    /* nothing to do here */
                }
            } while (nextUrl != null);

            // SENT MESSAGES
            do {
                MessageListResponse sentResponse = nextUrl == null ?
                        peppermintApi.getMessages(null, serverAccountId, originalSyncTimestamp, false, true) :
                        peppermintApi.getMessages(null, nextUrl);
                processMessages(sentResponse, mSentMessageIds, true, localEmailAddress);
                nextUrl = sentResponse.getNextUrl();

                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    /* nothing to do here */
                }
            } while (nextUrl != null);

        } catch(InterruptedIOException | InterruptedException e) {
            Log.w(TAG, "Cancelled Sync...", e);
            SyncService.postSyncEvent(SyncEvent.EVENT_CANCELLED, mReceivedMessageIds, mSentMessageIds, mAffectedChatIds, null);
        } catch(Exception e) {
            TrackerManager.getInstance(context).logException(e);
            SyncService.postSyncEvent(SyncEvent.EVENT_ERROR, mReceivedMessageIds, mSentMessageIds, mAffectedChatIds, null);
        } finally {
            GlobalManager.getInstance(context).clearCache();
        }

        if(!mLocalError && mLastMessageTimestamp != null) {
            Log.d(TAG, "New Sync Date: " + mLastMessageTimestamp);
            setLastSyncTimestamp(mLastMessageTimestamp);
        }

        SyncService.postSyncEvent(SyncEvent.EVENT_FINISHED, mReceivedMessageIds, mSentMessageIds, mAffectedChatIds, null);
    }

    private void processMessages(MessageListResponse response, Set<Long> trackList, boolean areSent, String localEmailAddress) {
        final Context context = getContext();
        final int sentAmount = response.getMessages().size();
        for (int i=0; i<sentAmount; i++) {
            final MessagesResponse messagesResponse = response.getMessages().get(i);

            if(!areSent && localEmailAddress.compareToIgnoreCase(messagesResponse.getRecipientEmail().trim()) != 0) {
                Log.w(TAG, "Received wrong message from GCM! Should have gone to email " + messagesResponse.getRecipientEmail());
            } else {
                try {
                    Message message = areSent ?
                            GlobalManager.getInstance(context).insertSentMessage(null,
                                    messagesResponse.getRecipientEmail(), messagesResponse.getSenderEmail(),
                                    messagesResponse.getAudioUrl(), messagesResponse.getMessageId(),
                                    messagesResponse.getTranscription(), messagesResponse.getCreatedTimestamp(),
                                    messagesResponse.getDuration(), true) :
                            GlobalManager.getInstance(context).insertReceivedMessage(messagesResponse.getRecipientEmail(),
                                    messagesResponse.getSenderName(), messagesResponse.getSenderEmail(),
                                    messagesResponse.getAudioUrl(), messagesResponse.getMessageId(),
                                    messagesResponse.getTranscription(), messagesResponse.getCreatedTimestamp(),
                                    messagesResponse.getDuration(), messagesResponse.getReadTimestamp(), true);

                    if (message != null) {
                        trackList.add(message.getId());
                        if(!mAffectedChatIds.contains(message.getChatId())) {
                            mAffectedChatIds.add(message.getChatId());
                        }
                        if (mLastMessageTimestamp == null || mLastMessageTimestamp.compareTo(message.getRegistrationTimestamp()) < 0) {
                            mLastMessageTimestamp = message.getRegistrationTimestamp();
                        }
                    }

                    long currentMs = System.currentTimeMillis();
                    if(currentMs - mLastProgressMs > PROGRESS_BETWEEN_MS) {
                        mLastProgressMs = currentMs;
                        SyncService.postSyncEvent(SyncEvent.EVENT_PROGRESS, mReceivedMessageIds, mSentMessageIds, mAffectedChatIds, null);
                    }
                } catch (Exception e) {
                    TrackerManager.getInstance(getContext()).logException(e);
                    mLocalError = true;
                }

                try {
                    Thread.sleep(100);
                } catch(InterruptedException e) {
                    /* nothing to do here */
                }
            }
        }
    }

    private void setLastSyncTimestamp(String ts) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(LAST_SYNC_TIMESTAMP_KEY, ts);
        editor.apply();
    }

    private String getLastSyncTimestamp() {
        return mSharedPreferences.getString(LAST_SYNC_TIMESTAMP_KEY, null);
    }

    private void doPendingLogouts() {
        final Context context = getContext();
        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        final PeppermintApi peppermintApi = new PeppermintApi(context);

        final Cursor cursor = PendingLogoutManager.getInstance().getAll(databaseHelper.getReadableDatabase());

        while(cursor.moveToNext()) {
            try {
                final PendingLogout pendingLogout = PendingLogoutManager.getInstance().getFromCursor(null, cursor);

                try {
                    peppermintApi.setAuthenticationToken(pendingLogout.getAuthenticationToken());
                    peppermintApi.removeReceiverRecorder(null, pendingLogout.getAccountServerId(), pendingLogout.getDeviceServerId());
                } catch (PeppermintApiInvalidAccessTokenException e) {
                    /* just eat it up and delete the pending logout anyway
                     * TODO how to solve this so that the request is always performed? */
                }

                databaseHelper.lock();
                PendingLogoutManager.getInstance().delete(databaseHelper.getWritableDatabase(), pendingLogout.getId());
                databaseHelper.unlock();

                TrackerManager.getInstance(context).track(TrackerApi.TYPE_EVENT, "Performed Pending Logout for " + pendingLogout.toString(), TAG);
            } catch (Exception e) {
                TrackerManager.getInstance(context).logException(e);
            }
        }

        cursor.close();
    }
}
