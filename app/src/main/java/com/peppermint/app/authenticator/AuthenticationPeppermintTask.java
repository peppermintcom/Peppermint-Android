package com.peppermint.app.authenticator;

import android.content.Context;

import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.api.data.JWTsResponse;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Peppermint API authorization support task.
 * </p>
 */
public class AuthenticationPeppermintTask extends SenderSupportTask {

    private static final String TAG = AuthenticationPeppermintTask.class.getSimpleName();

    private String mDeviceServerId;
    private String mDeviceId, mDeviceKey;
    private String mEmail, mPassword;
    private int mAccountType;
    private String mAccessToken;

    public AuthenticationPeppermintTask(Context context, int accountType, String deviceId, String deviceKey, String email, String password, SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        mEmail = email;
        mPassword = password;
        mAccountType = accountType;
        mDeviceId = deviceId;
        mDeviceKey = deviceKey;
        getIdentity().setContext(context);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();
        String fullName = getSenderPreferences().getFullName();
        JWTsResponse response = getPeppermintApi().authOrRegister(mEmail, mPassword, mAccountType, mDeviceId, mDeviceKey, fullName, getTrackerManager());
        mAccessToken = response.getAccessToken();
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
}
