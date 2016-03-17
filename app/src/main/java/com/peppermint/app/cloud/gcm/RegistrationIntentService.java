package com.peppermint.app.cloud.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticatorUtils;
import com.peppermint.app.cloud.apis.PeppermintApi;

/**
 * Created by Nuno Luz on 02-02-2016.
 *
 * Intent GCM service that performs the GCM registration process.
 */
public class RegistrationIntentService extends IntentService {

    private static final String TAG = PeppermintInstanceIDListenerService.class.getSimpleName();

    public static final String REGISTRATION_COMPLETE_ACTION = "com.peppermint.app.cloud.gcm.REGISTRATION_COMPLETE";
    public static final String PARAM_REGISTRATION_ERROR = TAG + "_paramError";

    private PeppermintApi mPeppermintApi;

    public RegistrationIntentService() {
        super(TAG);
        mPeppermintApi = new PeppermintApi();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Throwable error = null;
        try {
            AuthenticatorUtils authenticatorUtils = new AuthenticatorUtils(this);
            String accessToken = authenticatorUtils.getAccessToken();
            AuthenticationData data = authenticatorUtils.getAccountData();
            mPeppermintApi.setAccessToken(accessToken);

            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls are local.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_default_sender_id), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]

            Log.d(TAG, "New GCM Registration Token: " + token);

            mPeppermintApi.updateRecorder(data.getDeviceServerId(), token);
            authenticatorUtils.updateAccountGcmRegistration(token);
            // [END register_for_gcm]
        } catch (Throwable e) {
            Log.d(TAG, "Failed to Complete Token Refresh", e);
            error = e;
        }

        // notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(REGISTRATION_COMPLETE_ACTION);
        registrationComplete.putExtra(PARAM_REGISTRATION_ERROR, error);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

}
