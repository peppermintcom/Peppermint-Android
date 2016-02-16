package com.peppermint.app.authenticator;

import android.content.Context;

import com.peppermint.app.cloud.apis.data.JWTsResponse;
import com.peppermint.app.cloud.senders.SenderSupportListener;
import com.peppermint.app.cloud.senders.SenderSupportTask;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Authentication task for the Peppermint API.
 * </p>
 */
public class AuthenticationPeppermintTask extends SenderSupportTask {

    private String mDeviceServerId;
    private String mDeviceId, mDeviceKey;
    private String mEmail, mPassword;
    private String mAccountServerId;
    private int mAccountType;
    private String mAccessToken;

    public AuthenticationPeppermintTask(Context context, int accountType, String deviceId, String deviceKey, String email, String password, SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        mEmail = email;
        mPassword = password;
        mAccountType = accountType;
        mDeviceId = deviceId;
        mDeviceKey = deviceKey;
        // override to set the context
        getIdentity().setContext(context);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();

        String fullName = getSenderPreferences().getFullName();

        JWTsResponse response = getPeppermintApi().authOrRegister(mEmail, mPassword, mAccountType, mDeviceId, mDeviceKey, fullName, getTrackerManager());

        mAccessToken = response.getAccessToken();
        mAccountServerId = response.getAccount().getAccountId();
        mDeviceServerId = response.getRecorder().getRecorderId();
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public String getDeviceKey() {
        return mDeviceKey;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getPassword() {
        return mPassword;
    }

    public int getAccountType() {
        return mAccountType;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public String getDeviceServerId() {
        return mDeviceServerId;
    }

    public String getAccountServerId() {
        return mAccountServerId;
    }
}
