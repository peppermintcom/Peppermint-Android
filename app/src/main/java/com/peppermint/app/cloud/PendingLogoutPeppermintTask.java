package com.peppermint.app.cloud;

import android.content.Context;
import android.database.Cursor;

import com.peppermint.app.cloud.apis.PeppermintApi;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.cloud.rest.HttpResponse;
import com.peppermint.app.cloud.senders.Sender;
import com.peppermint.app.cloud.senders.SenderSupportTask;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.PendingLogout;
import com.peppermint.app.data.PendingLogoutManager;
import com.peppermint.app.tracking.TrackerApi;
import com.peppermint.app.tracking.TrackerManager;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Sign out task for the Peppermint API and local account.
 * </p>
 */
public class PendingLogoutPeppermintTask extends SenderSupportTask {

    private static final String TAG = PendingLogoutPeppermintTask.class.getSimpleName();

    public PendingLogoutPeppermintTask(Context context) {
        super(null, null, null);
        getIdentity().setContext(context);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();

        final PeppermintApi peppermintApi = getPeppermintApi();
        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getContext());

        Cursor cursor = PendingLogoutManager.getAll(DatabaseHelper.getInstance(getContext()).getReadableDatabase());
        while(cursor.moveToNext()) {
            try {
                final PendingLogout pendingLogout = PendingLogoutManager.getPendingLogoutFromCursor(cursor);

                try {
                    peppermintApi.setAuthenticationToken(pendingLogout.getAuthenticationToken());
                    peppermintApi.removeReceiverRecorder(getId().toString(),
                            pendingLogout.getAccountServerId(), pendingLogout.getDeviceServerId());
                } catch (PeppermintApiInvalidAccessTokenException e) {
                    /* just eat it up and delete the pending logout anyway
                     * TODO how to solve this so that the request is always performed? */
                }

                databaseHelper.lock();
                PendingLogoutManager.delete(databaseHelper.getWritableDatabase(), pendingLogout.getId());
                databaseHelper.unlock();

                TrackerManager.getInstance(getContext()).track(TrackerApi.TYPE_EVENT, "Performed Pending Logout for " + pendingLogout.toString(), TAG);
            } catch (Exception e) {
                TrackerManager.getInstance(getContext()).logException(e);
            }
        }

        if(cursor != null) {
            cursor.close();
        }
    }

    @Override
    protected PeppermintApi getPeppermintApi() {
        PeppermintApi api = (PeppermintApi) getParameter(Sender.PARAM_PEPPERMINT_API);
        if(api == null) {
            api = new PeppermintApi(getContext()) {
                @Override
                public String renewAuthenticationToken() throws Exception {
                    // do not get any of the logged in user's authentication token
                    return peekAuthenticationToken();
                }

                @Override
                protected boolean isAuthenticationTokenRenewalRequired(HttpResponse response) {
                    // do not try to get a new authentication token
                    return false;
                }
            };
            setParameter(Sender.PARAM_PEPPERMINT_API, api);
        }
        return api;
    }
}
