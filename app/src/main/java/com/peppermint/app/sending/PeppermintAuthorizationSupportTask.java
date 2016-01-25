package com.peppermint.app.sending;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.api.PeppermintApi;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     Peppermint API authorization support task.
 * </p>
 */
public class PeppermintAuthorizationSupportTask extends SenderSupportTask implements Cloneable {

    public PeppermintAuthorizationSupportTask(PeppermintAuthorizationSupportTask supportTask) {
        super(supportTask);
    }

    public PeppermintAuthorizationSupportTask(Sender sender, SendingRequest sendingRequest, SenderSupportListener senderSupportListener) {
        super(sender, sendingRequest, senderSupportListener);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();

        String[] keys = PeppermintApi.getKeys(getContext());
        getPeppermintApi().authRecorder(keys[0], keys[1]);
    }

}
