package com.peppermint.app.services.messenger;

import android.content.Context;
import android.util.Log;

import com.peppermint.app.dal.GlobalManager;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.services.authenticator.AuthenticationService;
import com.peppermint.app.services.authenticator.SignOutEvent;
import com.peppermint.app.services.messenger.handlers.NoInternetConnectionException;
import com.peppermint.app.services.messenger.handlers.SenderPreferences;
import com.peppermint.app.services.messenger.handlers.SenderSupportListener;
import com.peppermint.app.services.messenger.handlers.SenderSupportTask;
import com.peppermint.app.trackers.TrackerManager;

import java.sql.SQLException;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Sync Peppermint messages.
 * </p>
 */
public class MessagesMarkPlayedTask extends SenderSupportTask {

    private static final String TAG = MessagesMarkPlayedTask.class.getSimpleName();

    public MessagesMarkPlayedTask(Context context, Message message, SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        getIdentity().setContext(context);
        getIdentity().setTrackerManager(TrackerManager.getInstance(context.getApplicationContext()));
        getIdentity().setPreferences(new SenderPreferences(context));
        setMessage(message);
    }

    @Override
    protected void execute() throws Throwable {
        Log.d(TAG, "Starting Mark as Played...");

        Message message = getMessage();
        setupPeppermintAuthentication();
        getPeppermintApi().markAsPlayedMessage(getId().toString(), message.getServerId());
        message.setPlayed(true);
    }

    public void onEventMainThread(SignOutEvent event) {
        cancel(true);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        AuthenticationService.registerEventListener(this);
    }

    @Override
    protected void onCancelled(Void aVoid) {
        super.onCancelled(aVoid);
        AuthenticationService.unregisterEventListener(this);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        AuthenticationService.unregisterEventListener(this);
        if(getError() != null) {
            Message message = getMessage();
            try {
                GlobalManager.unmarkAsPlayed(getContext(), message);
            } catch (SQLException e) {
                getTrackerManager().logException(e);
            }

            if(!(getError() instanceof NoInternetConnectionException)) {
                getTrackerManager().logException(getError());
            }
        }
    }
}
