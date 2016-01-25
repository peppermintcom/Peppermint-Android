package com.peppermint.app.sending;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 01-10-2015.
 * <p>
 *     Peppermint API registration support task.
 * </p>
 */
public class PeppermintRegistrationSupportTask extends SenderSupportTask implements Cloneable {

    public PeppermintRegistrationSupportTask(PeppermintRegistrationSupportTask supportTask) {
        super(supportTask);
    }

    public PeppermintRegistrationSupportTask(Sender sender, SendingRequest sendingRequest, SenderSupportListener senderSupportListener) {
        super(sender, sendingRequest, senderSupportListener);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();

        String[] keys = PeppermintApi.getKeys(getContext());
        getPeppermintApi().registerRecorder(keys[0], keys[1], Utils.getAndroidVersion() + " - " + Utils.getDeviceName());
    }

}
