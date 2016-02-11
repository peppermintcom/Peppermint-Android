package com.peppermint.app.authenticator;

import android.content.Context;

import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Peppermint API authorization support task.
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
        // TODO DELETE Recorder-Account Rel.
        /*checkInternetConnection();
        AuthenticationData data = setupPeppermintAuthentication();
        getPeppermintApi().updateRecorder(data.getDeviceId(), "");*/

        Utils.clearApplicationData(getContext());
        getSenderPreferences().clearAll();

        mAuthenticatorUtils.signOut();
    }
}
