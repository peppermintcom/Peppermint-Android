package com.peppermint.app.sending;

import com.peppermint.app.authenticator.AuthenticatorUtils;
import com.peppermint.app.data.SendingRequest;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     Peppermint API authorization support task.
 * </p>
 */
public class PeppermintAuthorizationSupportTask extends SenderSupportTask implements Cloneable {

    private static final String TAG = PeppermintAuthorizationSupportTask.class.getSimpleName();

    public PeppermintAuthorizationSupportTask(PeppermintAuthorizationSupportTask supportTask) {
        super(supportTask);
    }

    public PeppermintAuthorizationSupportTask(Sender sender, SendingRequest sendingRequest, SenderSupportListener senderSupportListener) {
        super(sender, sendingRequest, senderSupportListener);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();

        final AuthenticatorUtils authenticatorUtils = new AuthenticatorUtils(getContext());
        authenticatorUtils.invalidateAccessToken();
        String authToken = authenticatorUtils.getAccessToken();
        getPeppermintApi().setAccessToken(authToken);
    }

}
