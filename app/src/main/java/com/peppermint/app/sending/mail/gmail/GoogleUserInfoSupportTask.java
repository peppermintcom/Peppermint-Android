package com.peppermint.app.sending.mail.gmail;

import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.api.GoogleApi;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 22-01-2016.
 */
public class GoogleUserInfoSupportTask extends SenderSupportTask {

    public GoogleUserInfoSupportTask(GoogleUserInfoSupportTask supportTask) {
        super(supportTask);
    }

    public GoogleUserInfoSupportTask(Sender sender, SendingRequest sendingRequest, SenderSupportListener senderSupportListener) {
        super(sender, sendingRequest, senderSupportListener);
    }

    @Override
    protected void execute() throws Throwable {
        GoogleApi.UserInfoResponse response = getGoogleApi().getUserInfo();

        String firstName = response.getFirstName();
        String lastName = response.getLastName();
        String fullName = response.getFullName();

        if (firstName != null && lastName != null && Utils.isValidName(firstName) && Utils.isValidName(lastName)) {
            getSenderPreferences().setFirstName(firstName);
            getSenderPreferences().setLastName(lastName);
        } else if (fullName != null && Utils.isValidName(fullName)) {
            getSenderPreferences().setFullName(fullName);
        }
    }
}
