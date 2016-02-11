package com.peppermint.app.cloud.senders;

import com.peppermint.app.data.Message;

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

    public PeppermintAuthorizationSupportTask(Sender sender, Message message, SenderSupportListener senderSupportListener) {
        super(sender, message, senderSupportListener);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();
        setupPeppermintAuthentication(true);
    }

}
