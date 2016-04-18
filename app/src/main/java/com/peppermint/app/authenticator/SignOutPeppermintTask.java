package com.peppermint.app.authenticator;

import android.content.Context;

import com.peppermint.app.cloud.senders.SenderSupportListener;
import com.peppermint.app.cloud.senders.SenderSupportTask;
import com.peppermint.app.data.ContactData;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.utils.Utils;

import java.util.Map;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Sign out task for the Peppermint API and local account.
 * </p>
 */
public class SignOutPeppermintTask extends SenderSupportTask {

    private AuthenticatorUtils mAuthenticatorUtils;

    public SignOutPeppermintTask(Context context, SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        getIdentity().setContext(context);
        mAuthenticatorUtils = new AuthenticatorUtils(context);
    }

    @Override
    protected void execute() throws Throwable {
        setupPeppermintAuthentication();
        AuthenticationData data = setupPeppermintAuthentication(true);

        getPeppermintApi().removeReceiverRecorder(getId().toString(), data.getAccountServerId(), data.getDeviceServerId());

        Map<Long, ContactData> peppermintContacts = ContactManager.getPeppermintContacts(getContext());
        for(ContactData contactData : peppermintContacts.values()) {
            ContactManager.deletePeppermint(getContext(), contactData.getRawId(), null);
        }

        PeppermintEventBus.postSignOutEvent();

        Utils.clearApplicationData(getContext());
        getSenderPreferences().clearAll();
        DatabaseHelper.clearInstance();

        mAuthenticatorUtils.signOut();
    }
}
