package com.peppermint.app.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticatorUtils;
import com.peppermint.app.sending.api.PeppermintApi;

/**
 * Created by Nuno Luz on 02-02-2016.
 */
public class RegistrationIntentService extends IntentService {

    private static final String TAG = PeppermintInstanceIDListenerService.class.getSimpleName();

    public static final String REGISTRATION_COMPLETE = "com.peppermint.app.gcm.REGISTRATION_COMPLETE";

    private AuthenticatorUtils mAuthenticatorUtils;
    private PeppermintApi mPeppermintApi;

    public RegistrationIntentService() {
        super(TAG);
        mPeppermintApi = new PeppermintApi();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            mAuthenticatorUtils = new AuthenticatorUtils(this);
            String accessToken = mAuthenticatorUtils.getAccessToken();
            AuthenticationData data = mAuthenticatorUtils.getAccountData();
            mPeppermintApi.setAccessToken(accessToken);

            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_default_sender_id),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            mPeppermintApi.updateRecorder(data.getDeviceServerId(), token);
            mAuthenticatorUtils.updateAccountGcmRegistration(token);
            // [END register_for_gcm]
        } catch (Exception e) {
            Log .d(TAG, "Failed to complete token refresh", e);
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

}
