package com.peppermint.app.cloud;

import android.content.Context;
import android.util.Log;

import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.cloud.senders.SenderSupportListener;
import com.peppermint.app.cloud.senders.SenderSupportTask;
import com.peppermint.app.cloud.senders.exceptions.NoInternetConnectionException;
import com.peppermint.app.data.GlobalManager;
import com.peppermint.app.data.Message;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.SignOutEvent;
import com.peppermint.app.tracking.TrackerManager;

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
        getPeppermintApi().markAsPlayedMessage(message.getServerId());
        message.setPlayed(true);
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
        } else {
            PeppermintEventBus.postMarkAsPlayedEvent(getMessage());
        }
    }
}
