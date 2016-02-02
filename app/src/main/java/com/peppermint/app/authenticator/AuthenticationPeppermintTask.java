package com.peppermint.app.authenticator;

import android.content.Context;

import com.peppermint.app.sending.Sender;
import com.peppermint.app.sending.SenderSupportListener;
import com.peppermint.app.sending.SenderSupportTask;
import com.peppermint.app.sending.api.PeppermintApi;
import com.peppermint.app.sending.api.data.JWTsResponse;

/**
 * Created by Nuno Luz on 28-01-2016.
 * <p>
 *     Peppermint API authorization support task.
 * </p>
 */
public class AuthenticationPeppermintTask extends SenderSupportTask {

    private static final String TAG = AuthenticationPeppermintTask.class.getSimpleName();

    private String mDeviceId, mDeviceKey;
    private String mEmail, mPassword;
    private String mFirstName, mLastName;
    private int mAccountType;
    private String mAccessToken;

    public AuthenticationPeppermintTask(Context context, PeppermintApi peppermintApi, int accountType, String deviceId, String deviceKey, String email, String password, String firstName, String lastName, SenderSupportListener senderSupportListener) {
        super(null, null, senderSupportListener);
        mEmail = email;
        mPassword = password;
        mAccountType = accountType;
        mFirstName = firstName;
        mLastName = lastName;
        mDeviceId = deviceId;
        mDeviceKey = deviceKey;
        setParameter(Sender.PARAM_PEPPERMINT_API, peppermintApi);
        getIdentity().setContext(context);
    }

    @Override
    protected void execute() throws Throwable {
        checkInternetConnection();
        String fullName = (mFirstName + " " + mLastName).trim();
        JWTsResponse response = getPeppermintApi().authOrRegister(mEmail, mPassword, mAccountType, mDeviceId, mDeviceKey, fullName, getTrackerManager());
        mAccessToken = response.getAccessToken();
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

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public int getAccountType() {
        return mAccountType;
    }

    public String getAccessToken() {
        return mAccessToken;
    }
}
