package com.peppermint.app.sending;

import java.util.Map;

/**
 * Created by Nuno Luz on 25-11-2015.
 */
public abstract class SenderAuthorizationTask extends SenderTask {

    public SenderAuthorizationTask(Sender sender, SenderListener listener) {
        super(sender, null, listener);
    }

    public SenderAuthorizationTask(Sender sender, SenderListener listener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(sender, null, listener, parameters, preferences);
    }

    public SenderAuthorizationTask(SenderTask sendingTask) {
        super(sendingTask);
    }
}
